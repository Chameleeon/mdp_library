package biblioteka.gui.books;

import java.util.logging.Level;
import biblioteka.App;
import java.net.Socket;
import biblioteka.connection.Book;

import javafx.stage.Stage;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class PopupController {

  private Book book;
  private String urlPort;

  @FXML
  private Label title_label;

  @FXML
  private Label author_label;

  @FXML
  private TextField amount_field;

  @FXML
  private JFXButton send_btn;

  @FXML
  private JFXButton cancel_btn;

  @FXML
  private void handleSendOrder() {
    orderBook(this.book, Integer.parseInt(amount_field.getText()));
    Stage window = (Stage) send_btn.getScene().getWindow();
    window.close();
  }

  @FXML
  private void handleCancelOrder() {
    Stage window = (Stage) cancel_btn.getScene().getWindow();
    window.close();
  }

  public void setOrderDetails(Book book) {
    this.book = book;
    title_label.setText(book.getTitle());
    author_label.setText(book.getAuthor());
  }

  private void orderBook(Book book, int amount) {
    try {

      Properties properties = new Properties();
      try (InputStream input = BookController.class.getClassLoader().getResourceAsStream("config.properties")) {
        if (input == null) {
          return;
        }
        properties.load(input);
      }

      String serverUrl = urlPort.split(":")[0];
      int port = Integer.parseInt(urlPort.split(":")[1]);

      try (Socket socket = new Socket(serverUrl, port);
          BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
          BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

        String orderString = "ORDER " + book.getTitle() + " " + amount;

        writer.write(orderString);
        writer.newLine();
        writer.flush();

      }

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  public void setUrlPort(String urlPort) {
    this.urlPort = urlPort;
  }
}
