package bookkeeping.server;

import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import bookkeeping.server.rmi.BookkeepingServiceImpl;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class App {
    public static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) throws RemoteException {
        try {
            setupLogger();
        } catch (IOException e) {
            logger.severe("Error setting up logger!");
        }
        System.setProperty("java.security.policy", "security_policy.txt");

        Properties properties = new Properties();
        String url;
        int port;

        try {
            properties.load(App.class.getClassLoader().getResourceAsStream("config.properties"));
            url = properties.getProperty("rmi.registry.url");
            port = Integer.parseInt(properties.getProperty("rmi.registry.port"));

            BookkeepingServiceImpl service = new BookkeepingServiceImpl();

            Registry registry = LocateRegistry.createRegistry(port);

            registry.rebind("BookkeepingService", service);
            logger.info("Listening on: " + port);
            System.in.read();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "An exception has occurred", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An exception has occurred", e);
        }
    }

    private static void setupLogger() throws IOException {
        Files.createDirectories(Paths.get("logs"));
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String logFileName = "logs/bookkeeping_service_" + timeStamp + ".log";

        FileHandler fileHandler = new FileHandler(logFileName, true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new SimpleFormatter());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(fileHandler);
    }
}
