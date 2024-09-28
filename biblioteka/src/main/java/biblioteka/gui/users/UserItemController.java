package biblioteka.gui.users;

import java.util.logging.Level;
import biblioteka.App;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import java.io.IOException;

public class UserItemController {

  @FXML
  private HBox root;

  @FXML
  private Label username_label;

  @FXML
  private Label name_label;

  @FXML
  private Label lastname_label;

  @FXML
  private Label email_label;

  @FXML
  private Label status_label;

  public void setData(User user, UserController parent) {
    username_label.setText(user.getUsername());
    lastname_label.setText(user.getLastName());
    name_label.setText(user.getFirstName());
    email_label.setText(user.getEmail());
    status_label.setText(user.getStatus().toString().toUpperCase());
    FXMLLoader fxmlLoader;
    switch (user.getStatus()) {
      case ACTIVATED:
        fxmlLoader = new FXMLLoader(getClass().getResource("/biblioteka/active_buttons.fxml"));
        try {
          HBox hBox = fxmlLoader.load();
          ActiveBtnController abc = (ActiveBtnController) (fxmlLoader.getController());
          abc.setUserController(parent);
          abc.setUsername(user.getUsername());
          root.getChildren().add(hBox);
          break;
        } catch (IOException e) {
          App.logger.log(Level.SEVERE, "An exception has occurred", e);
        }
        break;
      case PENDING:
        fxmlLoader = new FXMLLoader(getClass().getResource("/biblioteka/inactive_buttons.fxml"));
        try {
          HBox hBox = fxmlLoader.load();
          InactiveBtnController ibc = (InactiveBtnController) (fxmlLoader.getController());
          ibc.setUsername(user.getUsername());
          ibc.setUserController(parent);
          root.getChildren().add(hBox);
        } catch (IOException e) {
          App.logger.log(Level.SEVERE, "An exception has occurred", e);
        }
        break;
      case SUSPENDED:
        fxmlLoader = new FXMLLoader(getClass().getResource("/biblioteka/suspended_buttons.fxml"));
        try {
          HBox hBox = fxmlLoader.load();
          SuspendedBtnController sbc = (SuspendedBtnController) (fxmlLoader.getController());
          sbc.setUsername(user.getUsername());
          sbc.setUserController(parent);
          root.getChildren().add(hBox);
        } catch (IOException e) {
          App.logger.log(Level.SEVERE, "An exception has occurred", e);
        }
        break;
    }
  }
}
