package rabbitmq;

import com.rabbitmq.client.*;
import de.uniba.rz.entities.amqp.Request;
import de.uniba.rz.entities.amqp.Response;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RabbitMQClient {

    private final Logger logger = LoggerFactory.getLogger(RabbitMQClient.class);
    private final ConnectionFactory connectionFactory = new ConnectionFactory();

    public static final String EXCHANGE = "ticket_exchange";
    public static final String NEW_TICKET_QUEUE = "create_new_ticket";
    public static final String STATUS_QUEUE ="update_status";
    public static final String GET_ALL_TICKETS_QUEUE = "get_all_tickets";
    public static final String GET_TICKET_BY_ID_QUEUE = "get_ticket_by_id";
    public static final String BINDING_QUEUE = "binding_queue";

    private final String queueName;
    private String corelId;
    public RabbitMQClient(String host,String queueName) {
        this.queueName = queueName;
        this.connectionFactory.setHost(host);
    }

    private void send(Request request,String queueName,String replyQueue){
//        logger.info("In send function");
        try(Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(RabbitMQClient.EXCHANGE, BuiltinExchangeType.DIRECT,true);
            channel.queueDeclare(queueName,true,false,false,null);
            channel.queueBind(queueName,RabbitMQClient.EXCHANGE,RabbitMQClient.BINDING_QUEUE);

            String replyQueueName = channel.queueDeclare(replyQueue,true,false,false,null).getQueue();
            corelId = UUID.randomUUID().toString();

            AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().correlationId(corelId).replyTo(replyQueueName).build();

            channel.queueDeclarePassive(replyQueueName);
            channel.basicPublish(RabbitMQClient.EXCHANGE,RabbitMQClient.BINDING_QUEUE,properties, SerializationUtils.serialize(request));

        }
        catch (IOException | TimeoutException e)
        {
            logger.error("Error while sending request to queue, queueName:{},exception:{}",queueName,e);
        }
    }

    private Response consume(String replyQueueName)
    {
//        logger.info("In consume function");
        final BlockingQueue<Response> blockingQueue = new ArrayBlockingQueue<>(1,true);
        try(Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel()){

//            logger.info("Connection: "+connection.toString());
//            logger.info("Channel: "+channel.toString());

            channel.queueDeclare(replyQueueName,true,false,false,null);


            channel.basicConsume(replyQueueName,true,"ticketConsumer",new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
//                            logger.info(SerializationUtils.deserialize(body));
                    try {
                        if(properties.getCorrelationId().equals(corelId))
                        {
                            blockingQueue.put(SerializationUtils.deserialize(body));
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            while (!Thread.currentThread().isInterrupted()){
                try{
                    return blockingQueue.poll(5, TimeUnit.SECONDS);
                }catch (InterruptedException e){
                    logger.error("Interrupted!",e);
                    Thread.currentThread().interrupt();
                }
            }
        }catch (IOException | TimeoutException e){
            logger.warn("Error occurred when consuming response from queue, queueName{},exception{}",queueName,e);
        }
        return null;
    }

    public Response sendCreateNewTicketQueue(Request payload) {
        logger.info("In sendCreateNewTicketQueue function");
        send(payload, queueName, NEW_TICKET_QUEUE);
        return consume(NEW_TICKET_QUEUE);
    }

    public Response sendGetAllTicketsQueue(Request payload) {
        send(payload, queueName, GET_ALL_TICKETS_QUEUE);
        return consume(GET_ALL_TICKETS_QUEUE);
    }

    public Response sendStatusUpdateQueue(Request payload) {
        send(payload, queueName, STATUS_QUEUE);
        return consume(STATUS_QUEUE);
    }

    public Response sendGetTicketByIdQueue(Request payload) {
        send(payload, queueName, GET_TICKET_BY_ID_QUEUE);
        return consume(GET_TICKET_BY_ID_QUEUE);
    }

}