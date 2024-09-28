package biblioteka.gui.books;

import java.util.logging.Level;
import biblioteka.App;
import biblioteka.connection.Book;
import com.jfoenix.controls.JFXButton;
import com.google.gson.Gson;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AddPopupController {

    @FXML
    private TextField title_field;

    @FXML
    private TextField author_field;

    @FXML
    private TextField translator_field;

    @FXML
    private TextField date_field;

    @FXML
    private TextField language_field;

    @FXML
    private TextField cover_field;

    @FXML
    private TextField amount_field;

    @FXML
    private TextArea content_field;

    @FXML
    private JFXButton add_button;

    @FXML
    private JFXButton cancel_btn;

    @FXML
    private Label error_label;

    private boolean isEdit = false;

    private String serverUrl;
    private String serverPort;
    private String originalTitle;
    private String originalAuthor;

    public AddPopupController() {
        loadConfig();
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                error_label.setText("Sorry, unable to find config.properties");
                error_label.setVisible(true);
                return;
            }
            props.load(input);
            serverUrl = props.getProperty("bibliotekaServer.url");
            serverPort = props.getProperty("bibliotekaServer.port");
        } catch (Exception e) {
            App.logger.log(Level.SEVERE, "An exception has occurred", e);
            error_label.setText("Error loading configuration: " + e.getMessage());
            error_label.setVisible(true);
        }
    }

    public void setBookForEdit(BookWithContentAmount book) {
        title_field.setText(book.getBook().getBook().getTitle());
        author_field.setText(book.getBook().getBook().getAuthor());
        translator_field.setText(book.getBook().getBook().getTranslators());
        date_field.setText(book.getBook().getBook().getReleaseDate());
        language_field.setText(book.getBook().getBook().getLanguage());
        cover_field.setText(book.getBook().getBook().getCoverLink());
        amount_field.setText(String.valueOf(book.getBook().getAmount()));
        content_field.setText(book.getContent());

        originalTitle = book.getBook().getBook().getTitle();
        originalAuthor = book.getBook().getBook().getAuthor();

        isEdit = true;
        add_button.setText("Update Book");
    }

    @FXML
    void handleSendOrder(ActionEvent event) {
        String title = title_field.getText();
        String author = author_field.getText();
        String translator = translator_field.getText();
        String date = date_field.getText();
        String language = language_field.getText();
        String cover = cover_field.getText();
        String amount = amount_field.getText();
        String content = content_field.getText();

        error_label.setVisible(false);
        error_label.setText("");

        if (title.isEmpty() || author.isEmpty() || content.isEmpty() || amount.isEmpty()) {
            error_label.setText("Naslov, autor, količina i sadržaj su obavezni!");
            error_label.setVisible(true);
            return;
        }

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setTranslators(translator);
        book.setReleaseDate(date);
        book.setLanguage(language);
        book.setCoverLink(cover);

        BookWithAmount bwa = new BookWithAmount();
        bwa.setBook(book);
        bwa.setAmount(Integer.parseInt(amount));

        BookWithContentAmount bwc = new BookWithContentAmount(bwa, content);
        Gson gson = new Gson();
        String jsonInputString = gson.toJson(bwc);

        try {
            URL url;
            HttpURLConnection conn;

            if (isEdit) {

                if (!originalTitle.equals(title) || !originalAuthor.equals(author)) {

                    url = new URL("http://" + serverUrl + ":" + serverPort + "/api/books/content?title="
                            + originalTitle.replaceAll(" ", "%20")
                            + "&author=" + originalAuthor.replaceAll(" ", "%20"));
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    int deleteResponseCode = conn.getResponseCode();
                    if (deleteResponseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                        error_label.setText(
                                "Error: Could not delete the old book (Status code: " + deleteResponseCode + ")");
                        error_label.setVisible(true);
                        return;
                    }
                    url = new URL("http://" + serverUrl + ":" + serverPort + "/api/books/content");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                } else {
                    url = new URL("http://" + serverUrl + ":" + serverPort + "/api/books/content");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                }

            } else {

                url = new URL("http://" + serverUrl + ":" + serverPort + "/api/books/content");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
            }

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {

                title_field.clear();
                author_field.clear();
                translator_field.clear();
                date_field.clear();
                language_field.clear();
                cover_field.clear();
                amount_field.clear();
                content_field.clear();

                Stage stage = (Stage) cancel_btn.getScene().getWindow();
                stage.close();
            } else {
                error_label.setText("Error: Could not process the book (Status code: " + responseCode + ")");
                error_label.setVisible(true);
            }

        } catch (Exception e) {
            App.logger.log(Level.SEVERE, "An exception has occurred", e);
            error_label.setText("Error: " + e.getMessage());
            error_label.setVisible(true);
        }
    }

    @FXML
    void handleCancelOrder(ActionEvent event) {
        title_field.clear();
        author_field.clear();
        translator_field.clear();
        date_field.clear();
        language_field.clear();
        cover_field.clear();
        amount_field.clear();
        content_field.clear();

        Stage stage = (Stage) cancel_btn.getScene().getWindow();
        stage.close();
    }
}
