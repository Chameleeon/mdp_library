package client.gui;

import java.util.logging.Level;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import client.books.Book;
import client.books.BookWithContent;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class BookEntryController {

  private boolean customBook = false;
  private Book book;
  private BookWithContent bookWithContent;
  private int amount;
  private BookController bookController;
  private Gson gson = new Gson();

  private Node rootNode;

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
  public void handleCartButton() {
    if (bookController != null) {
      if (customBook) {
        bookController.addBookWithContentToCart(bookWithContent);
      } else {
        bookController.addBookToCart(book);
      }

      bookController.replaceBookEntryWithCartEntry(this);

      App.logger.info("Book added to cart: " + book.getTitle());
    } else {
      App.logger.warning("BookController is not set.");
    }
  }

  @FXML
  public void handleInfoButton() {
    try {

      FXMLLoader loader = new FXMLLoader(getClass().getResource("details_popup.fxml"));
      Parent popupRoot = loader.load();

      DetailsPopupController popupController = loader.getController();

      if (isCustom()) {
        popupController.setBookDetails(bookWithContent.getBook(), bookWithContent.getContent());
      } else {
        popupController.setBookDetails(this.book);
      }

      Scene popupScene = new Scene(popupRoot);
      Stage popupStage = new Stage();
      popupStage.setScene(popupScene);
      popupStage.setTitle("Book Details");

      popupStage.show();
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  public void setBookDetails(Book book, int amount) {
    this.book = book;
    this.amount = amount;

    title_label.setText(book.getTitle());
    author_label.setText(book.getAuthor());
    release_date_label.setText(book.getReleaseDate());
    language_label.setText(book.getLanguage());
    amount_label.setText(String.valueOf(amount));

    try {
      Image coverImage = new Image(book.getCoverLink(), true);
      book_cover.setImage(coverImage);
    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }

  }

  public void setBookDetails(BookWithContent book, int amount) {
    this.bookWithContent = book;
    this.amount = amount;
    this.customBook = true;

    title_label.setText(book.getBook().getTitle());
    author_label.setText(book.getBook().getAuthor());
    release_date_label.setText(book.getBook().getReleaseDate());
    language_label.setText(book.getBook().getLanguage());
    amount_label.setText(String.valueOf(amount));

    try {
      Image coverImage = new Image(book.getBook().getCoverLink(), true);
      book_cover.setImage(coverImage);
    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }

  }

  public void setBookController(BookController controller) {
    this.bookController = controller;
  }

  public Book getBook() {
    return this.book;
  }

  public void setRootNode(Node node) {
    this.rootNode = node;
  }

  public Node getRootNode() {
    return this.rootNode;
  }

  public int getAmount() {
    return this.amount;
  }

  public void setCustomBook(boolean isCustom) {
    this.customBook = isCustom;
  }

  public boolean isCustom() {
    return this.customBook;
  }

  public BookWithContent getBookWithContent() {
    return bookWithContent;
  }
}
