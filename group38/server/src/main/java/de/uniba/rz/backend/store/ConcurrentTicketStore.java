package de.uniba.rz.backend.store;

import de.uniba.rz.entities.ticket.Priority;
import de.uniba.rz.entities.ticket.Status;
import de.uniba.rz.entities.ticket.Ticket;
import de.uniba.rz.entities.ticket.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentTicketStore implements TicketStore {

    private final Logger logger = LoggerFactory.getLogger(ConcurrentTicketStore.class);

    ConcurrentHashMap<Integer, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicInteger ticketId;

    public ConcurrentTicketStore() {
        this.ticketId = new AtomicInteger(1);
    }

    @Override
    public Ticket storeNewTicket(String reporter, String topic, String description, Type type, Priority priority) {
        logger.info("Store new ticket :: " + reporter + "::" + topic + "::" + description + "::" + type + "::" + priority);
        Ticket newTicket = new Ticket(ticketId.getAndIncrement(), reporter, topic, description, type, priority);
        tickets.put(newTicket.getId(), newTicket);
        return (Ticket) newTicket.clone();
    }

    @Override
    public Ticket updateTicketStatus(int ticketId, Status newStatus) throws IllegalStateException {
        logger.info("Update Ticket Status " + ticketId + "::" + newStatus);
        tickets.forEach((id, ticket) -> {
            if (id == ticketId) {
                ticket.setStatus(newStatus);
            }
        });
        return null;
    }

    @Override
    public List<Ticket> getAllTickets() {
        logger.info("getAllTickets");
        List<Ticket> allTickets = new ArrayList<>();
        tickets.forEach((id, ticket) -> allTickets.add(ticket));
        return allTickets;
    }

    @Override
    public Ticket getTicketById(int ticketId) {
        logger.info("getTicketByID :: " + ticketId);
        return (Ticket) tickets.entrySet().stream().filter(ticket -> ticket.getKey() == ticketId).findFirst().orElse(null);
    }
}
