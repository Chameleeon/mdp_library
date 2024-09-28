package client.gui;

import java.util.logging.Level;
import client.books.Book;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class DetailsPopupController {

  private Book book;
  private boolean isCustom;
  private String content = "";

  @FXML
  private Label title_label;

  @FXML
  private Label author_label;

  @FXML
  private TextArea book_text_area;

  @FXML
  private ImageView book_cover;

  private final Gson gson = new Gson();

  public void setBookDetails(Book book) {
    this.book = book;
    title_label.setText(book.getTitle());
    author_label.setText(book.getAuthor());

    String first100Lines = getFirst100LinesOfBook();
    book_text_area.setText(first100Lines);

    downloadAndSetCoverImage(book.getCoverLink());
  }

  public void setBookDetails(Book book, String content) {
    this.isCustom = true;
    this.content = content;
    setBookDetails(book);
  }

  private String getFirst100LinesOfBook() {
    if (isCustom) {
      String[] lines = content.split("\\r?\\n");
      StringBuilder result = new StringBuilder();
      int maxLines = Math.min(100, lines.length);
      for (int i = 0; i < maxLines; i++) {
        result.append(lines[i]);
        if (i < maxLines - 1) {
          result.append(System.lineSeparator());
        }
      }
      return result.toString();
    }

    Properties properties = new Properties();
    try {
      properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return "Failed to load configuration.";
    }

    String serverUrl = properties.getProperty("server.url");
    String serverPort = properties.getProperty("server.port");

    if (serverUrl == null || serverPort == null) {
      return "Server URL or port is not configured.";
    }

    String apiUrl = "http://" + serverUrl + ":" + serverPort + "/api/books/first100lines";

    try {
      URL url = new URL(apiUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setDoOutput(true);

      String jsonInputString = gson.toJson(book);

      try (java.io.OutputStream os = connection.getOutputStream()) {
        byte[] input = jsonInputString.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
          StringBuilder response = new StringBuilder();
          String line;
          while ((line = in.readLine()) != null) {
            response.append(line.trim());
          }
          return response.toString();
        }
      } else {
        return "Failed to retrieve book content. Server returned status code: " + responseCode;
      }
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return "Failed to connect to the server.";
    }
  }

  private void downloadAndSetCoverImage(String imageUrl) {
    try {
      URL url = new URL(imageUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();

      int responseCode = connection.getResponseCode();
      if (responseCode == 200) {
        Image coverImage = new Image(url.openStream());
        book_cover.setImage(coverImage);
      }
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }
}
