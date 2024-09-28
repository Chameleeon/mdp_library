package dobavljac.server.books;

import java.util.Properties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;

import java.util.logging.Level;
import dobavljac.server.connection.Server;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class BookParser {
  private static BufferedReader downloadBook(String bookURL) {
    URL book;
    BufferedReader content = null;
    try {
      book = new URL(bookURL);
      content = new BufferedReader(new InputStreamReader(book.openStream()));
    } catch (IOException e) {
      Server.logger.log(Level.SEVERE, "An exception occurred", e);
    }
    return content;
  }

  public static String saveBook(String url) {
    File folder = new File("saved_books");
    if (!folder.exists()) {
      folder.mkdir();
    }

    String filePath = "";

    Book book = new Book();
    String regex = "(?m)^Title:\\s*(?<title>.+?)\\s*$" +
        "|^Author:\\s*(?<author>.+?)\\s*$" +
        "|^Translator:\\s*(?<translator>.+?)\\s*$" +
        "|^Release date:\\s*(?<releaseDate>.+?)\\s*(?:\\[.+?\\])?\\s*$" +
        "|^Language:\\s*(?<language>.+?)\\s*$" +
        "|^Credits:\\s*(?<credits>.+?)\\s*$";

    Pattern pattern = Pattern.compile(regex);
    try {
      BufferedReader in = downloadBook(url);
      String inputLine = "";

      while ((inputLine = in.readLine()) != null) {
        Matcher matcher = pattern.matcher(inputLine);
        if (matcher.find()) {
          if (matcher.group("title") != null) {
            book.setTitle(matcher.group("title"));
          } else if (matcher.group("author") != null) {
            book.setAuthor(matcher.group("author"));
          } else if (matcher.group("translator") != null) {
            book.setTranslators(matcher.group("translator"));
          } else if (matcher.group("releaseDate") != null) {
            book.setReleaseDate(matcher.group("releaseDate"));
          } else if (matcher.group("language") != null) {
            book.setLanguage(matcher.group("language"));
          } else if (matcher.group("credits") != null) {
            book.setCredits(matcher.group("credits"));
          }
        }

        if (inputLine.contains("*** START")) {
          break;
        }
      }
      int lastDotIndex = url.lastIndexOf(".");

      if (lastDotIndex != -1) {
        book.setCoverLink(url.substring(0, lastDotIndex) + ".cover.medium.jpg");
      }

      filePath = folder.getAbsolutePath() + File.separator + book.getTitle() + ".json";

      try (FileWriter writer = new FileWriter(filePath)) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(book, writer);
      }

      try (FileWriter writer = new FileWriter(filePath, true)) {
        while ((inputLine = in.readLine()) != null) {
          writer.write(inputLine + "\n");
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return filePath;
  }

  public static void saveAllBooks() {
    Properties properties = new Properties();
    try (InputStream input = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
      if (input == null) {
        Server.logger.severe("Sorry, unable to find config.properties");
        return;
      }
      properties.load(input);
    } catch (IOException e) {
      Server.logger.log(Level.SEVERE, "An exception occurred", e);
      return;
    }

    String urlPath = properties.getProperty("books.urls");
    File file = new File(urlPath);
    if (!file.exists()) {
      Server.logger.severe("URL path file does not exist: " + urlPath);
      return;
    }

    String hashFilePath = urlPath + ".hash";
    File hashFile = new File(hashFilePath);

    try {

      String currentHash = generateFileHash(file);

      if (!hashFile.exists()) {
        saveBooksFromFile(file);
        writeHashToFile(hashFile, currentHash);
      } else {

        String storedHash = readHashFromFile(hashFile);

        if (!currentHash.equals(storedHash)) {
          try {
            deleteDirectory(Paths.get("saved_books"));
          } catch (IOException e) {
            Server.logger.log(Level.SEVERE, "An exception occurred", e);
          }
          saveBooksFromFile(file);
          writeHashToFile(hashFile, currentHash);
        }
      }

    } catch (IOException | NoSuchAlgorithmException e) {
      Server.logger.log(Level.SEVERE, "An error occurred while processing the book URLs file", e);
    }
  }

  private static String generateFileHash(File file) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (InputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis)) {

      byte[] buffer = new byte[1024];
      int count;
      while ((count = bis.read(buffer)) != -1) {
        digest.update(buffer, 0, count);
      }
    }
    byte[] hashBytes = digest.digest();
    return bytesToHex(hashBytes);
  }

  private static void writeHashToFile(File hashFile, String hash) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(hashFile))) {
      writer.write(hash);
    }
  }

  private static String readHashFromFile(File hashFile) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(hashFile))) {
      return reader.readLine();
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : bytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1)
        hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }

  private static void saveBooksFromFile(File file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        saveBook(line);
      }
    }
  }

  private static void deleteDirectory(Path path) throws IOException {
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
