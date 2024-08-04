package de.uniba.rz.backend.rpc;

import com.google.protobuf.ByteString;
import de.uniba.rz.backend.RemoteAccess;
import de.uniba.rz.backend.store.TicketStore;
import de.uniba.rz.entities.ticket.Priority;
import de.uniba.rz.entities.ticket.Ticket;
import de.uniba.rz.entities.ticket.Type;
import de.uniba.rz.io.rpc.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class GRPCRemoteAccess implements RemoteAccess {
    private final Logger logger = LoggerFactory.getLogger(GRPCRemoteAccess.class);
    private static TicketStore ticketStore;
    private final int port;
    private final Server server;

    public GRPCRemoteAccess(int port) {
        logger.info("GRPC :: Initialization successful | " + port);
        this.port = port;
        this.server = ServerBuilder.forPort(port).addService(new TicketManagementServiceImpl()).build();
    }

    @Override
    public void prepareStartup(TicketStore store) {
        ticketStore = store;
    }

    @Override
    public void shutdown() {
        if (server != null) {
            server.shutdown();
            logger.info("GRPC :: Connection closed");
        }
    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            logger.error("Failed to start server.." + e);
        }
    }

    public void start() throws IOException {
        server.start();
        logger.info("GRPC :: Server started and listened on port " + this.port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.error("Shutting down server");
            GRPCRemoteAccess.this.shutdown();
        }));
    }

    private static class TicketManagementServiceImpl extends TicketServiceGrpc.TicketServiceImplBase {
        private static final LinkedHashSet<StreamObserver<TicketList>> observers = new LinkedHashSet<>();

        @Override
        public void createTicket(TicketRequest request, StreamObserver<TicketResponse> responseObserver) {
            Ticket createdTicket = ticketStore.storeNewTicket(request.getReporter(),
                    request.getTopic(),
                    request.getDescription(),
                    Type.valueOf(request.getType()),
                    Priority.valueOf(request.getPriority()));
            TicketResponse ticketResponse = TicketResponse.newBuilder()
                    .setDescription(createdTicket.getDescription())
                    .setId(createdTicket.getId())
                    .setTopic(createdTicket.getTopic())
                    .setType(createdTicket.getType().toString())
                    .setPriority(createdTicket.getPriority().toString())
                    .setReporter(createdTicket.getReporter())
                    .setStatus(createdTicket.getStatus().toString())
                    .build();
            responseObserver.onNext(ticketResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void acceptTicket(TicketId request, StreamObserver<TicketResponse> responseObserver) {
            long ticketId = request.getTicketId();
            ticketStore.getAllTickets().forEach(ticket -> {
                if (ticket.getId() == ticketId) {
                    ticket.setStatus(de.uniba.rz.entities.ticket.Status.ACCEPTED);
                    TicketResponse ticketResponse = TicketResponse.newBuilder()
                            .setDescription(ticket.getDescription())
                            .setId(ticket.getId())
                            .setTopic(ticket.getTopic())
                            .setType(ticket.getType().toString())
                            .setPriority(ticket.getPriority().toString())
                            .setReporter(ticket.getReporter())
                            .setStatus(ticket.getStatus().toString())
                            .build();
                    responseObserver.onNext(ticketResponse);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public void rejectTicket(TicketId request, StreamObserver<TicketResponse> responseObserver) {
            long ticketId = request.getTicketId();
            ticketStore.getAllTickets().forEach(ticket -> {
                if (ticket.getId() == ticketId) {
                    ticket.setStatus(de.uniba.rz.entities.ticket.Status.REJECTED);
                    TicketResponse ticketResponse = TicketResponse.newBuilder()
                            .setDescription(ticket.getDescription())
                            .setId(ticket.getId())
                            .setTopic(ticket.getTopic())
                            .setType(ticket.getType().toString())
                            .setPriority(ticket.getPriority().toString())
                            .setReporter(ticket.getReporter())
                            .setStatus(ticket.getStatus().toString())
                            .build();
                    responseObserver.onNext(ticketResponse);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public void closeTicket(TicketId request, StreamObserver<TicketResponse> responseObserver) {
            long ticketId = request.getTicketId();
            ticketStore.getAllTickets().forEach(ticket -> {
                if (ticket.getId() == ticketId) {
                    ticket.setStatus(de.uniba.rz.entities.ticket.Status.CLOSED);
                    TicketResponse ticketResponse = TicketResponse.newBuilder()
                            .setDescription(ticket.getDescription())
                            .setId(ticket.getId())
                            .setTopic(ticket.getTopic())
                            .setType(ticket.getType().toString())
                            .setPriority(ticket.getPriority().toString())
                            .setReporter(ticket.getReporter())
                            .setStatus(ticket.getStatus().toString())
                            .build();
                    responseObserver.onNext(ticketResponse);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public void getTicketById(TicketId request, StreamObserver<TicketResponse> responseObserver) {
            long ticketId = request.getTicketId();
            ticketStore.getAllTickets().forEach(ticket -> {
                if (ticket.getId() == ticketId) {
                    TicketResponse ticketResponse = TicketResponse.newBuilder()
                            .setDescription(ticket.getDescription())
                            .setId(ticket.getId())
                            .setTopic(ticket.getTopic())
                            .setType(ticket.getType().toString())
                            .setPriority(ticket.getPriority().toString())
                            .setReporter(ticket.getReporter())
                            .setStatus(ticket.getStatus().toString())
                            .build();
                    responseObserver.onNext(ticketResponse);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public void getAllTicket(Empty request, StreamObserver<TicketList> responseObserver) {
            List<Ticket> tickets = ticketStore.getAllTickets();
            if (tickets == null) {
                tickets = new ArrayList<>();
            }
            TicketList allTicketResponse = TicketList
                    .newBuilder()
                    .setAllTickets(ByteString.copyFrom(SerializationUtils.serialize(
                            (Serializable) tickets)
                    )).build();
            responseObserver.onNext(allTicketResponse);
            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<AutoNewTicketRequest> streamNewTicket(StreamObserver<TicketList> responseObserver) {
            observers.add(responseObserver);
            return new StreamObserver<>() {
                @Override
                public void onNext(AutoNewTicketRequest value) {
                    TicketList ticketLS = TicketList.newBuilder()
                            .setAllTickets(ByteString.copyFrom(
                                    SerializationUtils.serialize(
                                            (Serializable) ticketStore.getAllTickets())
                            )).build();
                    observers.forEach(o -> o.onNext(ticketLS));
                }

                @Override
                public void onError(Throwable t) {
                    observers.remove(responseObserver);
                }

                @Override
                public void onCompleted() {
                    observers.remove(responseObserver);
                }
            };
        }
    }
}
