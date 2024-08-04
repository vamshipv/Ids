package de.uniba.rz.entities.amqp;

import de.uniba.rz.entities.ticket.Ticket;

import java.io.Serializable;
import java.util.List;

public class Response implements Serializable {
    List<Ticket> tickets;

    public Response() {
    }

    public Response(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    @Override
    public String toString() {
        return "Response{" +
                "tickets=" + tickets +
                '}';
    }
}
