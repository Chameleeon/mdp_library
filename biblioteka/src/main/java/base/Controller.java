package base;

import javafx.scene.layout.StackPane;
import javafx.scene.image.ImageView;

import javafx.fxml.*;

public class Controller {

    @FXML
    private ImageView background; // Injected via FXML

    @FXML
    private StackPane apane1; // The root pane of your FXML

@FXML
    public void initialize() {
        if (background != null && apane1 != null) {
            background.fitWidthProperty().bind(apane1.widthProperty());
            background.fitHeightProperty().bind(apane1.heightProperty());
        }
    }
}
