package server.bibl;

import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.server.ResourceConfig;
import java.io.IOException;

import java.io.InputStream;

public class App {

    public static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) throws Exception {
        try {
            setupLogger();
        } catch (IOException e) {
            logger.severe("Error setting up logger!");
        }
        Properties properties = new Properties();
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.severe("Unable to load properties!");
                return;
            }
            properties.load(input);
        }

        String baseUrl = properties.getProperty("server.url");
        int port = Integer.parseInt(properties.getProperty("server.port"));

        String baseUri = baseUrl + ":" + port + "/";

        Server server = new Server(port);

        ResourceConfig config = new ResourceConfig();
        config.packages("server.bibl");

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        ServletContainer servletContainer = new ServletContainer(config);
        context.addServlet(servletContainer.getClass(), "/*");

        try {
            server.start();
            logger.info("Jersey app started at " + baseUri);
            server.join();
        } finally {
            server.destroy();
        }
    }

    private static void setupLogger() throws IOException {
        Files.createDirectories(Paths.get("logs"));
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String logFileName = "logs/library_server_" + timeStamp + ".log";

        FileHandler fileHandler = new FileHandler(logFileName, true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new SimpleFormatter());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(fileHandler);
    }
}
