package biblioteka.gui.users;

import java.util.logging.Level;
import biblioteka.App;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import java.util.Properties;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ActiveBtnController {

  private String url;
  private int port;

  @FXML
  private Button suspend_button;

  @FXML
  private Button delete_button;

  private UserController userController;
  private String username;

  public void setUserController(UserController userController) {
    this.userController = userController;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @FXML
  public void initialize() {
    suspend_button.setOnAction(event -> handleSuspendAction());
    delete_button.setOnAction(event -> handleDeleteAction());
    loadConfig();
  }

  @FXML
  private void handleSuspendAction() {
    if (username != null) {
      try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + url + ":" + port + "/api/users/suspend?username=" + username))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          userController.refreshUsers();
        } else {
          App.logger.severe("Failed to suspend user. HTTP code: " + response.statusCode());
        }
      } catch (Exception e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    }
  }

  @FXML
  private void handleDeleteAction() {
    if (username != null) {
      try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + url + ":" + port + "/api/users/delete?username=" + username))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          userController.refreshUsers();
        } else {
          App.logger.severe("Failed to delete user. HTTP code: " + response.statusCode());
        }
      } catch (Exception e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    }
  }

  private void loadConfig() {
    Properties properties = new Properties();

    try (InputStreamReader reader = new InputStreamReader(this.getClass().getResourceAsStream("config.properties"))) {
      properties.load(reader);
      url = properties.getProperty("bibliotekaServer.url");
      port = Integer.parseInt(properties.getProperty("bibliotekaServer.port"));
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception occured while reading properties. ", e);
    }
  }
}
