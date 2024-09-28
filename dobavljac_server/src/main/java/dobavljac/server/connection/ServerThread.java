package dobavljac.server.connection;

import dobavljac.server.books.Book;
import dobavljac.server.books.BookParser;
import dobavljac.server.books.BookWithContent;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ServerThread implements Runnable {

  private static final String BOOKS_FOLDER = "saved_books";
  private Socket clientSocket;

  public ServerThread(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

      String request;

      while ((request = reader.readLine()) != null) {

        if ("GET_BOOKS".equalsIgnoreCase(request)) {
          List<Book> books = getAllBooks();
          Gson gson = new Gson();

          writer.println(books.size());

          for (Book book : books) {
            String jsonBook = gson.toJson(book);
            writer.println(jsonBook);
          }
        } else if (request.startsWith("GET ")) {
          String bookTitle = request.substring(4).trim();
          BookWithContent bookWithContent = getBookByTitle(bookTitle);
          if (bookWithContent != null) {
            Gson gson = new Gson();
            String jsonResponse = gson.toJson(bookWithContent);
            writer.println(jsonResponse);
            writer.println("END_OF_BOOK");
          } else {
            writer.println("Book not found: " + bookTitle);
          }
        } else if ("REGISTER".equalsIgnoreCase(request.split(" ")[0])) {
          Server.providers.add(request.split(" ")[1]);
          break;
        } else if ("PROVIDERS".equalsIgnoreCase(request)) {
          for (String provider : Server.providers) {
            writer.println(provider);
            writer.flush();
          }
          break;
        } else if ("REMOVE".equalsIgnoreCase(request.split(" ")[0])) {
          Server.providers.remove(request.split(" ")[1]);
        } else if ("END".equalsIgnoreCase(request)) {
          writer.println("Connection closed");
          break;
        } else {
          writer.println("Unknown request: " + request);
        }
      }

    } catch (IOException e) {
      Server.logger.log(Level.SEVERE, "An exception occurred", e);
    } finally {
      try {
        clientSocket.close();
      } catch (IOException e) {
        Server.logger.log(Level.SEVERE, "An exception occurred", e);
      }
    }
  }

  private List<Book> getAllBooks() {
    List<Book> books = new ArrayList<>();
    File folder = new File(BOOKS_FOLDER);

    BookParser.saveAllBooks();

    File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

    if (files != null) {
      for (File file : files) {
        Book book = getBookFromFile(file.getName());
        if (book != null) {
          books.add(book);
        }
      }
    }
    return books;
  }

  private Book getBookFromFile(String fileName) {
    Gson gson = new Gson();
    try (BufferedReader reader = new BufferedReader(new FileReader(BOOKS_FOLDER + File.separator + fileName))) {
      JsonReader jsonReader = new JsonReader(reader);
      jsonReader.setLenient(false);
      Book book = gson.fromJson(jsonReader, Book.class);
      return book;
    } catch (JsonSyntaxException | IOException e) {
      Server.logger.log(Level.SEVERE, "An exception occurred", e);
      return null;
    }
  }

  private BookWithContent getBookByTitle(String title) {

    String fileName = title + ".json";
    File file = new File(BOOKS_FOLDER, fileName);

    if (file.exists()) {
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        StringBuilder jsonBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        String line;
        boolean inJson = true;

        while ((line = reader.readLine()) != null) {
          if (inJson) {
            if (line.trim().isEmpty() || line.trim().matches("^[A-Z ]+$")) {

              inJson = false;
            } else {
              jsonBuilder.append(line).append("\n");
            }
          } else {
            contentBuilder.append(line).append("\n");
          }
        }

        String jsonString = jsonBuilder.toString();
        String content = contentBuilder.toString();

        Gson gson = new Gson();
        JsonReader jsonReader = new JsonReader(new StringReader(jsonString));
        jsonReader.setLenient(true);
        Book book = gson.fromJson(jsonReader, Book.class);

        if (book != null && title.equalsIgnoreCase(book.getTitle() != null ? book.getTitle() : "")) {
          return new BookWithContent(book, content);
        }

      } catch (IOException e) {
        Server.logger.log(Level.SEVERE, "An exception occurred", e);
      } catch (JsonSyntaxException e) {
        Server.logger.severe("Error parsing JSON: " + e.getMessage());
      }
    } else {
      Server.logger.severe("File not found: " + fileName);
    }

    return null;
  }

}
