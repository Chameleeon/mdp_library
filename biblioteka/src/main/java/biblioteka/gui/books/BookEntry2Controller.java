package biblioteka.gui.books;

import java.util.logging.Level;
import biblioteka.App;
import java.io.*;
import biblioteka.connection.Book;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;

public class BookEntry2Controller {

  private Book book;
  private String urlPort;

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
  public void handlePublishButton() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/biblioteka/order_popup.fxml"));
      Parent popupRoot = loader.load();
      PopupController popupController = loader.getController();
      popupController.setOrderDetails(book);
      popupController.setUrlPort(urlPort);
      Stage popupStage = new Stage();
      popupStage.initModality(Modality.APPLICATION_MODAL);
      popupStage.setTitle("Narudžba");
      popupStage.setScene(new Scene(popupRoot));
      popupStage.showAndWait();
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  public void setBookDetails(Book book) {
    this.book = book;

    title_label.setText(this.book.getTitle());
    author_label.setText(this.book.getAuthor());
    release_date_label.setText(this.book.getReleaseDate());
    language_label.setText(this.book.getLanguage());

    Image defaultImage = new Image(getClass().getResource("/assets/book_icon.png").toString());

    try {
      Image coverImage = new Image(this.book.getCoverLink(), true);
      book_cover.setImage(coverImage);
    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      book_cover.setImage(defaultImage);
    }

    if (this.book.getCoverLink() == null || this.book.getCoverLink().isEmpty()) {
      book_cover.setImage(defaultImage);
    }
  }

  public void setUrlPort(String urlPort) {
    this.urlPort = urlPort;
  }
}
