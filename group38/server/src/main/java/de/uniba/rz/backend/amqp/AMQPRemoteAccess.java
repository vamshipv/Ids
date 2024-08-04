package de.uniba.rz.backend.amqp;

import com.rabbitmq.client.*;
import de.uniba.rz.backend.RemoteAccess;
import de.uniba.rz.backend.store.TicketStore;
import de.uniba.rz.backend.UnknownTicketException;
import de.uniba.rz.entities.amqp.MessageType;
import de.uniba.rz.entities.amqp.Request;
import de.uniba.rz.entities.amqp.Response;
import de.uniba.rz.entities.ticket.Status;
import de.uniba.rz.entities.ticket.Ticket;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import static rabbitmq.RabbitMQClient.*;


public class AMQPRemoteAccess implements RemoteAccess {

    private final Logger logger = LoggerFactory.getLogger(AMQPRemoteAccess.class);
    private final ConnectionFactory connectionFactory = new ConnectionFactory();
    private TicketStore ticketStore;
    private boolean active = false;
    private final String host;
    private final int port;
    private final String queueName;
    private final String vHost;
    private final String userName;
    private final String password;
    private final Boolean autoDelete;
    private final Boolean durable;
    private final String consumerTag;

    public AMQPRemoteAccess(String host, int port, String queueName, String vHost, String userName, String password, Boolean autoDelete, Boolean durable, String consumerTag) {

        logger.info("AMQP initialized successfully with host: "+host+" & port: "+port);
        this.host = host;
        this.port = port;
        this.queueName = queueName;
        this.vHost = vHost;
        this.userName = userName;
        this.password = password;
        this.autoDelete = autoDelete;
        this.durable = durable;
        this.consumerTag = consumerTag;
    }

    @Override
    public void prepareStartup(TicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    @Override
    public void shutdown() {
        active = false;
        logger.info("AMQP :: Connection closed");
    }

    private void startServer() {
        while (active) {
            pushQueueConsumer();
        }
    }

    @Override
    public void run() {
        logger.info("AMQP :: Connected to AMQP Host");
        active = true;
        startServer();

    }

    private void pushQueueConsumer() {
        connectionFactory.setHost(host);
        connectionFactory.setUsername(userName);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(vHost);
        connectionFactory.setPort(port);
        final BlockingQueue<Request> blockingQueue = new ArrayBlockingQueue<>(1, true);
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, durable, false, autoDelete, null);
            logger.info("AMQP :: Connection Successful " + channel.getConnection().getAddress());
            channel.basicConsume(queueName, false, consumerTag,
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            try {
                                channel.basicAck(envelope.getDeliveryTag(), false);
                                blockingQueue.put(SerializationUtils.deserialize(body));
                            }catch (InterruptedException e) {
                                logger.error("Interrupted!", e);
                                Thread.currentThread().interrupt();
                            }
                        }
                    });
            while (active) {
                processRequestMessage(blockingQueue.poll(), channel);
            }
        } catch (IOException | TimeoutException e) {
            logger.error("Error occurred during consuming payload from queue, queueName:{}, exception:{}", queueName, e);
        }
    }

    private void processRequestMessage(Request request, Channel channel) {
        try {
            if (request != null) {
                MessageType messageType = request.getMessageType();
                switch (messageType) {
                    case CREATE -> createNewTicket(request, channel);
                    case UPDATE -> updateTicketStatus(request, channel);
                    case GET_ALL -> getAllTickets(channel);
                    case GET_ONE -> getTicketById(request, channel);
                    default -> {}
                }
            }
        } catch (UnknownTicketException e) {
            logger.error("Ticket status update exception : {} ", e.getMessage());
        } catch (IOException e) {
            logger.error("Object serialization exception : {}", e.getMessage());
        }
    }

    private void getTicketById(Request request, Channel channel) throws IOException {
        Response ticketResponse = new Response();
        int ticketId = request.getTicketId();
        Ticket ticket1 = ticketStore.getTicketById(ticketId);
        ticketResponse.setTickets(List.of(ticket1));
        channel.basicPublish("", GET_TICKET_BY_ID_QUEUE, null,
                SerializationUtils.serialize(ticketResponse));

    }

    private void getAllTickets(Channel channel) throws IOException {
        Response allTickets = new Response();
        allTickets.setTickets(ticketStore.getAllTickets());
        channel.basicPublish("", GET_ALL_TICKETS_QUEUE, null,
                SerializationUtils.serialize(allTickets));

    }

    private void updateTicketStatus(Request request, Channel channel) throws UnknownTicketException, IOException {int id = request.getTicketId();
        Status status = request.getStatus();
        Ticket updatedTicket = ticketStore.updateTicketStatus(id, status);
        Response updateTicketResponse = new Response();
        updateTicketResponse.setTickets(List.of(updatedTicket));
        channel.basicPublish("", STATUS_QUEUE, null,
                SerializationUtils.serialize(updateTicketResponse));
    }

    private void createNewTicket(Request request, Channel channel) throws IOException {
        Ticket ticket = request.getTicket();
        Ticket storedTicket = ticketStore.storeNewTicket(ticket.getReporter(), ticket.getTopic(),
                ticket.getDescription(),
                ticket.getType(), ticket.getPriority());
        Response responseMessage = new Response();
        responseMessage.setTickets(List.of(storedTicket));
        channel.basicPublish("", NEW_TICKET_QUEUE, null,
                SerializationUtils.serialize(responseMessage));
    }


}
