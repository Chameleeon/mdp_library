package client.gui;

import java.util.logging.Level;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.text.Text;
import java.io.IOException;

public class DashboardController {
  @FXML
  private HBox root;

  @FXML
  private JFXButton btn_home;

  @FXML
  private JFXButton btn_books;

  @FXML
  private JFXButton btn_order;

  @FXML
  private JFXButton btn_chat;

  @FXML
  private Label username_label;

  @FXML
  private Pane contentPane;

  @FXML
  public void initialize() {
    username_label.setText(App.username);
  }

  public void setUsername(String username) {
    username_label.setText(username);
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
