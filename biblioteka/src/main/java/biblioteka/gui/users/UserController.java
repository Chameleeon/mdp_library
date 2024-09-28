package biblioteka.gui.users;

import java.util.Properties;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.logging.Level;
import biblioteka.App;
import java.util.List;
import java.util.ArrayList;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import com.jfoenix.controls.JFXButton;

public class UserController {

  @FXML
  private HBox root;

  @FXML
  private JFXButton btn_home;

  @FXML
  private JFXButton btn_books;

  @FXML
  private VBox user_list;

  @FXML
  public void initialize() {
    btn_home.setOnAction(event -> loadScene("/biblioteka/window.fxml"));
    btn_books.setOnAction(event -> loadScene("/biblioteka/books.fxml"));
    List<User> users = fetchUsers();
    for (User user : users) {
      FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/biblioteka/user_entry.fxml"));

      try {
        HBox hBox = fxmlLoader.load();
        UserItemController uic = fxmlLoader.getController();
        uic.setData(user, this);
        user_list.getChildren().add(hBox);
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

  public void refreshUsers() {
    Node title = user_list.getChildren().get(0);
    user_list.getChildren().clear();
    user_list.getChildren().add(title);
    List<User> users = fetchUsers();
    for (User user : users) {
      FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/biblioteka/user_entry.fxml"));

      try {
        HBox hBox = fxmlLoader.load();
        UserItemController uic = fxmlLoader.getController();
        uic.setData(user, this);
        user_list.getChildren().add(hBox);
      } catch (IOException e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    }
  }

  public static List<User> fetchUsers() {
    List<User> users = new ArrayList<>();
    try {
      HttpClient client = HttpClient.newHttpClient();
      Properties properties = new Properties();

      String url = "localhost";
      int port = 8080;
      try (InputStreamReader reader = new InputStreamReader(
          UserController.class.getResourceAsStream("config.properties"))) {
        properties.load(reader);
        url = properties.getProperty("bibliotekaServer.url");
        port = Integer.parseInt(properties.getProperty("bibliotekaServer.port"));
      } catch (IOException e) {
        App.logger.log(Level.SEVERE, "An exception occured while reading properties. ", e);
      }

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("http://" + url + ":" + port + "/api/users"))
          .GET()
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        Gson gson = new Gson();
        users = gson.fromJson(response.body(), new TypeToken<List<User>>() {
        }.getType());
      } else {
        App.logger.severe("Failed to fetch users. HTTP code: " + response.statusCode());
      }
    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }

    return users;
  }
}
