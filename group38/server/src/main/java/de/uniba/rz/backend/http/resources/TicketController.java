package de.uniba.rz.backend.http.resources;

import de.uniba.rz.backend.http.service.TicketService;
import de.uniba.rz.entities.ticket.Status;
import de.uniba.rz.entities.ticket.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("api")
public class TicketController {

    private Logger logger = LoggerFactory.getLogger(TicketController.class);
    private TicketService ticketService = new TicketService();


    @GET
    @Path("/tickets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Ticket> getAllTickets() {
        logger.info("Request received for list all tickets");
        return ticketService.getAllTickets();
    }

    @GET
    @Path("/ticket/{ticketId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getTicketById(@PathParam("ticketId") int id) {
        logger.info("Request received for list all tickets");
        Ticket ticket = ticketService.getTicketById(id);
        return ticket == null ? Response.status(404).build() : ticket;
    }

    @POST
    @Path("/ticket")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Ticket createNewTicket(Ticket ticket) {
        logger.info("Create new ticket Request :: " + ticket.toString());
        return ticketService.createNewTicket(ticket);
    }

    @PUT
    @Path("/ticket/{ticketId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Ticket updateTicketStatus(@PathParam("ticketId") int id, Status status) {
        logger.info("Update ticket Request :: " + status + ":" + id);
        return ticketService.updateStatus(id, status);
    }
}
