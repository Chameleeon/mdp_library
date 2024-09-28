package bookkeeping.server.rmi;

import java.util.logging.Level;
import bookkeeping.server.App;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class BookkeepingServiceImpl extends UnicastRemoteObject implements BookkeepingService {

  private Gson gson;
  private String outputFolder;

  public BookkeepingServiceImpl() throws RemoteException {
    super();
    gson = new Gson();
    loadConfig();
  }

  private void loadConfig() {
    Properties properties = new Properties();
    try {

      properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
      outputFolder = properties.getProperty("invoice.output.folder");
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  @Override
  public void method() {
  }

  @Override
  public double calculateTax(double saleAmount) throws RemoteException {
    return saleAmount * 0.17;
  }

  @Override
  public void serializeInvoice(Invoice invoice) {

    String jsonInvoice = gson.toJson(invoice);

    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String filename = "invoice_" + timestamp + ".json";

    Path filePath = Paths.get(outputFolder, filename);

    try {

      if (Files.notExists(Paths.get(outputFolder))) {
        Files.createDirectories(Paths.get(outputFolder));
      }

      Files.write(filePath, jsonInvoice.getBytes());
      App.logger.info("Invoice serialized and saved to: " + filePath.toString());

    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

}
