package de.uniba.rz.app;

import de.uniba.rz.app.ampq.AMPQTicketManagementBackend;
import de.uniba.rz.app.http.HttpTicketManagementBackend;
import de.uniba.rz.app.rpc.RPCTicketManagementBackend;
import de.uniba.rz.app.udp.UdpTicketManagementBackend;
import de.uniba.rz.ui.swing.MainFrame;
import de.uniba.rz.ui.swing.SwingMainController;
import de.uniba.rz.ui.swing.SwingMainModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Main class to start the TicketManagement5000 client application Currently
 * only a local backend implementation is registered.<br>
 * <p>
 * To add additional implementations modify the method
 * <code>evaluateArgs(String[] args)</code>
 *
 * @see #evaluateArgs(String[])
 */
public class Main {

	public static MainFrame mf;
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	/**
	 * Starts the TicketManagement5000 application based on the given arguments
	 *
	 * <p>
	 * <b>TODO No changes needed here - but documentation of allowed args should
	 * be updated</b>
	 * </p>
	 *
	 * @param args
	 */
	public static void main(String[] args) throws IOException, TimeoutException {
		TicketManagementBackend backendToUse = evaluateArgs(args);

		SwingMainController control = new SwingMainController(backendToUse);
		SwingMainModel model = new SwingMainModel(backendToUse);
		mf = new MainFrame(control, model);

		control.setMainFrame(mf);
		control.setSwingMainModel(model);

		control.start();
	}

	/**
	 * Determines which {@link TicketManagementBackend} should be used by
	 * evaluating the given {@code args}.
	 * <p>
	 * If <code>null</code>, an empty array or an unknown argument String is
	 * passed, the default {@code LocalTicketManagementBackend} is used.
	 *
	 * <p>
	 * <b>This method must be modified in order to register new implementations
	 * of {@code TicketManagementBackend}.</b>
	 * </p>
	 *
	 * @param args a String array to be evaluated
	 * @return the selected {@link TicketManagementBackend} implementation
	 * @see TicketManagementBackend
	 */
	private static TicketManagementBackend evaluateArgs(String[] args) throws IOException, TimeoutException {
		if (args == null || args.length == 0) {
			LOGGER.info("No arguments passed. Using local backend implementation.");
			return new LocalTicketManagementBackend();
		} else {
			String host = args[1];
			int port = 0;
			String queueName = null;
			if (args[0].equals("amqp")) {
				queueName = args[2];
			} else {
				port = Integer.parseInt(args[2]);
			}
			switch (args[0]) {
				case "local":
					return new LocalTicketManagementBackend();
				case "udp":
					return new UdpTicketManagementBackend(host, port);
				case "amqp":
					return new AMPQTicketManagementBackend(host, queueName);
				case "http":
					return new HttpTicketManagementBackend(host, port);
				case "rpc":
					return new RPCTicketManagementBackend(host, port);
				default:
					LOGGER.warn("Unknown backend type. Using local backend implementation.");
					return new LocalTicketManagementBackend();
			}
		}
	}
}
