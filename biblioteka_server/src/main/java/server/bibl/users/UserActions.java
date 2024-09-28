package server.bibl.users;

import server.bibl.App;
import java.util.logging.Level;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.QueryParam;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.ArrayList;

@Path("/users")
public class UserActions {

  private static final String FILE_PATH = "users.xml";

  @POST
  @Path("/register")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response registerUser(User user) {
    try {

      if (usernameExists(user.getUsername())) {
        return Response.status(Response.Status.CONFLICT).entity("Username already exists").build();
      }

      String hashedPassword = hashPassword(user.getPasswordHash());

      User hashedUser = new User(
          user.getFirstName(),
          user.getLastName(),
          user.getAddress(),
          user.getUsername(),
          hashedPassword,
          user.getEmail(),
          UserStatus.PENDING);

      File xmlFile = new File(FILE_PATH);
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc;

      if (xmlFile.exists()) {
        doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();
      } else {
        doc = dBuilder.newDocument();
        Element rootElement = doc.createElement("users");
        doc.appendChild(rootElement);
      }

      Element newUser = doc.createElement("user");

      Element firstName = doc.createElement("firstName");
      firstName.appendChild(doc.createTextNode(hashedUser.getFirstName()));
      newUser.appendChild(firstName);

      Element lastName = doc.createElement("lastName");
      lastName.appendChild(doc.createTextNode(hashedUser.getLastName()));
      newUser.appendChild(lastName);

      Element address = doc.createElement("address");

      Element street = doc.createElement("street");
      street.appendChild(doc.createTextNode(hashedUser.getAddress().getStreet()));
      address.appendChild(street);

      Element number = doc.createElement("number");
      number.appendChild(doc.createTextNode(hashedUser.getAddress().getNumber()));
      address.appendChild(number);

      Element city = doc.createElement("city");
      city.appendChild(doc.createTextNode(hashedUser.getAddress().getCity()));
      address.appendChild(city);

      Element postcode = doc.createElement("postcode");
      postcode.appendChild(doc.createTextNode(hashedUser.getAddress().getPostcode()));
      address.appendChild(postcode);

      newUser.appendChild(address);

      Element username = doc.createElement("username");
      username.appendChild(doc.createTextNode(hashedUser.getUsername()));
      newUser.appendChild(username);

      Element passwordHash = doc.createElement("passwordHash");
      passwordHash.appendChild(doc.createTextNode(hashedUser.getPasswordHash()));
      newUser.appendChild(passwordHash);

      Element email = doc.createElement("email");
      email.appendChild(doc.createTextNode(hashedUser.getEmail()));
      newUser.appendChild(email);

      Element status = doc.createElement("status");
      status.appendChild(doc.createTextNode(hashedUser.getStatus().toString()));
      newUser.appendChild(status);

      doc.getDocumentElement().appendChild(newUser);

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(xmlFile);
      transformer.transform(source, result);

      return Response.status(Response.Status.CREATED).entity("User registered successfully").build();

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error registering user").build();
    }
  }

  private boolean usernameExists(String username) {
    try {
      File xmlFile = new File(FILE_PATH);
      if (!xmlFile.exists()) {
        return false;
      }

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();

      NodeList users = doc.getElementsByTagName("user");
      for (int i = 0; i < users.getLength(); i++) {
        Element userElement = (Element) users.item(i);
        String existingUsername = userElement.getElementsByTagName("username").item(0).getTextContent();
        if (existingUsername.equals(username)) {
          return true;
        }
      }
    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
    return false;
  }

  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response loginUser(User loginRequest) {
    try {
      File xmlFile = new File(FILE_PATH);
      if (!xmlFile.exists()) {
        return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
      }

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();

      NodeList users = doc.getElementsByTagName("user");
      for (int i = 0; i < users.getLength(); i++) {
        Element userElement = (Element) users.item(i);
        String existingUsername = userElement.getElementsByTagName("username").item(0).getTextContent();

        if (existingUsername.equals(loginRequest.getUsername())) {
          String storedPasswordHash = userElement.getElementsByTagName("passwordHash").item(0).getTextContent();
          String providedPasswordHash = hashPassword(loginRequest.getPasswordHash());

          if (storedPasswordHash.equals(providedPasswordHash)) {
            String userStatus = userElement.getElementsByTagName("status").item(0).getTextContent();
            if (!userStatus.equals(UserStatus.ACTIVATED.toString())) {
              return Response.status(Response.Status.FORBIDDEN).entity("Nalog nije aktiviran").build();
            }
            return Response.status(Response.Status.OK).entity("Uspješno prijavljivanje").build();
          } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Netačno korisničko ime ili lozinka").build();
          }
        }
      }

      return Response.status(Response.Status.NOT_FOUND).entity("Korisnik ne postoji").build();

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Greška pri prijavljivanju").build();
    }
  }

  private String hashPassword(String password) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hashedPassword = md.digest(password.getBytes());
    StringBuilder sb = new StringBuilder();
    for (byte b : hashedPassword) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  @POST
  @Path("/activate")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response activateUser(@QueryParam("username") String username) {
    try {
      File xmlFile = new File(FILE_PATH);
      if (!xmlFile.exists()) {
        return Response.status(Response.Status.NOT_FOUND).entity("User file not found").build();
      }

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();

      NodeList users = doc.getElementsByTagName("user");
      boolean userFound = false;

      for (int i = 0; i < users.getLength(); i++) {
        Element userElement = (Element) users.item(i);
        String currentUsername = userElement.getElementsByTagName("username").item(0).getTextContent();

        if (currentUsername.equals(username)) {
          userFound = true;

          Node statusNode = userElement.getElementsByTagName("status").item(0);
          statusNode.setTextContent("activated");

          TransformerFactory transformerFactory = TransformerFactory.newInstance();
          Transformer transformer = transformerFactory.newTransformer();
          DOMSource source = new DOMSource(doc);
          StreamResult result = new StreamResult(xmlFile);
          transformer.transform(source, result);

          return Response.status(Response.Status.OK).entity("User activated successfully").build();
        }
      }

      if (!userFound) {
        return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
      }

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error activating user").build();
    }

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error").build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response fetchUsers() {
    try {
      File xmlFile = new File(FILE_PATH);
      if (!xmlFile.exists()) {
        return Response.status(Response.Status.NOT_FOUND).entity("User file not found").build();
      }

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();

      NodeList usersNodeList = doc.getElementsByTagName("user");
      List<User> users = new ArrayList<>();

      for (int i = 0; i < usersNodeList.getLength(); i++) {
        Element userElement = (Element) usersNodeList.item(i);

        User user = new User();
        user.setFirstName(userElement.getElementsByTagName("firstName").item(0).getTextContent());
        user.setLastName(userElement.getElementsByTagName("lastName").item(0).getTextContent());

        Element addressElement = (Element) userElement.getElementsByTagName("address").item(0);
        Address address = new Address();
        address.setStreet(addressElement.getElementsByTagName("street").item(0).getTextContent());
        address.setNumber(addressElement.getElementsByTagName("number").item(0).getTextContent());
        address.setCity(addressElement.getElementsByTagName("city").item(0).getTextContent());
        address.setPostcode(addressElement.getElementsByTagName("postcode").item(0).getTextContent());
        user.setAddress(address);

        user.setUsername(userElement.getElementsByTagName("username").item(0).getTextContent());
        user.setPasswordHash(userElement.getElementsByTagName("passwordHash").item(0).getTextContent());
        user.setEmail(userElement.getElementsByTagName("email").item(0).getTextContent());
        user.setStatus(
            UserStatus.valueOf(userElement.getElementsByTagName("status").item(0).getTextContent().toUpperCase()));

        users.add(user);
      }

      Gson gson = new Gson();
      String jsonResponse = gson.toJson(users, new TypeToken<List<User>>() {
      }.getType());

      return Response.ok(jsonResponse, MediaType.APPLICATION_JSON).build();

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error fetching users").build();
    }
  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteUser(@QueryParam("username") String username) {
    try {
      File xmlFile = new File(FILE_PATH);
      if (!xmlFile.exists()) {
        return Response.status(Response.Status.NOT_FOUND).entity("User file not found").build();
      }

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();

      NodeList users = doc.getElementsByTagName("user");
      boolean userFound = false;

      for (int i = 0; i < users.getLength(); i++) {
        Element userElement = (Element) users.item(i);
        String currentUsername = userElement.getElementsByTagName("username").item(0).getTextContent();

        if (currentUsername.equals(username)) {
          userFound = true;
          userElement.getParentNode().removeChild(userElement);

          TransformerFactory transformerFactory = TransformerFactory.newInstance();
          Transformer transformer = transformerFactory.newTransformer();
          DOMSource source = new DOMSource(doc);
          StreamResult result = new StreamResult(xmlFile);
          transformer.transform(source, result);

          return Response.status(Response.Status.OK).entity("User deleted successfully").build();
        }
      }

      if (!userFound) {
        return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
      }

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error deleting user").build();
    }

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error").build();
  }

  @POST
  @Path("/suspend")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response suspendUser(@QueryParam("username") String username) {
    try {
      File xmlFile = new File(FILE_PATH);
      if (!xmlFile.exists()) {
        return Response.status(Response.Status.NOT_FOUND).entity("User file not found").build();
      }

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();

      NodeList users = doc.getElementsByTagName("user");
      boolean userFound = false;

      for (int i = 0; i < users.getLength(); i++) {
        Element userElement = (Element) users.item(i);
        String currentUsername = userElement.getElementsByTagName("username").item(0).getTextContent();

        if (currentUsername.equals(username)) {
          userFound = true;

          Node statusNode = userElement.getElementsByTagName("status").item(0);
          statusNode.setTextContent("SUSPENDED");

          TransformerFactory transformerFactory = TransformerFactory.newInstance();
          Transformer transformer = transformerFactory.newTransformer();
          DOMSource source = new DOMSource(doc);
          StreamResult result = new StreamResult(xmlFile);
          transformer.transform(source, result);

          return Response.status(Response.Status.OK).entity("User suspended successfully").build();
        }
      }

      if (!userFound) {
        return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
      }

    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error suspending user").build();
    }

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error").build();
  }

  public String getUserEmail(String username) throws Exception {
    File xmlFile = new File(FILE_PATH);
    if (!xmlFile.exists()) {
      return null; // Or throw an exception if preferred
    }

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(xmlFile);
    doc.getDocumentElement().normalize();

    NodeList users = doc.getElementsByTagName("user");
    for (int i = 0; i < users.getLength(); i++) {
      Element userElement = (Element) users.item(i);
      String existingUsername = userElement.getElementsByTagName("username").item(0).getTextContent();

      if (existingUsername.equals(username)) {
        String email = userElement.getElementsByTagName("email").item(0).getTextContent();
        return email;
      }
    }
    return null;
  }
}
