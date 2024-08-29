module base {
    requires javafx.controls;
    requires javafx.fxml;

    opens biblioteka to javafx.fxml;

    exports base;
}
