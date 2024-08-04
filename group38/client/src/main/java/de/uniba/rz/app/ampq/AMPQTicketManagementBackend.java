package de.uniba.rz.app.ampq;

import de.uniba.rz.app.TicketManagementBackend;
import de.uniba.rz.entities.amqp.MessageType;
import de.uniba.rz.entities.amqp.Request;
import de.uniba.rz.entities.amqp.Response;
import rabbitmq.RabbitMQClient;
import de.uniba.rz.entities.ticket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static de.uniba.rz.entities.amqp.MessageType.GET_ONE;
import static de.uniba.rz.entities.amqp.MessageType.UPDATE;
import static de.uniba.rz.entities.ticket.Status.CLOSED;
import static de.uniba.rz.entities.ticket.Status.REJECTED;

public class AMPQTicketManagementBackend implements TicketManagementBackend {

    private final Logger logger = LoggerFactory.getLogger(AMPQTicketManagementBackend.class);
    private final AtomicInteger ticketId;
    private final RabbitMQClient client;

    public AMPQTicketManagementBackend(String host, String queue) {
        this.ticketId = new AtomicInteger(1000);
        this.client = new RabbitMQClient(host,queue);
    }

    @Override
    public Ticket createNewTicket(String reporter, String topic, String description, Type type, Priority priority) throws TicketException {
        Ticket ticket = new Ticket(ticketId.getAndIncrement(),reporter,topic,description,type,priority);
        logger.info("createNewTicket :: "+ticket);
        Request request = new Request(ticket,MessageType.CREATE);
        Response response = client.sendCreateNewTicketQueue(request);
        if(response != null && response.getTickets().size() == 1)
            return response.getTickets().get(0);
        else
            throw new TicketException("Ticket could not be created");
    }

    @Override
    public List<Ticket> getAllTickets() throws TicketException {
        logger.info("getAllTickets :: getting all tickets");
        Request request = new Request(MessageType.GET_ALL);
        Response response = client.sendGetAllTicketsQueue(request);
        return response == null ? new ArrayList<>() : response.getTickets();
    }

    @Override
    public Ticket getTicketById(int id) {
        logger.info("getTicketsById :: "+ id);
        Request request = new Request(id,GET_ONE);
        Response response = client.sendGetTicketByIdQueue(request);
        if (response != null && response.getTickets().size() == 1)
            return response.getTickets().get(0);
        return null;
    }

    private Ticket updateStatus(int id, Status status) throws TicketException {
        Request request = new Request(UPDATE,id,status);
        Response response = client.sendStatusUpdateQueue(request);
        if (response != null && response.getTickets().size() == 1)
            return response.getTickets().get(0);
        else
            throw new TicketException("Could not update ticket status");
    }
    @Override
    public Ticket acceptTicket(int id) throws TicketException {
        logger.info("acceptTicket :: Status updated to --> ACCEPTED :: Ticket ID : "+ id);
        return updateStatus(id,Status.ACCEPTED);
    }

    @Override
    public List<Ticket> getTicketsByName(String name) throws TicketException {
        List<Ticket> searchByNameList = new ArrayList<>();
        logger.info("getAllTickets :: getting all tickets with Topic: "+name);
        Request request = new Request(MessageType.GET_ALL);
        Response response = client.sendGetAllTicketsQueue(request);
        if (response == null){
            return new ArrayList<>();
        }
        else
        {
            List<Ticket> tickets = response.getTickets();
            for (Ticket t:tickets) {
                if(t.getTopic().equals(name))
                {
                    searchByNameList.add(t);
                }
            }
        }
        if(searchByNameList.isEmpty()) {
            logger.warn("Invalid Topic name or Type");
            throw new TicketException("Ticket is not found");
        }
        else
            return searchByNameList;
    }

    @Override
    public List<Ticket> getTicketsByNameAndType(String name, Type type) throws TicketException {
        List<Ticket> searchByTypeList = new ArrayList<>();
        logger.info("getAllTickets :: getting all tickets with Topic "+name+" and Type: "+type);
        Request request = new Request(MessageType.GET_ALL);
        Response response = client.sendGetAllTicketsQueue(request);
        if (response == null){
            return new ArrayList<>();
        }
        else
        {
            List<Ticket> tickets = response.getTickets();
            for (Ticket t:tickets) {
                if(t.getTopic().equals(name) && t.getType().equals(type))
                {
                    searchByTypeList.add(t);
                }
            }
        }
        if(searchByTypeList.isEmpty()) {
            logger.warn("Invalid name or Type");
            throw new TicketException("Ticket not found");
        }
        else
            return searchByTypeList;
    }

    @Override
    public Ticket closeTicket(int id) throws TicketException {
        logger.info("acceptTicket :: Status updated to --> CLOSED  :: Ticket ID : " + id);
        return updateStatus(id, CLOSED);
    }

    @Override
    public void triggerShutdown() {

    }

    @Override
    public Ticket rejectTicket(int id) throws TicketException {
        logger.info("acceptTicket :: Status updated to --> REJECTED  :: Ticket ID : " + id);
        return updateStatus(id, REJECTED);
    }



}
