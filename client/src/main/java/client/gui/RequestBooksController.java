package client.gui;

import java.util.logging.Level;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import com.jfoenix.controls.JFXButton;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class RequestBooksController {
    @FXML
    private HBox root;

    @FXML
    private Label username_label;

    @FXML
    private TextField title_field;

    @FXML
    private TextField author_field;

    @FXML
    private JFXButton btn_request;

    private String multicastAddress;
    private int multicastPort;

    @FXML
    private void initialize() {
        loadMulticastConfig();
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

    private void loadMulticastConfig() {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            multicastAddress = properties.getProperty("multicast.address");
            multicastPort = Integer.parseInt(properties.getProperty("multicast.port"));
        } catch (IOException e) {
            App.logger.log(Level.SEVERE, "An exception has occurred", e);
            showAlert("Greška pri uspostavljanju komunikacije!");
        }
    }

    @FXML
    private void handleBtnRequestAction() {
        String title = title_field.getText().trim();
        String author = author_field.getText().trim();

        if (title.isEmpty() || author.isEmpty()) {
            showAlert("Popunite sve podatke!");
            return;
        }

        String message = String.format(
                "Nova Knjiga Predložena od strane korisnika " + App.username + " :\nNaslov: %s\nAutor: %s",
                title, author);

        try {
            sendMulticastMessage(message);
            showAlert("Prijedlog uspješno poslan!");
        } catch (IOException e) {
            App.logger.log(Level.SEVERE, "An exception has occurred", e);
            showAlert("Neuspješno slanje, pokušajte ponovo kasnije!");
        }
    }

    private void sendMulticastMessage(String message) throws IOException {
        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);

        InetAddress group = InetAddress.getByName(multicastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastPort);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(packet);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("INFO");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

    public void setUsername() {
        username_label.setText(App.username);
    }
}
