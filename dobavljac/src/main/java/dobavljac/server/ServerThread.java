package dobavljac.server;

import com.google.gson.Gson;

import dobavljac.App;
import dobavljac.Book;
import dobavljac.BookWithAmount;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class ServerThread extends Thread {

  private Socket clientSocket;

  public ServerThread(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

      Properties properties = new Properties();
      InputStream input = ServerThread.class.getClassLoader().getResourceAsStream("config.properties");

      properties.load(input);

      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(properties.getProperty("rabbitmq.host"));
      factory.setUsername(properties.getProperty("rabbitmq.username"));
      factory.setPassword(properties.getProperty("rabbitmq.password"));

      try (Connection connection = factory.newConnection();
          Channel channel = connection.createChannel()) {

        String queueName = "order_queue";
        channel.queueDeclare(queueName, true, false, false, null);

        String request;

        while ((request = reader.readLine()) != null) {

          if ("GET_BOOKS".equalsIgnoreCase(request)) {

            List<Book> publishedBooks = App.publishedBooks;

            Gson gson = new Gson();

            writer.println(publishedBooks.size());

            for (Book book : publishedBooks) {
              String bookJson = gson.toJson(book);
              writer.println(bookJson);
            }

          } else if (request.toLowerCase().startsWith("order")) {
            String[] parts = request.split(" ");
            if (parts.length < 2) {
              writer.println("Invalid request!");
              continue;
            }

            String bookTitle = parts[1];
            for (int i = 2; i < parts.length - 1; i++) {
              bookTitle += " " + parts[i];
            }

            int bookAmount;
            try {
              bookAmount = Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException e) {
              writer.println("Invalid amount!");
              continue;
            }

            Book book = null;
            for (Book b : App.publishedBooks) {
              if (b.getTitle().equalsIgnoreCase(bookTitle)) {
                book = b;
                break;
              }
            }

            if (book == null) {
              writer.println("Book not found: " + bookTitle);
              continue;
            }

            BookWithAmount order = new BookWithAmount(book, bookAmount);

            Gson gson = new Gson();
            String orderJson = gson.toJson(order);

            channel.basicPublish("", queueName, null, orderJson.getBytes("UTF-8"));

            writer.println("Order received for " + bookTitle + " (Amount: " + bookAmount + ")");

          } else if ("END".equalsIgnoreCase(request)) {
            writer.println("Connection closed");
            break;
          } else {
            writer.println("Unknown request: " + request);
          }
        }

      } catch (Exception e) {
        App.logger.log(Level.SEVERE, "An exception occurred", e);
        writer.println("Server error occurred.");
      }

    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception occurred", e);
    } finally {
      try {
        clientSocket.close();
      } catch (IOException e) {
        App.logger.log(Level.SEVERE, "An exception occurred", e);
      }
    }
  }
}
