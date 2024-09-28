package dobavljac.server.connection;

import java.util.Date;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.ArrayList;
import java.util.List;

public class Server {

    public static final Logger logger = Logger.getLogger(Server.class.getName());
    public static List<String> providers = new ArrayList<>();

    public static void main(String[] args) {
        try {

            setupLogger();

            Properties properties = new Properties();
            try (InputStream input = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    logger.severe("Sorry, unable to find config.properties");
                    return;
                }
                properties.load(input);
            }

            String serverUrl = properties.getProperty("server.url", "localhost");
            int port = Integer.parseInt(properties.getProperty("server.port"));

            InetSocketAddress socketAddress = new InetSocketAddress(serverUrl, port);
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(socketAddress);
            logger.info(String.format("Server started at %s:%d", serverUrl, port));

            while (true) {

                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected from " + clientSocket.getInetAddress());

                new Thread(new ServerThread(clientSocket)).start();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "An exception occurred", e);
        }
    }

    private static void setupLogger() throws IOException {
        Files.createDirectories(Paths.get("logs"));
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String logFileName = "logs/delivery_server_" + timeStamp + ".log";

        FileHandler fileHandler = new FileHandler(logFileName, true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new SimpleFormatter());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(fileHandler);
    }
}
