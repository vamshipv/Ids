package de.uniba.rz.backend.store;

import de.uniba.rz.entities.ticket.Priority;
import de.uniba.rz.entities.ticket.Status;
import de.uniba.rz.entities.ticket.Ticket;
import de.uniba.rz.entities.ticket.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a basic implementation of the <code>TicketStore</code> interface for
 * testing purposes only.
 *
 * Caution: This class is neither thread-safe nor does it perform any checks in
 * the updateTicketStatus method
 *
 * Do not use this class in the assignment solution but provide an own
 * implementation of <code>TicketStore</code>!
 */
public class SimpleTicketStore implements TicketStore {

	private int nextTicketId = 0;
	private List<Ticket> ticketList = new ArrayList<>();

	@Override
	public Ticket storeNewTicket(String reporter, String topic, String description, Type type, Priority priority) {
		System.out.println("Creating new Ticket from Reporter: " + reporter + " with the topic \"" + topic + "\"");
		Ticket newTicket = new Ticket(nextTicketId++, reporter, topic, description, type, priority);
		ticketList.add(newTicket);
		return newTicket;
	}

	@Override
	public Ticket updateTicketStatus(int ticketId, Status newStatus) {
		for (Ticket ticket : ticketList) {
			if (ticket.getId() == ticketId) {
				ticket.setStatus(newStatus);
			}
		}
		return ticketList.stream().filter(ticket -> ticket.getId() == ticketId).findFirst().orElse(null);
	}

	@Override
	public List<Ticket> getAllTickets() {
		return ticketList;
	}

	@Override
	public Ticket getTicketById(int ticketId) {
		return ticketList.stream().filter(ticket -> ticket.getId() == ticketId).findFirst().orElse(null);
	}
}
