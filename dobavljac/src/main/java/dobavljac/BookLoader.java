package dobavljac;

import com.google.gson.Gson;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class BookLoader {

  private ProgressBar progressBar;

  public BookLoader(ProgressBar progressBar) {
    this.progressBar = progressBar;
  }

  public void loadBooksFromServer() {
    Task<List<Book>> task = new Task<>() {
      @Override
      protected List<Book> call() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
          if (input == null) {
            return null;
          }

          properties.load(input);

          String serverAddress = properties.getProperty("server.url");
          int serverPort = Integer.parseInt(properties.getProperty("server.port"));

          try (Socket socket = new Socket(serverAddress, serverPort);
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
              PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            writer.println("GET_BOOKS");

            int numberOfBooks = Integer.parseInt(reader.readLine());
            if (numberOfBooks <= 0) {
              return new ArrayList<>();
            }

            List<Book> books = new ArrayList<>();
            Gson gson = new Gson();
            long totalRead = 0;

            for (int i = 0; i < numberOfBooks; i++) {
              String bookJson = reader.readLine();
              Book book = gson.fromJson(bookJson, Book.class);
              books.add(book);

              totalRead++;
              updateProgress(totalRead, numberOfBooks);
            }

            return books;

          } catch (IOException e) {
            App.logger.log(Level.SEVERE, "An exception occurred", e);
            return null;
          }
        } catch (IOException e) {
          App.logger.log(Level.SEVERE, "An exception occurred", e);
          return null;
        }
      }

      @Override
      protected void succeeded() {
        super.succeeded();
        App.availableBooks = getValue();
        notifyUser("Knjige su uspješno preuzete sa servera!");
      }

      @Override
      protected void failed() {
        super.failed();
        notifyUser("Failed to load books.");
      }
    };

    progressBar.progressProperty().bind(task.progressProperty());
    new Thread(task).start();
  }

  private void notifyUser(String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Obavještenje");
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
