package de.uniba.rz.backend.udp;

import de.uniba.rz.backend.RemoteAccess;
import de.uniba.rz.backend.store.TicketStore;
import de.uniba.rz.entities.ticket.Status;
import de.uniba.rz.entities.ticket.Ticket;
import de.uniba.rz.entities.ticket.Type;
import de.uniba.rz.entities.udp.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class UdpRemoteAccess implements RemoteAccess {

    private static final Logger logger = LoggerFactory.getLogger(UdpRemoteAccess.class);
    private final String host;
    private final int port;
    private DatagramSocket serverSocket;
    private TicketStore ticketStore;
    private boolean active = false;

    public UdpRemoteAccess(String host, int port) {
        logger.info("UDP --> Initialization Successful {}:{}", host, port);
        this.host = host;
        this.port = port;
    }

    @Override
    public void prepareStartup(TicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    @Override
    public void shutdown() {
        active = false;
        serverSocket.close();
        logger.info("UDP --> Connection closed");
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(host))) {
            active = true;
            serverSocket = socket;
            logger.info("UDP --> Server ready to receive packets on {}", port);
            receivePackets();
        } catch (SocketException | UnknownHostException e) {
            logger.error("UDP --> Unable to start the server: {}", e.getLocalizedMessage());
        }
    }

    // Receives packets
    public void receivePackets() {
        byte[] receivedData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);
        while (active) {
            try {
                serverSocket.receive(receivePacket);
                Packet receivedPacket = deserializePacket(receivePacket.getData());
                switch (receivedPacket.getType()) {
                    case "createNewTicket":
                        createNewTicket(receivePacket, receivedPacket);
                        break;
                    case "getAllTickets":
                        getAllTickets(receivePacket);
                        break;
                    case "getTicketById":
                        getTicketByID(receivePacket, receivedPacket);
                        break;
                    case "getTicketsByName":
                        getTicketByName(receivePacket, receivedPacket);
                        break;
                    case "getTicketsByNameAndType":
                        getTicketsByNameAndType(receivePacket, receivedPacket);
                        break;
                    case "acceptTicket":
                    case "rejectTicket":
                    case "closeTicket":
                        updateTicketStatus(receivePacket, receivedPacket);
                        break;
                }
            } catch (IOException e) {
                logger.error("UDP --> Exception: {}", e.getLocalizedMessage(), e);
            }
        }
    }

    // Updates the status of a ticket
    private void updateTicketStatus(DatagramPacket receivePacket, Packet receivedPacket) throws IOException {
        int ticketId = (int) receivedPacket.getData();
        logger.info("{} --> TicketID: {}", receivedPacket.getType(), ticketId);
        for (Ticket t : ticketStore.getAllTickets()) {
            if (t.getId() == ticketId) {
                switch (receivedPacket.getType()) {
                    case "acceptTicket":
                        t.setStatus(Status.ACCEPTED);
                        break;
                    case "rejectTicket":
                        t.setStatus(Status.REJECTED);
                        break;
                    case "closeTicket":
                        t.setStatus(Status.CLOSED);
                        break;
                }
                sendTicketResponse(receivePacket, t);
                logger.info("{} --> Updated Ticket: {}", receivedPacket.getType(), t);
            }
        }
    }

    // Retrieves tickets by name and type and sends the response
    private void getTicketsByNameAndType(DatagramPacket receivePacket, Packet receivedPacket) throws IOException {
        Type receivedType = (Type) receivedPacket.getData();
        logger.info("getTicketsByNameAndType --> {}", receivedType);
        for (Ticket t : ticketStore.getAllTickets()) {
            if (t.getType() == receivedType) {
                sendTicketResponse(receivePacket, t);
                logger.info("getTicketsByNameAndType --> Response: {}", t);
            }
        }
    }

    // Retrieves a ticket by name and sends the response
    private void getTicketByName(DatagramPacket receivePacket, Packet receivedPacket) throws IOException {
        String receivedName = (String) receivedPacket.getData();
        logger.info("getTicketsByName --> {}", receivedName);
        for (Ticket t : ticketStore.getAllTickets()) {
            if (Objects.equals(t.getTopic(), receivedName)) {
                sendTicketResponse(receivePacket, t);
                logger.info("getTicketsByName --> Response: {}", t);
            }
        }
    }

    // Retrieves a ticket by ID and sends the response
    private void getTicketByID(DatagramPacket receivePacket, Packet receivedPacket) throws IOException {
        int receivedTicketId = (Integer) receivedPacket.getData();
        logger.info("getTicketById --> {}", receivedTicketId);
        for (Ticket t : ticketStore.getAllTickets()) {
            if (t.getId() == receivedTicketId) {
                sendTicketResponse(receivePacket, t);
                logger.info("getTicketById --> Response: {}", t);
            }
        }
    }

    // Retrieves all tickets and sends the response
    private void getAllTickets(DatagramPacket receivePacket) throws IOException {
        List<Ticket> allTickets = ticketStore.getAllTickets();
        logger.info("getAllTickets --> Fetching all tickets from ticketStore");
        sendTicketListResponse(receivePacket, allTickets);
        logger.info("getAllTickets --> Ticket Size: {}", allTickets.size());
    }

    // Creates a new ticket and sends the response
    private void createNewTicket(DatagramPacket receivePacket, Packet receivedPacket) throws IOException {
        Ticket receivedTicket = (Ticket) receivedPacket.getData();
        logger.info("createNewTicket --> {}", receivedTicket);
        Ticket savedTicket = ticketStore.storeNewTicket(receivedTicket.getReporter(), receivedTicket.getTopic(), receivedTicket.getDescription(), receivedTicket.getType(), receivedTicket.getPriority());
        byte[] ack = serializeObject("Ticket ID: " + savedTicket.getId());
        sendPacket(receivePacket.getAddress(), receivePacket.getPort(), ack);
        logger.info("createNewTicket --> response sent successfully");
    }

    // Sends a ticket as a response to the client
    private void sendTicketResponse(DatagramPacket receivePacket, Ticket ticket) throws IOException {
        byte[] response = serializeObject(ticket);
        sendPacket(receivePacket.getAddress(), receivePacket.getPort(), response);
    }

    // Sends a list of tickets as a response to the client
    private void sendTicketListResponse(DatagramPacket receivePacket, List<Ticket> ticketList) throws IOException {
        byte[] response = serializeObject(ticketList);
        sendPacket(receivePacket.getAddress(), receivePacket.getPort(), response);
    }

    // Sends a packet to the client
    private void sendPacket(InetAddress address, int port, byte[] data) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, port);
        serverSocket.send(sendPacket);
    }

    private byte[] serializeObject(Object object) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(object);
            return byteStream.toByteArray();
        }
    }

    private Packet deserializePacket(byte[] data) throws IOException {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
             ObjectInputStream objectStream = new ObjectInputStream(byteStream)) {
            return (Packet) objectStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize packet", e);
        }
    }
}
