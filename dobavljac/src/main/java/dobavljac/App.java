package dobavljac;

import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import dobavljac.server.ServerThread;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

public class App extends Application {

    public static final Logger logger = Logger.getLogger(App.class.getName());
    public static boolean initialized = false;
    public static List<Book> availableBooks = new ArrayList<>();
    public static List<Book> publishedBooks = new ArrayList<>();

    private static Socket socket;
    private static PrintWriter writer;
    private static String clientServerUrl;
    private static int clientServerPort;
    private static String serverUrl;
    private static int serverPort;

    @Override
    public void start(Stage stage) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dobavljac/gui/dashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("Dashboard");

            stage.setOnCloseRequest(event -> sendClosingMessage());

            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An exception occurred", e);
        }
    }

    public static void main(String[] args) {

        try {
            setupLogger();
        } catch (IOException e) {
            logger.severe("Error setting up logger!");
        }
        startServerThread();

        launch();
    }

    private static void startServerThread() {
        Thread serverThread = new Thread(() -> {
            Properties properties = new Properties();

            try (InputStream input = App.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    logger.severe("Properties could not be loaded");
                    return;
                }
                properties.load(input);

                serverUrl = properties.getProperty("server.url");
                clientServerUrl = properties.getProperty("clientServer.url");
                serverPort = Integer.parseInt(properties.getProperty("server.port"));
                clientServerPort = Integer.parseInt(properties.getProperty("clientServer.port"));
                setupConnection(serverUrl, serverPort);

                registerClient();

                startClientServer();

            } catch (IOException e) {
                logger.log(Level.SEVERE, "An exception occurred", e);
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void registerClient() {
        InetSocketAddress serverSocketAddress = new InetSocketAddress(serverUrl, serverPort);
        try (Socket clSocket = new Socket()) {
            clSocket.connect(serverSocketAddress);

            writer.println("REGISTER " + clientServerUrl + ":" + clientServerPort);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An exception occurred", e);
        }
    }

    private static void startClientServer() {
        InetSocketAddress socketAddress = new InetSocketAddress(clientServerUrl, clientServerPort);
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(socketAddress);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(new ServerThread(clientSocket));
                clientThread.setDaemon(true);
                clientThread.start();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An exception occurred", e);
        }
    }

    private static void sendClosingMessage() {
        if (writer != null) {
            writer.println("REMOVE " + clientServerUrl + ":" + clientServerPort);
        }
        closeConnection();
    }

    private static void setupConnection(String host, int port) {
        try {
            InetSocketAddress address = new InetSocketAddress(host, port);
            socket = new Socket();
            socket.connect(address);

            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An exception occurred", e);
        }
    }

    private static void closeConnection() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An exception occurred", e);
        }
    }

    private static void setupLogger() throws IOException {
        Files.createDirectories(Paths.get("logs"));
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String logFileName = "logs/delivery_client_" + timeStamp + ".log";

        FileHandler fileHandler = new FileHandler(logFileName, true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new SimpleFormatter());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(fileHandler);
    }
}
