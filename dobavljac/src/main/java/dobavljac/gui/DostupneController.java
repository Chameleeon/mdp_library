package dobavljac.gui;

import dobavljac.App;
import java.util.logging.Level;
import dobavljac.Book;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class DostupneController {

  @FXML
  private VBox book_list;

  @FXML
  public void initialize() {
    populateBookList();
  }

  @FXML
  private void handleButtonHome(ActionEvent event) throws IOException {
    loadScene(event, "/dobavljac/gui/dashboard.fxml");
  }

  @FXML
  private void handleButtonIzdavanje(ActionEvent event) throws IOException {
    loadScene(event, "/dobavljac/gui/izdavanje.fxml");
  }

  @FXML
  private void handleButtonDostupne(ActionEvent event) throws IOException {
    loadScene(event, "/dobavljac/gui/dostupne.fxml");
  }

  @FXML
  private void handleButtonNarudzbe(ActionEvent event) throws IOException {
    loadScene(event, "/dobavljac/gui/narudzbe.fxml");
  }

  private void loadScene(ActionEvent event, String fxmlFile) throws IOException {
    Parent sceneParent = FXMLLoader.load(getClass().getResource(fxmlFile));
    Scene scene = new Scene(sceneParent);
    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
    window.setScene(scene);
    window.show();
  }

  private void populateBookList() {
    for (Book book : dobavljac.App.availableBooks) {
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/dobavljac/gui/book_entry2.fxml"));
        HBox bookEntry = loader.load();

        BookEntry2Controller controller = loader.getController();
        controller.setBookDetails(book);

        book_list.getChildren().add(bookEntry);
      } catch (IOException e) {
        App.logger.log(Level.SEVERE, "An exception has occurred", e);
      }
    }
  }
}
