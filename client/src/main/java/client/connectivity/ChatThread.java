package client.connectivity;

import java.util.logging.Level;
import client.gui.App;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.util.function.Consumer;
import java.util.Properties;

public class ChatThread extends Thread {
  private String serverUrl;
  private int serverPort;
  private String keystorePath;
  private String keystorePassword;
  private SSLSocket socket;
  private BufferedReader in;
  private PrintWriter out;
  private String username;
  private String partnerUsername;
  private Consumer<String> messageConsumer;

  public ChatThread(String username, Consumer<String> messageConsumer) {
    this.username = username;
    this.messageConsumer = messageConsumer;
    loadConfig();
    System.setProperty("javax.net.ssl.trustStore", keystorePath);
    System.setProperty("javax.net.ssl.trustStorePassword", keystorePassword);

  }

  @Override
  public void run() {
    try {
      Thread.sleep(500);
      SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      socket = (SSLSocket) factory.createSocket(InetAddress.getByName(serverUrl), serverPort);
      App.logger.info("Connected to chat server at " + serverUrl + ":" + serverPort);

      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);

      sendMessage("CONNECT " + username);

      listenForMessages();

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  private void loadConfig() {
    Properties properties = new Properties();
    try {
      properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
      serverUrl = properties.getProperty("chatServer.url");
      serverPort = Integer.parseInt(properties.getProperty("chatServer.port"));
      keystorePath = properties.getProperty("keystore.path");
      keystorePassword = properties.getProperty("keystore.password");
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  public void sendMessage(String message) {
    if (out != null) {
      out.println(message);
      out.flush();
    }
  }

  private void listenForMessages() {
    String serverMessage;
    try {
      while ((serverMessage = in.readLine()) != null) {
        if (serverMessage.startsWith("backlog-")) {
          messageConsumer.accept(serverMessage.split("-", 2)[1]);
        } else if (serverMessage.split(":", 2)[0].equals(partnerUsername)) {
          messageConsumer.accept(serverMessage);
        }
      }
    } catch (IOException e) {
      App.logger.severe("Error receiving message from server: " + e.getMessage());
    }
  }

  public void closeConnection() {
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  public void connectWithUser(String username) {
    this.partnerUsername = username;
    if (out != null) {
      out.println("CHAT " + App.username + " " + username);
      out.flush();
    }
  }
}
