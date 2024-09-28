package biblioteka.gui.users;

import java.util.Properties;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.logging.Level;
import biblioteka.App;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class InactiveBtnController {

  private String url;
  private int port;

  @FXML
  private Button activate_btn;

  @FXML
  private Button reject_btn;

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
    activate_btn.setOnAction(event -> handleActivateAction());
    reject_btn.setOnAction(event -> handleRejectAction());
    loadConfig();
  }

  @FXML
  private void handleActivateAction() {
    if (username != null) {
      try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + url + ":" + port + "/api/users/activate?username=" + username))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          userController.refreshUsers();
        } else {
          App.logger.severe("Failed to activate user. HTTP code: " + response.statusCode());
        }
      } catch (Exception e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    }
  }

  @FXML
  private void handleRejectAction() {
    if (username != null) {
      try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + url + ":" + port + "/api/users/reject?username=" + username))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          userController.refreshUsers();
        } else {
          App.logger.severe("Failed to reject user. HTTP code: " + response.statusCode());
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
