package biblioteka;

import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import biblioteka.connection.MulticastReceiver;
import biblioteka.connection.Book;
import biblioteka.connection.BookFetcher;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class App extends Application {

    public static final Logger logger = Logger.getLogger(App.class.getName());
    public static List<Book> availableBooks = new ArrayList<>();

    public static HashMap<Integer, String> indexProviderMap = new HashMap<>();

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("window.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Biblioteka");
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An exception has occurred", e);
        }
    }

    public static void main(String[] args) {
        try {
            setupLogger();
        } catch (IOException e) {
            logger.severe("Error setting up logger!");
        }
        Thread downloader = new Thread(() -> {
            synchronized (availableBooks) {
                availableBooks = new BookFetcher().fetchBooks();
            }
        });
        downloader.setDaemon(true);
        downloader.start();
        Thread listener = new Thread(() -> new MulticastReceiver().receiveMulticastMessages());
        listener.setDaemon(true);
        listener.start();
        launch(args);
    }

    private static void setupLogger() throws IOException {
        Files.createDirectories(Paths.get("logs"));
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String logFileName = "logs/library_" + timeStamp + ".log";

        FileHandler fileHandler = new FileHandler(logFileName, true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new SimpleFormatter());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(fileHandler);
    }

}
