package client.gui;

import java.util.logging.Level;
import javafx.scene.control.Label;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.io.IOException;

public class LoginController {

    public LoginController() {
        loadProperties();
    }

    @FXML
    private Label errorLabel;

    @FXML
    private Button rgstr_btn;

    @FXML
    private TextField username_field;

    @FXML
    private TextField passwd_field;

    @FXML
    private Button login_btn;

    private double xOffset = 0;
    private double yOffset = 0;

    private String serverUrl;
    private int serverPort;

    @FXML
    private void handleLoginButtonClick() {
        String username = username_field.getText();
        boolean invalid = false;

        if (username.equals("")) {
            updateBorderColor(username_field, "red");
            invalid = true;
        } else {
            updateBorderColor(username_field, "#67d8ef");
        }

        String password = passwd_field.getText();

        if (password.equals("")) {
            updateBorderColor(passwd_field, "red");
            invalid = true;
        } else {
            updateBorderColor(passwd_field, "#67d8ef");
        }

        if (!invalid) {
            boolean success = sendLoginRequest(username, password);
            if (success) {
                loadScene("window.fxml", username);
            }
        }
    }

    @FXML
    private void handleRegisterButtonClick() {
        loadScene("client_register.fxml");
    }

    @FXML
    private void handleCloseButtonClick() {
        System.exit(0);
    }

    private void addDragListeners(Parent root, Stage stage) {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private boolean sendLoginRequest(String username, String password) {
        try {
            String fullUrl = serverUrl + ":" + serverPort + "/api/users/login";
            URL url = new URL(fullUrl);

            String jsonInputString = "{\"username\": \"" + username + "\", \"passwordHash\": \"" + password + "\"}";

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED
                    || responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                errorLabel.setText("Korisničko ime ili lozinka nisu tačni.");
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                errorLabel.setText("Nalog nije aktiviran.\nMolimo vas sačekajte.");
            } else {
                errorLabel.setText("Prijava nije uspjela.\nPokušajte ponovo kasnije.");
            }

        } catch (Exception e) {
            App.logger.log(Level.SEVERE, "Server unreachable!", e);
        }
        return false;
    }

    private void loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                App.logger.severe("Sorry, unable to find config.properties");
                return;
            }
            properties.load(input);
            serverUrl = "http://" + properties.getProperty("server.url");
            serverPort = Integer.parseInt(properties.getProperty("server.port"));
        } catch (Exception e) {
            App.logger.log(Level.SEVERE, "An exception has occurred", e);
        }
    }

    public void updateBorderColor(TextField textField, String newColor) {
        String currentStyle = textField.getStyle();

        currentStyle = currentStyle.replaceAll("-fx-border-color: [^;]+;", "").trim();

        String newStyle = currentStyle + (currentStyle.isEmpty() ? "" : "; ") + "-fx-border-color: " + newColor + ";";

        textField.setStyle(newStyle);
    }

    private void loadScene(String fxmlFile) {
        loadScene(fxmlFile, null);
    }

    private void loadScene(String fxmlFile, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (username != null && controller instanceof DashboardController) {
                DashboardController dashboardController = (DashboardController) controller;
                dashboardController.setUsername(username);
                App.username = username;
            }

            Stage stage = (Stage) login_btn.getScene().getWindow();
            stage.close();

            Stage newStage = new Stage();
            Scene scene = new Scene(root);

            addDragListeners(root, newStage);

            newStage.setScene(scene);
            newStage.setTitle("Dashboard");
            newStage.show();

        } catch (IOException e) {
            App.logger.log(Level.SEVERE, "An exception has occurred", e);
        }
    }
}
