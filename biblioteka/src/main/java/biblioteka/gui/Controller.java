package biblioteka.gui;

import java.util.logging.Level;
import biblioteka.App;
import biblioteka.gui.users.UserController;
import biblioteka.gui.users.User;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import java.util.List;

import java.io.IOException;
import com.jfoenix.controls.JFXButton;

public class Controller {

    @FXML
    private ImageView background;

    @FXML
    private HBox root;

    @FXML
    private JFXButton btn_users;

    @FXML
    private JFXButton btn_books;

    @FXML
    private Label user_count_label;

    @FXML
    public void initialize() {
        List<User> users = UserController.fetchUsers();
        user_count_label.setText("" + users.size());
        btn_users.setOnAction(event -> loadScene("/biblioteka/users.fxml"));
        btn_books.setOnAction(event -> loadScene("/biblioteka/books.fxml"));
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
