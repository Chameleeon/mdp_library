package dobavljac.gui;

import dobavljac.App;
import dobavljac.Book;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.logging.Level;

public class BookEntry2Controller {

  private Book book;

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
    if (book != null) {
      App.availableBooks.remove(book);

      App.publishedBooks.add(book);
      HBox bookEntry = (HBox) book_cover.getParent();
      VBox parentVBox = (VBox) bookEntry.getParent();
      parentVBox.getChildren().remove(bookEntry);
    }
  }

  public void setBookDetails(Book book) {
    this.book = book;

    title_label.setText(book.getTitle());
    author_label.setText(book.getAuthor());
    release_date_label.setText(book.getReleaseDate());
    language_label.setText(book.getLanguage());

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
