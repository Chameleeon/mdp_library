package chat.server.connections;

import java.util.logging.Level;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.List;

public class ServerThread extends Thread {
  private SSLSocket socket;
  private String username;
  private ServerThread partner;
  private String partnerUsername;
  private BufferedReader in;
  private PrintWriter out;

  public ServerThread(SSLSocket socket, String username) {
    this.socket = socket;
    this.username = username;
    try {
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);
      ChatServer.logger.info("Connected: " + username);
    } catch (IOException e) {
      ChatServer.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }

  public void setSocket(SSLSocket socket) throws IOException {
    this.socket = socket;
    this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.out = new PrintWriter(socket.getOutputStream(), true);
    ChatServer.logger.info("Reconnected: " + username);
  }

  public void setPartner(ServerThread partner) {
    this.partner = partner;
    this.partnerUsername = partner.username;
    ChatServer.logger.info(username + " is now chatting with " + partnerUsername);
  }

  public void setPartnerUsername(String partnerUsername) {
    this.partnerUsername = partnerUsername;
    ChatServer.logger.info(username + " set partner to " + partnerUsername);
  }

  public void sendMessage(String message) {
    if (!socket.isClosed() && out != null) {
      out.println(message);
      out.flush();
      ChatServer.logger.info("Sent message to " + username + ": " + message);
    } else {
      ChatServer.logger.warning("Failed to send message to " + username + ": Socket closed");
    }
  }

  public void sendBacklogMessages() {
    if (partnerUsername != null) {
      List<String> backlog = ChatServer.getConversationMessages(username, partnerUsername);
      if (backlog != null) {
        for (String message : backlog) {
          sendMessage("backlog-" + message);
        }
      }
    }
  }

  @Override
  public void run() {
    String message;
    try {
      while ((message = in.readLine()) != null) {

        if (message.startsWith("CHAT")) {
          handleChatMessage(message);
        } else if (partner != null) {
          partner.sendMessage(username + ":" + message);
          ChatServer.addMessageToConversation(username, partner.username, username + ":" + message);
        } else if (partnerUsername != null) {
          ChatServer.addMessageToConversation(username, partnerUsername, username + ": " + message);
        } else {
          sendMessage("No partner connected. Waiting for a partner...");
        }
      }
      ChatServer.logger.info(username + " disconnected.");
    } catch (IOException e) {
      ChatServer.logger.log(Level.SEVERE, "I/O error with client " + username, e);
    } finally {
      cleanUp();
    }
  }

  private void handleChatMessage(String message) {
    String[] tokens = message.split(" ");
    if (tokens.length == 3) {
      String myUsername = tokens[1];
      String partnerUsername = tokens[2];

      synchronized (ChatServer.userThreads) {
        ServerThread partnerThread = ChatServer.userThreads.get(partnerUsername);

        if (partnerThread != null) {
          setPartner(partnerThread);
          partnerThread.setPartner(this);
          sendBacklogMessages();
        } else {
          setPartnerUsername(partnerUsername);
          sendBacklogMessages();
          sendMessage("Partner is offline. You can send messages that will be delivered when they come online.");
        }
      }
    } else {
      sendMessage("Invalid CHAT command. Usage: CHAT myUsername partnerUsername");
    }
  }

  private void cleanUp() {
    try {
      if (socket != null && !socket.isClosed()) {
        ChatServer.logger.info("Closing connection for " + username);
        socket.close();
      }
    } catch (IOException e) {
      ChatServer.logger.log(Level.SEVERE, "Error closing socket for " + username, e);
    } finally {
      synchronized (ChatServer.userThreads) {
        if (username != null && ChatServer.userThreads.containsKey(username)) {
          ChatServer.userThreads.remove(username);
        }
      }
    }
  }
}
