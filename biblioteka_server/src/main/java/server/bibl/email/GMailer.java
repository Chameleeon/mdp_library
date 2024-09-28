package server.bibl.email;

import server.bibl.App;
import java.util.logging.Level;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.io.*;
import java.util.Properties;
import java.util.Set;
import java.nio.file.Paths;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.model.Message;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.commons.codec.binary.Base64;

public class GMailer {
  public Message sendMail(String subject, String content, String fromEmailAddress, String toEmailAddress,
      String attachmentFilePath) {
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
      Gmail service = new Gmail.Builder(HTTP_TRANSPORT, jsonFactory, getCredentials(HTTP_TRANSPORT))
          .setApplicationName("E-Biblioteka")
          .build();
      Properties props = new Properties();
      Session session = Session.getDefaultInstance(props, null);

      MimeMessage email = createEmailWithAttachment(toEmailAddress, fromEmailAddress, subject, content,
          attachmentFilePath, session);

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      email.writeTo(buffer);
      byte[] rawMessageBytes = buffer.toByteArray();
      String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
      Message message = new Message();
      message.setRaw(encodedEmail);

      try {
        message = service.users().messages().send("me", message).execute();
        App.logger.info("Message id: " + message.getId());
        App.logger.info(message.toPrettyString());
        return message;
      } catch (GoogleJsonResponseException e) {
        GoogleJsonError error = e.getDetails();
        if (error.getCode() == 403) {
          System.err.println("Unable to send message: " + e.getDetails());
        } else {
          throw e;
        }
      }
    } catch (Exception e) {
      App.logger.log(Level.SEVERE, "An exception has occurred", e);
    }
    return null;
  }

  private MimeMessage createEmailWithAttachment(String toEmailAddress, String fromEmailAddress, String subject,
      String bodyText, String attachmentFilePath, Session session) throws MessagingException {
    MimeMessage email = new MimeMessage(session);

    email.setFrom(new InternetAddress(fromEmailAddress));
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(toEmailAddress));
    email.setSubject(subject);

    MimeBodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setText(bodyText);

    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(messageBodyPart);

    if (attachmentFilePath != null && !attachmentFilePath.isEmpty()) {
      MimeBodyPart attachmentBodyPart = new MimeBodyPart();
      DataSource source = new FileDataSource(attachmentFilePath);
      attachmentBodyPart.setDataHandler(new DataHandler(source));
      attachmentBodyPart.setFileName(new File(attachmentFilePath).getName());
      multipart.addBodyPart(attachmentBodyPart);
    }

    email.setContent(multipart);
    return email;
  }

  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
    InputStream in = GMailer.class.getResourceAsStream("/client_secret.json");
    if (in == null) {
      throw new FileNotFoundException("Resource not found: client_secret.json");
    }
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(),
        new InputStreamReader(in));

    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), clientSecrets, Set.of(GmailScopes.GMAIL_SEND))
        .setDataStoreFactory(new FileDataStoreFactory(Paths.get("tokens").toFile()))
        .setAccessType("offline")
        .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }
}
