package de.uniba.rz.backend.config;

import de.uniba.rz.backend.TicketServerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    public static Properties loadProperties() {
        try (InputStream stream = TicketServerMain.class.getClassLoader().getResourceAsStream("application.properties")) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            LOGGER.error("Error while loading configuration " + e.getMessage());
            return null;
        }

    }

}
