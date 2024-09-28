package biblioteka.connection;

import java.util.logging.Level;
import biblioteka.App;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BookFetcher {
  private int index = 0;

  public List<Book> fetchBooks() {
    Properties properties = new Properties();

    try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
      if (input == null) {
        App.logger.severe("Sorry, unable to find config.properties");
        return null;
      }

      properties.load(input);

      String providerUrl = properties.getProperty("providerServer.url");
      int providerPort = Integer.parseInt(properties.getProperty("providerServer.port"));

      List<String> providerAddresses = fetchProviders(providerUrl, providerPort);
      if (providerAddresses.isEmpty()) {
        return new ArrayList<>();
      }

      List<Book> allBooks = new ArrayList<>();
      for (String address : providerAddresses) {
        String[] urlPort = address.split(":");
        String url = urlPort[0];
        int port = Integer.parseInt(urlPort[1]);

        List<Book> books = fetchBooksFromProvider(url, port);
        if (books != null) {
          allBooks.addAll(books);
        }
      }

      return allBooks;

    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return null;
    }
  }

  private List<String> fetchProviders(String providerUrl, int providerPort) {
    List<String> providerAddresses = new ArrayList<>();

    try (Socket socket = new Socket(providerUrl, providerPort);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

      writer.println("PROVIDERS");

      String line;
      while ((line = reader.readLine()) != null && !line.isEmpty()) {
        providerAddresses.add(line);
      }

    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }

    return providerAddresses;
  }

  private List<Book> fetchBooksFromProvider(String providerUrl, int providerPort) {
    try (Socket socket = new Socket(providerUrl, providerPort);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

      writer.println("GET_BOOKS");

      int numberOfBooks = Integer.parseInt(reader.readLine());
      if (numberOfBooks <= 0) {
        return new ArrayList<>();
      }

      List<Book> books = new ArrayList<>();
      Gson gson = new Gson();

      for (int i = 0; i < numberOfBooks; i++) {
        String bookJson = reader.readLine();
        Book book = gson.fromJson(bookJson, Book.class);
        books.add(book);
        App.indexProviderMap.put(index, providerUrl + ":" + providerPort);
        index++;
      }

      return books;

    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return null;
    }
  }
}
