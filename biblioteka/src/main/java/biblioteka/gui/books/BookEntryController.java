package biblioteka.gui.books;

import java.util.logging.Level;
import biblioteka.App;
import biblioteka.connection.Book;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class BookEntryController {

  private Book book;
  private Gson gson = new Gson();

  @FXML
  private ImageView book_cover;

  @FXML
  private Label title_label;

  @FXML
  private Label author_label;

  @FXML
  private Label release_date_label;

  @FXML
  private Label language_label;

  @FXML
  private Label amount_label;

  @FXML
  public void handleDeleteButton() {
    if (book != null) {

      BookWithAmount bookWithAmount = new BookWithAmount(book, 10);

      boolean success = deleteBookFromApi(bookWithAmount);

      if (success) {

        HBox bookEntry = (HBox) book_cover.getParent();
        VBox parentVBox = (VBox) bookEntry.getParent();
        parentVBox.getChildren().remove(bookEntry);
      } else {
        App.logger.severe("Failed to delete the book from the API.");
      }
    }
  }

  private boolean deleteBookFromApi(BookWithAmount book) {
    try {

      Properties properties = new Properties();
      try (InputStream input = BookController.class.getClassLoader().getResourceAsStream("config.properties")) {
        if (input == null) {
          App.logger.severe("Config properties file not found.");
          return false;
        }
        properties.load(input);
      }

      String serverUrl = properties.getProperty("bibliotekaServer.url", "localhost");
      int port = Integer.parseInt(properties.getProperty("bibliotekaServer.port"));

      URL url = new URL(
          "http://" + serverUrl + ":" + port + "/api/books?title=" + book.getBook().getTitle().replaceAll("\\s", "%20")
              + "&author=" + book.getBook().getAuthor().replaceAll("\\s", "%20"));
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("DELETE");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setDoOutput(true);

      String bookJson = gson.toJson(book);

      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = bookJson.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
        return true;
      } else {
        App.logger.severe("Failed to delete the book. Response code: " + responseCode);
        return false;
      }

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return false;
    }
  }

  public void setBookDetails(Book book, int amount) {
    this.book = book;

    title_label.setText(book.getTitle());
    author_label.setText(book.getAuthor());
    release_date_label.setText(book.getReleaseDate());
    language_label.setText(book.getLanguage());
    amount_label.setText("" + amount);

    Image defaultImage = new Image(getClass().getResource("/assets/book_icon.png").toString());

    try {
      Image coverImage = new Image(book.getCoverLink(), true);
      book_cover.setImage(coverImage);
    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      book_cover.setImage(defaultImage);
    }

    if (book.getCoverLink() == null || book.getCoverLink().isEmpty()) {
      book_cover.setImage(defaultImage);
    }
  }
}
