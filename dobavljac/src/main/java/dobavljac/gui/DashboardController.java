package dobavljac.gui;

import java.util.logging.Level;
import dobavljac.App;
import dobavljac.BookLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.io.IOException;

public class DashboardController {

  @FXML
  private ProgressBar download_progress;

  @FXML
  private Label published_count_label;

  @FXML
  private Label available_count_label;

  @FXML
  private Label order_count_label;

  @FXML
  public void initialize() {
    published_count_label.setText("" + App.publishedBooks.size());
    available_count_label.setText("" + App.availableBooks.size());
    if (!App.initialized) {
      App.initialized = true;
      BookLoader bookLoader = new BookLoader(download_progress);
      bookLoader.loadBooksFromServer();
    } else {
      download_progress.setVisible(false);
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
