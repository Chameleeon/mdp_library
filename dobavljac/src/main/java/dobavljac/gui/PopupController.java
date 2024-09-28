package dobavljac.gui;

import dobavljac.App;
import javafx.stage.Stage;
import java.util.logging.Level;
import bookkeeping.server.rmi.BookkeepingService;
import bookkeeping.server.rmi.Invoice;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.Random;
import dobavljac.BookWithAmount;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

public class PopupController {

  private BookWithAmount book;

  @FXML
  private Label title_label;

  @FXML
  private Label author_label;

  @FXML
  private Label amount_label;

  @FXML
  private JFXButton accept_btn;

  @FXML
  private JFXButton reject_btn;

  @FXML
  private void handleAcceptOrder() {
    if (book != null) {
      try {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
          if (input == null) {
            System.err.println("Unable to find config.properties");
            return;
          }
          properties.load(input);
        }

        String serverUrl = properties.getProperty("bibliotekaServer.url", "localhost");
        String port = properties.getProperty("bibliotekaServer.port", "8080");
        String apiEndpoint = "http://" + serverUrl + ":" + port + "/api/books";

        sendBookToServer(apiEndpoint);

        Invoice invoice = createInvoice(book);

        int rmiPort = Integer.parseInt(properties.getProperty("rmi.registry.port", "1099"));

        new Thread(() -> {
          try {
            Registry registry = LocateRegistry.getRegistry(rmiPort);
            BookkeepingService service = (BookkeepingService) registry.lookup("BookkeepingService");

            service.serializeInvoice(invoice);

            double taxAmount = service.calculateTax(invoice.getTotal());

            Platform
                .runLater(() -> showAlert("Iznos PDV", "Cijena bez PDV: " + String.format("%.2f", invoice.getTotal())
                    + "\nIznos PDV za narudžbu: " + String.format("%.2f", taxAmount)));

          } catch (Exception e) {
            App.logger.log(Level.SEVERE, "An exception has occurred", e);

            Platform.runLater(() -> showAlert("Error", "Failed to connect to RMI service: " + e.getMessage()));
          }
        }).start();

      } catch (Exception e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    }
    Stage window = (Stage) accept_btn.getScene().getWindow();
    window.close();
  }

  private void sendBookToServer(String apiEndpoint) throws IOException {
    URL url = new URL(apiEndpoint);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);

    Gson gson = new Gson();
    String jsonInputString = gson.toJson(book);

    try (OutputStream os = connection.getOutputStream()) {
      byte[] input = jsonInputString.getBytes("utf-8");
      os.write(input, 0, input.length);
    }

    int code = connection.getResponseCode();
    if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_CREATED) {
      System.out.println("Book successfully sent to the server.");
    } else {
      System.err.println("Failed to send book to server. Response code: " + code);
      try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
          response.append(responseLine.trim());
        }
        System.err.println("Response: " + response.toString());
      }
    }
  }

  private Invoice createInvoice(BookWithAmount book) {
    Invoice invoice = new Invoice();
    Random random = new Random();
    float price = (15 + 10 * random.nextFloat(0.3f));
    invoice.setTotal(book.getAmount() * price);
    invoice.setDate(new Date());
    bookkeeping.server.books.BookWithAmount book2 = new bookkeeping.server.books.BookWithAmount();
    bookkeeping.server.books.Book tmp = new bookkeeping.server.books.Book();
    tmp.setTitle(book.getBook().getTitle());
    tmp.setAuthor(book.getBook().getAuthor());
    tmp.setCredits(book.getBook().getCredits());
    tmp.setTranslators(book.getBook().getTranslators());
    tmp.setLanguage(book.getBook().getLanguage());
    tmp.setReleaseDate(book.getBook().getReleaseDate());
    tmp.setCoverLink(book.getBook().getCoverLink());
    book2.setBook(tmp);
    book2.setAmount(book.getAmount());

    HashMap<bookkeeping.server.books.BookWithAmount, Float> books = new HashMap<>();
    books.put(book2, book.getAmount() * price);
    invoice.setBooks(books);
    return invoice;
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  @FXML
  private void handleRejectOrder() {
    Stage window = (Stage) reject_btn.getScene().getWindow();
    window.close();
  }

  public void setOrderDetails(BookWithAmount book) {
    this.book = book;
    title_label.setText(book.getBook().getTitle());
    author_label.setText(book.getBook().getAuthor());
    amount_label.setText(String.valueOf(book.getAmount()));
  }
}
