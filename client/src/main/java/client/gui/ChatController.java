package client.gui;

import java.util.logging.Level;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.Pane;
import client.user.User;
import java.io.IOException;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import com.jfoenix.controls.JFXButton;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import client.connectivity.ChatThread;

public class ChatController {
  @FXML
  private HBox root;

  @FXML
  private TextField message_content;

  @FXML
  private VBox message_list_container;

  @FXML
  private VBox users_list_container;

  private ChatThread chatThread;

  @FXML
  public void initialize() {

    chatThread = new ChatThread(App.username, this::displayMessage);
    chatThread.setDaemon(true);
    chatThread.start();
    populateUsers();
  }

  @FXML
  private void handleBtnHomeAction() {
    loadScene("window.fxml");
  }

  @FXML
  private void handleBtnSendAction() {
    String messageToSend = message_content.getText();
    if (!messageToSend.isEmpty()) {

      chatThread.sendMessage(messageToSend);

      displayMessage(App.username + ":" + messageToSend);

      message_content.clear();
    }
  }

  public void displayMessage(String message) {
    Platform.runLater(() -> {
      String[] parts = message.split(":", 2);
      HBox hBox = new HBox();
      hBox.setAlignment(parts[0].equals(App.username) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
      hBox.setPadding(new Insets(5, 5, 5, 10));

      Text text;
      if (parts.length > 1) {
        text = new Text(parts[1]);
      } else {
        text = new Text(message);
      }
      TextFlow textFlow = new TextFlow(text);
      if (parts[0].equals(App.username)) {
        textFlow.setStyle("-fx-background-color: rgb(15,125,242); -fx-background-radius: 20px");
        text.setFill(Color.color(0.93, 0.945, 0.996));
      } else {
        textFlow.setStyle("-fx-background-color: rgb(233,233,235); -fx-background-radius: 20px");
        text.setFill(Color.BLACK);
      }

      textFlow.setPadding(new Insets(5, 10, 5, 10));
      hBox.getChildren().add(textFlow);
      message_list_container.getChildren().add(hBox);
    });
  }

  private void populateUsers() {

    for (User user : App.allUsers) {
      String username = user.getUsername();
      if (username.equals(App.username)) {
        continue;
      }
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("user_entry_chat.fxml"));
        Pane userEntry = loader.load();

        JFXButton btnHome = (JFXButton) userEntry.lookup("#user_btn");
        btnHome.setText(username);

        btnHome.setOnAction(event -> handleUserButtonAction(username));

        users_list_container.getChildren().add(userEntry);
      } catch (IOException e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    }
  }

  private void handleUserButtonAction(String username) {
    message_list_container.getChildren().clear();
    chatThread.connectWithUser(username);
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
}
