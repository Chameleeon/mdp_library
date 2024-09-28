package chat.server.connections;

import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

public class ChatServer {
  public static final Logger logger = Logger.getLogger(ChatServer.class.getName());
  private static int port;
  private static String keystorePath;
  private static String keystorePassword;
  public static Map<String, ServerThread> userThreads = new HashMap<>();
  public static Map<String, List<String>> conversationMessages = new HashMap<>();

  public static void main(String[] args) {
    try {
      setupLogger();
    } catch (IOException e) {
      logger.severe("Error setting up logger!");
    }
    loadConfig();
    try {

      System.setProperty("javax.net.ssl.keyStore", keystorePath);
      System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);

      SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

      try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port)) {
        serverSocket.setNeedClientAuth(false);
        logger.info("Chat server started on port " + port);

        while (true) {
          SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
          new Thread(() -> handleClient(clientSocket)).start();
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  private static void handleClient(SSLSocket clientSocket) {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String initialMessage = in.readLine();
      String[] tokens = initialMessage.split(" ");

      if (tokens.length == 2 && tokens[0].equals("CONNECT")) {
        String username = tokens[1];
        synchronized (userThreads) {
          ServerThread existingThread = userThreads.get(username);
          if (existingThread != null) {
            existingThread.setSocket(clientSocket);
          } else {
            ServerThread newThread = new ServerThread(clientSocket, username);
            userThreads.put(username, newThread);
            newThread.start();
          }
        }
      } else {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("Invalid command.");
        clientSocket.close();
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  private static void loadConfig() {
    Properties properties = new Properties();
    try (InputStream input = ChatServer.class.getClassLoader().getResourceAsStream("config.properties")) {
      properties.load(input);
      port = Integer.parseInt(properties.getProperty("server.port"));
      keystorePath = properties.getProperty("keystore.path");
      keystorePassword = properties.getProperty("keystore.password");
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error loading configuration. Using default port 8443", e);
      port = 8443;
    }
  }

  public static void addMessageToConversation(String user1, String user2, String message) {
    String conversationKey = getConversationKey(user1, user2);

    synchronized (conversationMessages) {
      conversationMessages.computeIfAbsent(conversationKey, k -> new ArrayList<>()).add(message);
    }
  }

  public static List<String> getConversationMessages(String user1, String user2) {
    String conversationKey = getConversationKey(user1, user2);

    synchronized (conversationMessages) {
      return conversationMessages.getOrDefault(conversationKey, new ArrayList<>());
    }
  }

  private static String getConversationKey(String user1, String user2) {
    if (user1.compareTo(user2) < 0) {
      return user1 + "-" + user2;
    } else {
      return user2 + "-" + user1;
    }
  }

  private static void setupLogger() throws IOException {
    Files.createDirectories(Paths.get("logs"));
    String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    String logFileName = "logs/chat_server_" + timeStamp + ".log";

    FileHandler fileHandler = new FileHandler(logFileName, true);
    fileHandler.setLevel(Level.ALL);
    fileHandler.setFormatter(new SimpleFormatter());

    Logger rootLogger = Logger.getLogger("");
    rootLogger.addHandler(fileHandler);
  }
}
