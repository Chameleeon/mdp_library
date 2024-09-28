package dobavljac.gui;

import dobavljac.App;
import java.util.logging.Level;
import dobavljac.BookWithAmount;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import com.google.gson.Gson;
import com.jfoenix.controls.JFXButton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class NarudzbeController {

  @FXML
  private JFXButton btn_home;

  @FXML
  private JFXButton btn_publish;

  @FXML
  private JFXButton btn_available;

  @FXML
  private JFXButton btn_orders;

  @FXML
  private Label published_count_label;

  @FXML
  private JFXButton next_btn;

  private Channel channel;

  @FXML
  public void initialize() {
    try {

      Properties properties = new Properties();
      InputStream input = NarudzbeController.class.getClassLoader().getResourceAsStream("config.properties");

      properties.load(input);

      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(properties.getProperty("rabbitmq.host"));
      factory.setUsername(properties.getProperty("rabbitmq.username"));
      factory.setPassword(properties.getProperty("rabbitmq.password"));

      Connection connection = factory.newConnection();
      channel = connection.createChannel();

      String queueName = "order_queue";
      channel.queueDeclare(queueName, true, false, false, null);

      long messageCount = channel.messageCount(queueName);
      published_count_label.setText(String.valueOf(messageCount));

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  @FXML
  private void handleNextOrder(ActionEvent event) throws IOException {
    try {

      GetResponse response = channel.basicGet("order_queue", true);
      if (response == null) {
        System.out.println("No messages in the queue");
        return;
      }

      String message = new String(response.getBody(), "UTF-8");

      Gson gson = new Gson();
      BookWithAmount book = gson.fromJson(message, BookWithAmount.class);

      System.out.println(book.toString());
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/dobavljac/gui/order_popup.fxml"));
      Parent popupRoot = loader.load();

      PopupController popupController = loader.getController();
      popupController.setOrderDetails(book);

      Stage popupStage = new Stage();
      popupStage.initModality(Modality.APPLICATION_MODAL);
      popupStage.setTitle("Next Order");
      popupStage.setScene(new Scene(popupRoot));
      popupStage.showAndWait();

      long messageCount = channel.messageCount("order_queue");
      published_count_label.setText(String.valueOf(messageCount));

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  @FXML
  private void handleButtonHome(ActionEvent event) throws IOException {
    loadScene(event, "dashboard.fxml");
  }

  @FXML
  private void handleButtonIzdavanje(ActionEvent event) throws IOException {
    loadScene(event, "izdavanje.fxml");
  }

  @FXML
  private void handleButtonDostupne(ActionEvent event) throws IOException {
    loadScene(event, "dostupne.fxml");
  }

  @FXML
  private void handleButtonNarudzbe(ActionEvent event) throws IOException {
    loadScene(event, "narudzbe.fxml");
  }

  private void loadScene(ActionEvent event, String fxmlFile) throws IOException {
    Parent sceneParent = FXMLLoader.load(getClass().getResource("/dobavljac/gui/" + fxmlFile));
    Scene scene = new Scene(sceneParent);
    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
    window.setScene(scene);
    window.show();
  }
}
