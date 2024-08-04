package de.uniba.rz.entities.amqp;

import de.uniba.rz.entities.ticket.Status;
import de.uniba.rz.entities.ticket.Ticket;

import java.io.Serializable;

public class Request implements Serializable {

    private Ticket ticket;
    private int ticketId;
    private Status status;
    private MessageType messageType;

    public Request() {
    }

    public Request(MessageType messageType) {
        this.messageType = messageType;
    }

    public Request(int ticketId, MessageType messageType) {
        this.ticketId = ticketId;
        this.messageType = messageType;
    }

    public Request(Ticket ticket, MessageType messageType) {
        this.ticket = ticket;
        this.messageType = messageType;
    }

    public Request(MessageType messageType, int ticketId, Status status) {
        this.messageType = messageType;
        this.ticketId = ticketId;
        this.status = status;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {
        return "Request{" +
                "ticket=" + ticket +
                ", ticketId=" + ticketId +
                ", status=" + status +
                ", messageType=" + messageType +
                '}';
    }
}
