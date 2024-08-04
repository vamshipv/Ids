package de.uniba.rz.backend.http.service;

import de.uniba.rz.backend.store.ConcurrentTicketStore;
import de.uniba.rz.entities.ticket.Status;
import de.uniba.rz.entities.ticket.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TicketService {

    public ConcurrentTicketStore ticketStore = new ConcurrentTicketStore();
    public final Logger logger = LoggerFactory.getLogger(TicketService.class);

    public TicketService() {
    }

    public List<Ticket> getAllTickets() {
        logger.info("getAllTickets");
        return ticketStore.getAllTickets();
    }

    public Ticket createNewTicket(Ticket ticket) {
        logger.info("createNewTicket" + ticket.toString());
        return ticketStore.storeNewTicket(
                ticket.getReporter(),
                ticket.getTopic(),
                ticket.getDescription(),
                ticket.getType(),
                ticket.getPriority()
        );
    }

    public Ticket updateStatus(int ticketId, Status status) {
        logger.info("updateStatus :: " + ticketId + " : " + status);
        return ticketStore.getAllTickets().stream().filter(ticket -> ticket.getId() == ticketId).findFirst().orElse(null);
    }

    public Ticket getTicketById(int ticketId) {
        logger.info("getTicketById :: " + ticketId);
        return ticketStore.getTicketById(ticketId);
    }
}
