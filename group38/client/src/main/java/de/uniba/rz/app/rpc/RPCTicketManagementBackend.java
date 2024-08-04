package de.uniba.rz.app.rpc;

import de.uniba.rz.app.Main;
import de.uniba.rz.app.TicketManagementBackend;
import de.uniba.rz.entities.ticket.Priority;
import de.uniba.rz.entities.ticket.Status;
import de.uniba.rz.entities.ticket.Ticket;
import de.uniba.rz.entities.ticket.Type;
import de.uniba.rz.io.rpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class RPCTicketManagementBackend extends TicketServiceGrpc.TicketServiceImplBase implements TicketManagementBackend {

    private final Logger logger = LoggerFactory.getLogger(RPCTicketManagementBackend.class);
    private final TicketServiceGrpc.TicketServiceBlockingStub syncStub;
    private final TicketServiceGrpc.TicketServiceStub asyncStub;
    private final ManagedChannel channel;

    public RPCTicketManagementBackend(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    public RPCTicketManagementBackend(ManagedChannelBuilder<?> channelBuilder) {
        this.channel = channelBuilder.build();
        this.syncStub = TicketServiceGrpc.newBlockingStub(this.channel);
        this.asyncStub = TicketServiceGrpc.newStub(this.channel);
    }

    @Override
    public void triggerShutdown() {
        if (!this.channel.isShutdown()) {
            try {
                this.channel.shutdown().awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("Error while shutting down : " + e.getLocalizedMessage());
            }
        }
    }

    @Override
    public Ticket createNewTicket(String reporter, String topic, String description, Type type, Priority priority) {
        TicketRequest ticketRequest = TicketRequest.newBuilder().setDescription(description)
                .setPriority(priority.toString()).setType(type.toString()).setReporter(reporter).setTopic(topic)
                .build();
        TicketResponse ticketResponse = this.syncStub.createTicket(ticketRequest);
        Ticket createdTicket = storeBackTicket(ticketResponse);
        broadcastReceiver();
        return createdTicket;
    }

    @Override
    public Ticket acceptTicket(int id) {
        TicketId ticketIdRequest = TicketId.newBuilder().setTicketId(id).build();
        TicketResponse response = this.syncStub.acceptTicket(ticketIdRequest);
        Ticket acceptTicket = storeBackTicket(response);
        broadcastReceiver();
        return acceptTicket;
    }

    @Override
    public Ticket rejectTicket(int id) {
        TicketId ticketIdRequest = TicketId.newBuilder().setTicketId(id).build();
        TicketResponse response = this.syncStub.rejectTicket(ticketIdRequest);
        Ticket rejectTicket = storeBackTicket(response);
        broadcastReceiver();
        return rejectTicket;
    }

    @Override
    public Ticket closeTicket(int id) {
        TicketId ticketIdRequest = TicketId.newBuilder().setTicketId(id).build();
        TicketResponse response = this.syncStub.closeTicket(ticketIdRequest);
        Ticket closedTicket = storeBackTicket(response);
        broadcastReceiver();
        return closedTicket;
    }

    @Override
    public List<Ticket> getAllTickets() {
        Empty emptyRequest = Empty.newBuilder().build();
        TicketList ticketResponse = this.syncStub.getAllTicket(emptyRequest);
        return (List<Ticket>) SerializationUtils.deserialize(ticketResponse.getAllTickets().toByteArray());
    }

    private Ticket storeBackTicket(TicketResponse response) {
        Ticket tempTicket = new Ticket();
        tempTicket.setId(response.getId());
        tempTicket.setReporter(response.getReporter());
        tempTicket.setType(Type.valueOf(response.getType()));
        tempTicket.setPriority(Priority.valueOf(response.getPriority()));
        tempTicket.setStatus(Status.valueOf(response.getStatus()));
        tempTicket.setTopic(response.getTopic());
        tempTicket.setDescription(response.getDescription());
        return tempTicket;
    }

    @Override
    public Ticket getTicketById(int id) {
        TicketId ticketIdRequest = TicketId.newBuilder().setTicketId(id).build();
        TicketResponse response = this.syncStub.getTicketById(ticketIdRequest);
        return storeBackTicket(response);
    }

    public void broadcastReceiver() {
        new AutoUpdateFields(this.asyncStub).start();
    }

    class AutoUpdateFields extends Thread {

        private final TicketServiceGrpc.TicketServiceStub asyncStub;

        public AutoUpdateFields(TicketServiceGrpc.TicketServiceStub asycStub) {
            this.asyncStub = asycStub;
        }

        @Override
        public void run() {
            StreamObserver<AutoNewTicketRequest> observer = this.asyncStub.streamNewTicket(new StreamObserver<TicketList>() {

                @Override
                public void onNext(TicketList response) {
                    List<Ticket> ticketList = (List<Ticket>) SerializationUtils.deserialize(response.getAllTickets().toByteArray());
                    for (Ticket t : ticketList) {
                        logger.info("Ticket : " + t.toString());
                    }
                    Main.mf.updateTable(ticketList);
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("Some error while streaming" + t.getLocalizedMessage());
                }

                @Override
                public void onCompleted() {
                    logger.info("Ticket list updated.");
                }
            });
            observer.onNext(AutoNewTicketRequest.newBuilder().setTicketId(1).build());
        }
    }
}
