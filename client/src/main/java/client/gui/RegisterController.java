package client.gui;

import java.util.logging.Level;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javafx.scene.control.Label;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class RegisterController {

  @FXML
  private Button login_btn;

  @FXML
  private Label errorLabel;

  @FXML
  private TextField username_input;
  @FXML
  private TextField passwd_input;
  @FXML
  private TextField name_input;
  @FXML
  private TextField lastname_input;
  @FXML
  private TextField street_input;
  @FXML
  private TextField postcode_input;
  @FXML
  private TextField city_input;
  @FXML
  private TextField email_input;
  @FXML
  private TextField confirm_passwd_input;

  private double xOffset = 0;
  private double yOffset = 0;

  private String serverUrl;
  private int serverPort;

  public RegisterController() {
    loadProperties();
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

  @FXML
  private void handleLoginButtonClick() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("client_login.fxml"));
      Parent root = loader.load();

      Stage stage = (Stage) login_btn.getScene().getWindow();
      Scene scene = new Scene(root);

      addDragListeners(root, stage);

      stage.setScene(scene);
      stage.setTitle("Login");
      stage.show();

    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
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

  @FXML
  private void handleRegisterButtonClick() {
    String password = passwd_input.getText();
    String confirmPassword = confirm_passwd_input.getText();

    if (validateData()) {
      String firstName = name_input.getText();
      String lastName = lastname_input.getText();
      // TODO FIX SPLIT FOR LONGER STREET NAMES!
      String street = street_input.getText().split(" ")[0];
      String streetNumber = street_input.getText().split(" ")[1];
      String postcode = postcode_input.getText();
      String city = city_input.getText();
      String email = email_input.getText();
      String username = username_input.getText();

      sendRegisterRequest(firstName, lastName, street, streetNumber, postcode, city, email, username, password);

    }
  }

  private void sendRegisterRequest(String firstName, String lastName, String street, String streetNumber,
      String postcode, String city, String email, String username, String password) {
    try {

      String fullUrl = serverUrl + ":" + serverPort + "/api/users/register";
      URL url = new URL(fullUrl);

      String jsonInputString = String.format(
          "{\"firstName\": \"%s\", \"lastName\": \"%s\", \"address\": {\"street\": \"%s\", \"number\": \"%s\", \"city\": \"%s\", \"postcode\": \"%s\"}, \"username\": \"%s\", \"passwordHash\": \"%s\", \"email\": \"%s\", \"status\": \"%s\"}",
          firstName, lastName, street, streetNumber, city, postcode, username, password, email, "2");

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
      if (responseCode == HttpURLConnection.HTTP_CREATED) {
        errorLabel.setText("Uspješna registracija!");
        errorLabel.setStyle("-fx-text-fill: green;");
      } else if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
        errorLabel.setText("Korisničko ime već postoji.");
      } else {
        errorLabel.setText("Registracija nije uspješna, pokušajte ponovo kasnije.");
      }

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  private boolean validateData() {
    boolean emptyFields = false;
    ArrayList<TextField> fields = new ArrayList<TextField>();
    fields.add(name_input);
    fields.add(lastname_input);
    fields.add(street_input);
    fields.add(postcode_input);
    fields.add(city_input);
    fields.add(username_input);
    fields.add(passwd_input);
    fields.add(email_input);
    fields.add(confirm_passwd_input);

    for (TextField tf : fields) {
      if (tf.getText().equals("")) {
        emptyFields = true;
        updateBorderColor(tf, "red");
      } else {
        updateBorderColor(tf, "#67d8ef");
      }
    }

    if (emptyFields) {
      errorLabel.setText("Popunite prazna polja");
      return false;
    }

    if (passwd_input.getText().length() < 8) {
      errorLabel.setText("Dužina lozinke mora biti najmanje 8 karaktera");
      return false;
    }

    if (!validateUsername()) {
      errorLabel.setText("Dozvoljeni karakteri za korisničko ime su:\nMala i velika slova, brojevi, ., _, -, i !");
      return false;
    }

    if (!passwd_input.getText().equals(confirm_passwd_input.getText())) {
      errorLabel.setText("Lozinke se ne podudaraju");
      return false;
    }

    if (!validateEmail()) {
      errorLabel.setText("Unesite validnu e-mail adresu");
      return false;
    }

    return true;
  }

  private boolean validateEmail() {
    String emailPattern = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
        "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    Pattern pattern = Pattern.compile(emailPattern);
    return pattern.matcher(email_input.getText()).matches();
  }

  private boolean validateUsername() {
    String usernamePattern = "[a-zA-Z0-9_\\-\\.!]*";

    Pattern pattern = Pattern.compile(usernamePattern);
    return pattern.matcher(username_input.getText()).matches();
  }

  public void updateBorderColor(TextField textField, String newColor) {
    String currentStyle = textField.getStyle();

    currentStyle = currentStyle.replaceAll("-fx-border-color: [^;]+;", "").trim();

    String newStyle = currentStyle + (currentStyle.isEmpty() ? "" : "; ") + "-fx-border-color: " + newColor + ";";

    textField.setStyle(newStyle);
  }

}
