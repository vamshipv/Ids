package de.uniba.rz.backend;

import de.uniba.rz.backend.udp.UdpRemoteAccess;
import de.uniba.rz.backend.amqp.AMQPRemoteAccess;
import de.uniba.rz.backend.config.Configuration;
import de.uniba.rz.backend.store.SimpleTicketStore;
import de.uniba.rz.backend.store.TicketStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TicketServerMain {

	private static final Logger logger = LoggerFactory.getLogger(TicketServerMain.class);
	private static final Properties PROPERTIES = Configuration.loadProperties();
	public static void main(String[] args) throws IOException {
		TicketStore simpleTestStore = new SimpleTicketStore();
		List<RemoteAccess> remoteAccessImplementations = getAvailableRemoteAccessImplementations(args);

		// Starting remote access implementations:
		for (RemoteAccess implementation : remoteAccessImplementations) {
			implementation.prepareStartup(simpleTestStore);
			new Thread(implementation).start();
		}

		try (BufferedReader shutdownReader = new BufferedReader(new InputStreamReader(System.in))) {
			logger.info("Press enter to shutdown system.");
			shutdownReader.readLine();
			logger.info("Shutting down...");

			// Shutting down all remote access implementations
			for (RemoteAccess implementation : remoteAccessImplementations) {
				logger.info("Shutting down: "+implementation.getClass().getName());
				implementation.shutdown();
			}
			logger.info("Shutdown complete.");
			System.exit(0);
		}
	}

	private static List<RemoteAccess> getAvailableRemoteAccessImplementations(String[] args) {
		List<RemoteAccess> implementations = new ArrayList<>();
		implementations.add(
				new UdpRemoteAccess(
						PROPERTIES.getProperty("udp.host"),
						Integer.parseInt(PROPERTIES.getProperty("udp.port"))
				));
		implementations.add(
				new AMQPRemoteAccess(
						PROPERTIES.getProperty("amqp.host"),
						Integer.parseInt(PROPERTIES.getProperty("amqp.port")),
						PROPERTIES.getProperty("amqp.queueName"),
						PROPERTIES.getProperty("amqp.vHost"),
						PROPERTIES.getProperty("amqp.userName"),
						PROPERTIES.getProperty("amqp.password"),
						Boolean.parseBoolean(PROPERTIES.getProperty("amqp.autoDelete")),
						Boolean.parseBoolean(PROPERTIES.getProperty("amqp.durable")),
						PROPERTIES.getProperty("amqp.consumerTag")
				)
		);

		// TODO Add your implementations of the RemoteAccess interface
		// e.g.:
		// implementations.add(new UdpRemoteAccess(args[0], args[1]));

		return implementations;
	}
}
