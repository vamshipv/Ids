package de.uniba.rz.backend.store;


import de.uniba.rz.backend.UnknownTicketException;
import de.uniba.rz.entities.ticket.Priority;
import de.uniba.rz.entities.ticket.Status;
import de.uniba.rz.entities.ticket.Ticket;
import de.uniba.rz.entities.ticket.Type;

import java.util.List;

public interface TicketStore {

    Ticket storeNewTicket(String reporter, String topic, String description,
                          Type type, Priority priority);

    Ticket updateTicketStatus(int ticketId, Status newStatus) throws UnknownTicketException, IllegalStateException;

    List<Ticket> getAllTickets();

    Ticket getTicketById(int ticketId);
}
