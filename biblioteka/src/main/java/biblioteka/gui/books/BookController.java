package biblioteka.gui.books;

import java.util.logging.Level;
import biblioteka.App;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import javafx.scene.Parent;
import javafx.stage.Modality;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jfoenix.controls.JFXButton;

import biblioteka.connection.Book;
import biblioteka.connection.BookFetcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BookController {
  @FXML
  private HBox root;

  @FXML
  private JFXButton btn_home;

  @FXML
  private JFXButton btn_users;

  @FXML
  private JFXButton btn_books;

  @FXML
  private VBox book_list;

  @FXML
  private TextField search_field;

  @FXML
  private JFXButton izdate_btn;

  @FXML
  private JFXButton dostupne_btn;

  @FXML
  public void initialize() {
    btn_home.setOnAction(event -> loadScene("/biblioteka/window.fxml"));
    btn_books.setOnAction(event -> loadScene("/biblioteka/books.fxml"));
    btn_users.setOnAction(event -> loadScene("/biblioteka/users.fxml"));
    populateBookListFromApi();
  }

  @FXML
  private void handleAddButtonAction() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/biblioteka/create_book_popup.fxml"));
      Parent popupRoot = loader.load();
      AddPopupController popupController = loader.getController();
      Stage popupStage = new Stage();
      popupStage.initModality(Modality.APPLICATION_MODAL);
      popupStage.setTitle("Dodaj knjigu");
      popupStage.setScene(new Scene(popupRoot));
      popupStage.showAndWait();
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  @FXML
  public void handleIzdateButtonClick() {
    populateBookListFromApi();
  }

  @FXML
  public void handleDostupneButtonClick() {
    biblioteka.App.availableBooks = new BookFetcher().fetchBooks();
    populateBookList(biblioteka.App.availableBooks);
  }

  @FXML
  public void handleSearch() {
    String searchQuery = search_field.getText();
  }

  private void populateBookListFromApi() {
    List<BookWithAmount> booksWithAmount = fetchBooksFromApi();
    List<BookWithContentAmount> booksWithContentAmount = fetchBooksContentFromApi();

    if (booksWithAmount != null) {
      try {
        book_list.getChildren().clear();
        FXMLLoader loader = null;
        HBox bookEntry = null;

        loader = new FXMLLoader(getClass().getResource("/biblioteka/title2.fxml"));
        bookEntry = loader.load();
        book_list.getChildren().add(bookEntry);
      } catch (IOException e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
      for (BookWithAmount bookWithAmount : booksWithAmount) {
        try {
          FXMLLoader loader = null;
          HBox bookEntry = null;

          loader = new FXMLLoader(getClass().getResource("/biblioteka/book_entry.fxml"));
          bookEntry = loader.load();

          BookEntryController controller = loader.getController();
          controller.setBookDetails(bookWithAmount.getBook(), bookWithAmount.getAmount());
          book_list.getChildren().add(bookEntry);

        } catch (IOException e) {
          App.logger.log(Level.SEVERE, "An exception has occurred", e);
        }
      }
    }
    if (booksWithContentAmount != null) {
      for (BookWithContentAmount bookWithContentAmount : booksWithContentAmount) {
        try {
          FXMLLoader loader = null;
          HBox bookEntry = null;

          loader = new FXMLLoader(getClass().getResource("/biblioteka/book_entry3.fxml"));
          bookEntry = loader.load();

          BookEntry3Controller controller = loader.getController();
          controller.setBookDetails(bookWithContentAmount.getBook().getBook(),
              bookWithContentAmount.getBook().getAmount());
          book_list.getChildren().add(bookEntry);
          controller.setBookWithContent(bookWithContentAmount);

        } catch (IOException e) {
          App.logger.log(Level.SEVERE, "An exception has occurred", e);
        }
      }

    }
  }

  private void populateBookList(java.util.List<Book> books) {
    try {
      book_list.getChildren().clear();
      FXMLLoader loader = null;
      HBox bookEntry = null;

      loader = new FXMLLoader(getClass().getResource("/biblioteka/title1.fxml"));
      bookEntry = loader.load();
      book_list.getChildren().add(bookEntry);
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }

    int ind = 0;
    for (Book book : books) {
      try {
        FXMLLoader loader = null;
        HBox bookEntry = null;
        loader = new FXMLLoader(getClass().getResource("/biblioteka/book_entry2.fxml"));
        bookEntry = loader.load();

        BookEntry2Controller controller = loader.getController();
        controller.setBookDetails(book);
        controller.setUrlPort(App.indexProviderMap.get(ind++));
        book_list.getChildren().add(bookEntry);
      } catch (IOException e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    }
  }

  private void loadScene(String fxmlFile) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
      StackPane newPane = new StackPane();
      Scene newScene = new Scene(newPane);
      newPane.getChildren().add(loader.load());
      Stage currentStage = (Stage) root.getScene().getWindow();
      currentStage.setScene(newScene);
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  private List<BookWithAmount> fetchBooksFromApi() {
    try {
      Properties properties = new Properties();
      try (InputStream input = BookController.class.getClassLoader().getResourceAsStream("config.properties")) {
        if (input == null) {
          return null;
        }
        properties.load(input);
      }

      String serverUrl = properties.getProperty("bibliotekaServer.url", "localhost");
      int port = Integer.parseInt(properties.getProperty("bibliotekaServer.port"));
      URL url = new URL("http://" + serverUrl + ":" + port + "/api/books");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Accept", "application/json");

      if (connection.getResponseCode() == 200) {
        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
        Gson gson = new Gson();
        List<BookWithAmount> booksWithAmount = gson.fromJson(reader, new TypeToken<List<BookWithAmount>>() {
        }.getType());
        reader.close();
        return booksWithAmount;
      } else {
        App.logger.severe("Failed to fetch books from API, response code: " + connection.getResponseCode());
        return null;
      }
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return null;
    }
  }

  private List<BookWithContentAmount> fetchBooksContentFromApi() {
    try {
      Properties properties = new Properties();
      try (InputStream input = BookController.class.getClassLoader().getResourceAsStream("config.properties")) {
        if (input == null) {
          return null;
        }
        properties.load(input);
      }

      String serverUrl = properties.getProperty("bibliotekaServer.url", "localhost");
      int port = Integer.parseInt(properties.getProperty("bibliotekaServer.port"));
      URL url = new URL("http://" + serverUrl + ":" + port + "/api/books/content/all");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Accept", "application/json");

      if (connection.getResponseCode() == 200) {
        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
        Gson gson = new Gson();
        List<BookWithContentAmount> booksWithAmount = gson.fromJson(reader,
            new TypeToken<List<BookWithContentAmount>>() {
            }.getType());
        reader.close();
        return booksWithAmount;
      } else {
        App.logger.severe("Failed to fetch books from API, response code: " + connection.getResponseCode());
        return null;
      }
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return null;
    }
  }
}
