package client.gui;

import java.util.logging.Level;
import client.books.BookWithContentAmount;
import client.books.BookWithContent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import client.books.Book;
import client.books.BookWithAmount;
import client.books.OrderRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class BookController {

  public List<Book> booksInCart = new ArrayList<>();
  public List<BookWithContent> booksWithContentInCart = new ArrayList<>();

  @FXML
  private HBox root;

  @FXML
  private VBox book_list;

  @FXML
  private TextField search_field;

  @FXML
  public void initialize() {
    populateBookListFromApi();
  }

  @FXML
  private void handleBtnHomeAction() {
    loadScene("window.fxml");
  }

  @FXML
  private void handleBtnBooksAction() {
    loadScene("books.fxml");
  }

  @FXML
  private void handleBtnOrderAction() {
    loadScene("request_books.fxml");
  }

  @FXML
  private void handleBtnChatAction() {
    loadScene("chat.fxml");
  }

  @FXML
  private void handleOrderButton() {
    new Thread(() -> sendOrderRequest(App.username)).start();
  }

  private void sendOrderRequest(String username) {
    if (booksInCart.isEmpty() && booksWithContentInCart.isEmpty()) {
      return;
    }

    Properties properties = new Properties();
    try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
      if (input == null) {
        App.logger.severe("Unable to find config.properties");
        return;
      }
      properties.load(input);
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return;
    }

    String serverUrl = properties.getProperty("server.url", "localhost");
    int port = Integer.parseInt(properties.getProperty("server.port", "80"));

    String fullUrl = "http://" + serverUrl + ":" + port + "/api/books/order";

    OrderRequest orderRequest = new OrderRequest();
    orderRequest.setBooks(booksInCart);
    orderRequest.setBooksWithContent(booksWithContentInCart);
    orderRequest.setUsername(username);

    Gson gson = new Gson();
    String json = gson.toJson(orderRequest);

    try {
      URL url = new URL(fullUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("Accept", "application/json");
      conn.setDoOutput(true);

      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = json.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
        App.logger.info("Order placed successfully.");
      } else {
        App.logger.severe("Failed to place order. Response code: " + responseCode);
      }

    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  @FXML
  public void handleIzdateButtonClick() {
    populateBookListFromApi();
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

        FXMLLoader loader = new FXMLLoader(getClass().getResource("title2.fxml"));
        HBox titleBar = loader.load();
        book_list.getChildren().add(titleBar);

        for (BookWithAmount bookWithAmount : booksWithAmount) {
          loadBookEntry(bookWithAmount.getBook(), null, bookWithAmount.getAmount(), false);
        }
      } catch (IOException e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    }

    if (booksWithContentAmount != null) {
      for (BookWithContentAmount bookWithContentAmount : booksWithContentAmount) {
        loadBookEntry(bookWithContentAmount.getBook().getBook(),
            new BookWithContent(bookWithContentAmount.getBook().getBook(), bookWithContentAmount.getContent()),
            bookWithContentAmount.getBook().getAmount(), true);
      }
    }
  }

  private void loadBookEntry(Book book, BookWithContent bookWithContent, int amount, boolean isCustom) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("book_entry.fxml"));
      HBox bookEntry = loader.load();

      BookEntryController controller = loader.getController();

      controller.setRootNode(bookEntry);

      controller.setBookController(this);

      if (isCustom) {
        controller.setCustomBook(true);
        controller.setBookDetails(bookWithContent, amount);
      }
      controller.setBookDetails(book, amount);

      book_list.getChildren().add(bookEntry);
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  public void addBookToCart(Book book) {
    booksInCart.add(book);
  }

  public void addBookWithContentToCart(BookWithContent book) {
    booksWithContentInCart.add(book);
  }

  public void replaceBookEntryWithCartEntry(BookEntryController entryController) {
    try {

      int index = -1;
      for (int i = 0; i < book_list.getChildren().size(); i++) {
        Node node = book_list.getChildren().get(i);
        if (node == entryController.getRootNode()) {
          index = i;
          break;
        }
      }

      if (index != -1) {

        book_list.getChildren().remove(index);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("book_entry2.fxml"));
        HBox newEntry = loader.load();

        BookEntry2Controller newEntryController = loader.getController();

        newEntryController.setRootNode(newEntry);

        newEntryController.setBookController(this);

        if (entryController.isCustom()) {
          newEntryController.setCustomBook(true);
          newEntryController.setBookDetails(entryController.getBookWithContent(), entryController.getAmount());
        } else {
          newEntryController.setBookDetails(entryController.getBook(), entryController.getAmount());
        }

        book_list.getChildren().add(index, newEntry);

      } else {
        App.logger.info("Entry not found in the list.");
      }
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  public void removeBookFromCart(Book book) {
    booksInCart.remove(book);
  }

  public void replaceCartEntryWithBookEntry(BookEntry2Controller entryController) {
    try {

      int index = -1;
      for (int i = 0; i < book_list.getChildren().size(); i++) {
        Node node = book_list.getChildren().get(i);
        if (node == entryController.getRootNode()) {
          index = i;
          break;
        }
      }

      if (index != -1) {

        book_list.getChildren().remove(index);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("book_entry.fxml"));
        HBox newEntry = loader.load();

        BookEntryController newEntryController = loader.getController();

        newEntryController.setRootNode(newEntry);

        newEntryController.setBookController(this);

        if (entryController.isCustom()) {
          newEntryController.setCustomBook(true);
          newEntryController.setBookDetails(entryController.getBookWithContent(), entryController.getAmount());
        } else {
          newEntryController.setBookDetails(entryController.getBook(), entryController.getAmount());
        }

        book_list.getChildren().add(index, newEntry);

      } else {
        App.logger.info("Entry not found in the list.");
      }
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
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

      String serverUrl = properties.getProperty("server.url", "localhost");
      int port = Integer.parseInt(properties.getProperty("server.port"));
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

      String serverUrl = properties.getProperty("server.url", "localhost");
      int port = Integer.parseInt(properties.getProperty("server.port"));
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
