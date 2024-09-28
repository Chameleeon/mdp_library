package client.connectivity;

import java.util.logging.Level;
import client.gui.App;
import client.gui.GUIUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Properties;

public class MulticastReceiver {

  private String multicastAddress;
  private int multicastPort;

  private void loadMulticastConfig() {
    Properties properties = new Properties();
    try {
      properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
      multicastAddress = properties.getProperty("multicast.address");
      multicastPort = Integer.parseInt(properties.getProperty("multicast.port"));
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "Failed to load multicast configuration!", e);
    }
  }

  public void receiveMulticastMessages() {
    loadMulticastConfig();
    try (MulticastSocket socket = new MulticastSocket(multicastPort)) {
      InetAddress group = InetAddress.getByName(multicastAddress);
      socket.joinGroup(group);

      byte[] buffer = new byte[1024];
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

      while (true) {
        socket.receive(packet);
        String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
        GUIUtils.showAlert(message);
      }
    } catch (IOException e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
  }
}
