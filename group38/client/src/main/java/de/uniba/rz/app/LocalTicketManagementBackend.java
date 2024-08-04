package de.uniba.rz.app;

import de.uniba.rz.entities.ticket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LocalTicketManagementBackend implements TicketManagementBackend {

	HashMap<Integer, Ticket> localTicketStore = new HashMap<>();
	private static Logger logger = LoggerFactory.getLogger(LocalTicketManagementBackend.class);

	AtomicInteger nextId;

	public LocalTicketManagementBackend() {
		nextId = new AtomicInteger(1);
	}

	@Override
	public void triggerShutdown() {
		// local implementation is in memory only - no need to close connections
		// and free resources

	}

	@Override
	public Ticket createNewTicket(String reporter, String topic, String description, Type type, Priority priority) {
		logger.info("Creating Ticket with\nTopic: "+topic+"    Desc: "+description+"\nType: "+type+"    Priority: "+priority);
		Ticket newTicket = new Ticket(nextId.getAndIncrement(), reporter, topic, description, type, priority);
		localTicketStore.put(newTicket.getId(), newTicket);

		return (Ticket) newTicket.clone();
	}

	@Override
	public List<Ticket> getAllTickets() throws TicketException {
		if(localTicketStore.isEmpty())
			return new ArrayList<>();
		else
			return localTicketStore.entrySet().stream().map(entry -> (Ticket) entry.getValue().clone())
					.collect(Collectors.toList());
	}

	@Override
	public Ticket getTicketById(int id) throws TicketException {
		logger.info("Getting Tickets By ID: "+id);
		if (!localTicketStore.containsKey(id)) {
			throw new TicketException("Ticket ID is unknown");
		}

		return (Ticket) getTicketByIdInternal(id).clone();
	}

	private Ticket getTicketByIdInternal(int id) throws TicketException {
		if (!localTicketStore.containsKey(id)) {
			throw new TicketException("Ticket ID is unknown");
		}

		return localTicketStore.get(id);
	}

	@Override
	public Ticket acceptTicket(int id) throws TicketException {

		Ticket ticketToModify = getTicketByIdInternal(id);
		if (ticketToModify.getStatus() != Status.NEW) {
			throw new TicketException(
					"Can not accept Ticket as it is currently in status " + ticketToModify.getStatus());
		}

		ticketToModify.setStatus(Status.ACCEPTED);
		logger.info("Ticket ID: "+id+" ACCEPTED.");
		return (Ticket) ticketToModify.clone();
	}

	@Override
	public Ticket rejectTicket(int id) throws TicketException {

		Ticket ticketToModify = getTicketByIdInternal(id);
		if (ticketToModify.getStatus() != Status.NEW) {
			throw new TicketException(
					"Can not reject Ticket as it is currently in status " + ticketToModify.getStatus());
		}

		ticketToModify.setStatus(Status.REJECTED);
		logger.info("Ticket ID: "+id+" REJECTED.");
		return (Ticket) ticketToModify.clone();
	}

	@Override
	public Ticket closeTicket(int id) throws TicketException {

		Ticket ticketToModify = getTicketByIdInternal(id);
		if (ticketToModify.getStatus() != Status.ACCEPTED) {
			throw new TicketException(
					"Can not close Ticket as it is currently in status " + ticketToModify.getStatus());
		}

		ticketToModify.setStatus(Status.CLOSED);
		logger.info("Ticket  ID: "+id+" CLOSED.");
		return (Ticket) ticketToModify.clone();
	}

}
