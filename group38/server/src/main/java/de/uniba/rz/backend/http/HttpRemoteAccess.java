package de.uniba.rz.backend.http;

import com.sun.net.httpserver.HttpServer;
import de.uniba.rz.backend.RemoteAccess;
import de.uniba.rz.backend.store.TicketStore;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;

public class HttpRemoteAccess implements RemoteAccess {

    private final String host;
    private final int port;
    HttpServer server;
    URI baseUri;
    ResourceConfig config;

    private Logger logger = LoggerFactory.getLogger(HttpRemoteAccess.class);

    public HttpRemoteAccess(String host, int port) {
        logger.info("HTTP : Initialization successful |" + host + ":" + port);
        this.host = host;
        this.port = port;
    }

    @Override
    public void prepareStartup(TicketStore ticketStore) {
        String serverUri = "http://" + host + ":" + port + "/";
        baseUri = UriBuilder.fromUri(serverUri).build();
        config = new ResourceConfig().packages("de.uniba.rz.backend.http.resources");
    }

    @Override
    public void shutdown() {
        server.stop(0);
        logger.info("HTTP :: Connection closed");
    }

    @Override
    public void run() {
        server = JdkHttpServerFactory.createHttpServer(baseUri, config);
        logger.info("HTTP :: Server is ready to take request on " + baseUri);
    }
}
