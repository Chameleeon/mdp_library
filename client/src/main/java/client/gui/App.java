package client.gui;

import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import client.user.User;
import client.connectivity.MulticastReceiver;
import javafx.stage.StageStyle;
import javafx.fxml.FXMLLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class App extends Application {

    public static final Logger logger = Logger.getLogger(App.class.getName());
    public static String username;
    public static List<User> allUsers = new ArrayList<>();
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("client_login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.initStyle(StageStyle.UNDECORATED);

            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });

            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });

            stage.setScene(scene);
            stage.setTitle("Biblioteka Klijent");
            stage.show();
        } catch (IOException e) {
            App.logger.log(Level.SEVERE, "An exception has occurred", e);
        }
    }

    public static void main(String[] args) {
        try {
            setupLogger();
        } catch (IOException e) {
            logger.severe("Error setting up logger!");
        }
        Thread t = new Thread(() -> new MulticastReceiver().receiveMulticastMessages());
        t.setDaemon(true);
        t.start();
        Thread infoFetcher = new Thread(() -> {
            Properties properties = new Properties();
            String serverUrl;
            int serverPort;
            try {
                properties.load(App.class.getClassLoader().getResourceAsStream("config.properties"));
                serverUrl = properties.getProperty("server.url", "localhost");
                serverPort = Integer.parseInt(properties.getProperty("server.port", "localhost"));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "An exception has occurred", e);
                return;
            }

            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://" + serverUrl + ":" + serverPort + "/api/users")).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                Gson gson = new Gson();
                Type listType = new TypeToken<List<User>>() {
                }.getType();
                allUsers = gson.fromJson(response.body(), listType);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An exception has occurred", e);
            }

        });
        infoFetcher.setDaemon(true);
        infoFetcher.start();
        launch(args);
    }

    private static void setupLogger() throws IOException {
        Files.createDirectories(Paths.get("logs"));
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String logFileName = "logs/client_" + timeStamp + ".log";

        FileHandler fileHandler = new FileHandler(logFileName, true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new SimpleFormatter());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(fileHandler);
    }

}
