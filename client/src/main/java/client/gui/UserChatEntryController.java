package client.gui;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;

public class UserChatEntryController {

  @FXML
  private JFXButton btn_home;

  public void setButtonText(String text) {
    btn_home.setText(text);
  }
}
