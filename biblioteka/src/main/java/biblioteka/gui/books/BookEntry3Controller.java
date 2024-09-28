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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.stage.Modality;

public class BookEntry3Controller {

  private Book book;
  private Gson gson = new Gson();
  private BookWithContentAmount bookWithContentAmount;

  @FXML
  private HBox root;

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
  private void initialize() {
    root.setOnMouseClicked(event -> {
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/biblioteka/create_book_popup.fxml"));
        Parent popupRoot = loader.load();
        AddPopupController popupController = loader.getController();
        popupController.setBookForEdit(bookWithContentAmount);
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Izmijeni knjigu");
        popupStage.setScene(new Scene(popupRoot));
        popupStage.showAndWait();
      } catch (IOException e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    });
  }

  @FXML
  public void handleDeleteButton() {
    if (book != null) {

      BookWithContentAmount bookWithContentAmount = new BookWithContentAmount(new BookWithAmount(book, 0), "");

      boolean success = deleteBookFromApi(bookWithContentAmount);

      if (success) {

        HBox bookEntry = (HBox) book_cover.getParent();
        VBox parentVBox = (VBox) bookEntry.getParent();
        parentVBox.getChildren().remove(bookEntry);
      } else {
        App.logger.severe("Failed to delete the book from the API.");
      }
    }
  }

  private boolean deleteBookFromApi(BookWithContentAmount book) {
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
          "http://" + serverUrl + ":" + port + "/api/books/content?title="
              + book.getBook().getBook().getTitle().replaceAll("\\s", "%20")
              + "&author=" + book.getBook().getBook().getAuthor().replaceAll("\\s", "%20"));

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

  public void setBookWithContent(BookWithContentAmount bookWithContentAmount) {
    this.bookWithContentAmount = bookWithContentAmount;
  }
}
