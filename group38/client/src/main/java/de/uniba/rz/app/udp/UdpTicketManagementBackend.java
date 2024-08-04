package de.uniba.rz.app.udp;

import de.uniba.rz.app.TicketManagementBackend;
import de.uniba.rz.entities.ticket.Priority;
import de.uniba.rz.entities.ticket.Ticket;
import de.uniba.rz.entities.ticket.TicketException;
import de.uniba.rz.entities.ticket.Type;
import de.uniba.rz.entities.udp.Packet;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UdpTicketManagementBackend implements TicketManagementBackend {

    private static final Logger logger = LoggerFactory.getLogger(UdpTicketManagementBackend.class);

    private final String host;
    private final int port;
    private final DatagramSocket connection;
    private final AtomicInteger id;

    private byte[] requestBuffer;
    private byte[] responseBuffer;

    public UdpTicketManagementBackend(String host, int port) {
        this.host = host;
        this.port = port;
        this.id = new AtomicInteger(1);
        this.connection = setupConnection();
    }

    private DatagramSocket setupConnection() {
        try {
            return new DatagramSocket();
        } catch (SocketException e) {
            logError("Failed to set up connection: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void triggerShutdown() {
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public Ticket createNewTicket(String reporter, String topic, String description, Type type, Priority priority)
            throws TicketException {
        Ticket ticket = new Ticket(id.getAndIncrement(), reporter, topic, description, type, priority);
        Packet packet = new Packet("createNewTicket", ticket);
        logInfo("createNewTicket --> {}", ticket);
        sendRequest(packet);
        String ack = (String) receiveResponse();
        logInfo("createNewTicket --> Response From Server : {}", ack);
        return (Ticket) ticket.clone();
    }

    @Override
    public List<Ticket> getAllTickets() throws TicketException {
        Packet wrapper = new Packet("getAllTickets", null);
        return getTickets(wrapper);
    }

    @Override
    public Ticket getTicketById(int id) throws TicketException {
        Packet wrapper = new Packet("getTicketById", id);
        Ticket ticket = null;
        try {
            logInfo("getTicketById --> Sending request to fetch tickets");
            sendRequest(wrapper);
            ticket = (Ticket) receiveResponse();
            logInfo("getTicketById --> Tickets : {}", ticket);
            return ticket;
        } catch (Exception e) {
            logError("getTicketById --> Failed to fetch tickets by ID: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Ticket acceptTicket(int id) throws TicketException {
        Packet wrapper = new Packet("acceptTicket", id);
        Ticket ticket = null;
        try {
            logInfo("acceptTicket --> Updating ticket status 'ACCEPTED' for TicketID : {}", id);
            sendRequest(wrapper);
            ticket = (Ticket) receiveResponse();
            logInfo("acceptTicket --> Server Response : {}", ticket);
            return ticket;
        } catch (Exception e) {
            logError("acceptTicket --> Failed for TicketID : {}. Exception: {}", id, e.getMessage(), e);
        }
        return null;
    }


    @Override
    public Ticket closeTicket(int id) throws TicketException {
        Packet wrapper = new Packet("closeTicket", id);
        Ticket ticket = null;
        try {
            logInfo("closeTicket --> Updating ticket status 'CLOSED' for TicketID : {}", id);
            sendRequest(wrapper);
            ticket = (Ticket) receiveResponse();
            logInfo("closeTicket --> Server Response : {}", ticket);
            return ticket;
        } catch (Exception e) {
            logError("closeTicket --> Failed for TicketID : {}. Exception: {}", id, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Ticket rejectTicket(int id) throws TicketException {
        Packet wrapper = new Packet("rejectTicket", id);
        Ticket ticket;
        try {
            logInfo("rejectTicket --> Updating ticket status 'REJECTED' for TicketID : {}", id);
            sendRequest(wrapper);
            ticket = (Ticket) receiveResponse();
            logInfo("rejectTicket --> Server Response : {}", ticket);
            return ticket;
        } catch (Exception e) {
            logError("rejectTicket --> Failed for TicketID : {}. Exception: {}", id, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Ticket> getTicketsByName(String name) throws TicketException {
        Packet wrapper = new Packet("getTicketsByName", name);
        return getTickets(wrapper);
    }

    @Override
    public List<Ticket> getTicketsByNameAndType(String name, Type type) throws TicketException {
        Packet wrapper = new Packet("getTicketsByNameAndType", type);
        return getTickets(wrapper);
    }

    private List<Ticket> getTickets(Packet wrapper) {
        List<Ticket> searchedTicketName = new ArrayList<>();
        try {
            logInfo("getTickets --> Sending request to fetch tickets : {}", wrapper);
            sendRequest(wrapper);
            if (wrapper.getType().equals("getAllTickets")) {
                List<Ticket> received = (List<Ticket>) receiveResponse();
                logInfo("getTickets --> No of tickets received : {}", received != null ? received.size() : 0);
                return received;
            } else {
                Ticket received = (Ticket) receiveResponse();
                searchedTicketName.add(received);
                logInfo("getTickets -->  Search Response : {}", received);
                return searchedTicketName;
            }
        } catch (Exception e) {
            logError("getTickets --> Exception while fetching tickets: {}", e.getMessage(), e);
        }
        return searchedTicketName;
    }

    private void sendRequest(Packet packet) throws TicketException {
        try {
            logInfo("sendRequest --> Packet Request : {}", packet);
            requestBuffer = SerializationUtils.serialize(packet);
            DatagramPacket request = new DatagramPacket(requestBuffer, requestBuffer.length,
                    InetAddress.getByName(host), port);
            connection.send(request);
        } catch (Exception e) {
            logError("sendRequest --> Failed to send packet: {}", e.getMessage(), e);
            throw new TicketException("Failed to send request to server", e);
        }
    }


    private void logInfo(String message, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(message, args);
        }
    }

    private void logError(String message, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(message, args);
        }
    }

    private Object receiveResponse() throws TicketException {
        try {
            logInfo("receiveResponse --> Waiting for server reply");
            responseBuffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
            connection.receive(response);
            return SerializationUtils.deserialize(response.getData());
        } catch (Exception e) {
            logError("receiveResponse --> Failed to receive response: {}", e.getMessage(), e);
            throw new TicketException("Error while receiving response from the server", e);
        }
    }
}

