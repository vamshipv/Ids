package de.uniba.rz.app.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniba.rz.app.TicketManagementBackend;
import de.uniba.rz.entities.ticket.*;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HttpTicketManagementBackend implements TicketManagementBackend {

    private final Logger logger = LoggerFactory.getLogger(HttpTicketManagementBackend.class);
    //    Using a HashMap is a good choice if you need fast retrieval of tickets based on their ID. It provides constant-time complexity (O(1)) for the get and put operations.
    HashMap<Integer,Ticket> localTicketHashMap = new HashMap<>();
    AtomicInteger nextId;
    //    Since the code uses AtomicInteger for nextId,
    //    it suggests that the code might be used in a concurrent environment.
    //    AtomicInteger provides atomic operations, ensuring that multiple threads can safely access and update the value of nextId.
    String host;
    int port;
    String url;
    public HttpTicketManagementBackend(String host, int port) {
        this.port = port;
        this.host = host;
        this.url = "https://" +host+":"+port+"/";
        nextId = new AtomicInteger(1000);
    }

    @Override
    public void triggerShutdown() {

    }

    @Override
    public Ticket createNewTicket(String reporter, String topic, String description, Type type, Priority priority) throws TicketException {
        logger.info("Creating ticket with ID:"+nextId.get());
        Ticket ticket = new Ticket(nextId.getAndIncrement(),reporter,topic,description,type,priority);
        Ticket responseTicket = ClientBuilder.newClient().target(url+"api/ticket/create")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(ticket,MediaType.APPLICATION_JSON), Ticket.class);
        if(responseTicket == null) {
            throw new TicketException("Can not create a response ticket");
        }
        else {
            localTicketHashMap.put(responseTicket.getId(), responseTicket);
            return ticket;
        }

    }

    @Override
    public List<Ticket> getAllTickets() throws TicketException {
        ArrayList<Ticket> tickets;
        try {
            Response response = ClientBuilder.newClient().target(url + "api/ticket")
                    .request(MediaType.APPLICATION_JSON).get();
            ObjectMapper objectMapper = new ObjectMapper();
            tickets = objectMapper.readValue(response.readEntity(String.class), new TypeReference<ArrayList<Ticket>>() {
            });
            localTicketHashMap.clear();

        }catch (JsonProcessingException e)
        {
            throw new TicketException("Error in parsing JSON",e);
        }
        if(tickets != null){
            tickets.forEach(ticket -> {
                localTicketHashMap.put(ticket.getId(),ticket);
            });
        }
        return localTicketHashMap.values().stream()
                .map(ticket -> (Ticket) ticket.clone())
                .collect(Collectors.toList());
    }

    @Override
    public Ticket getTicketById(int id) throws TicketException {
        if(localTicketHashMap.containsKey(id))
            return localTicketHashMap.get(id);
        else
            throw new TicketException("Ticket with ID: "+id+" Not found");

    }


    private Ticket updateTicketStatus(int id, Status status) throws TicketException {
        Ticket responseTicket = ClientBuilder.newClient().target(url+"api/ticket/update")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(status,MediaType.APPLICATION_JSON), Ticket.class);
        if (responseTicket != null)
            return (Ticket) responseTicket.clone();
        else
            throw new TicketException("Cannot update the ticket status");
    }

    @Override
    public Ticket rejectTicket(int id) throws TicketException {
        return updateTicketStatus(id, Status.REJECTED);    }

    @Override
    public Ticket closeTicket(int id) throws TicketException {
        return updateTicketStatus(id, Status.CLOSED);
    }


    @Override
    public Ticket acceptTicket(int id) throws TicketException {
        return updateTicketStatus(id, Status.ACCEPTED);
    }


}
