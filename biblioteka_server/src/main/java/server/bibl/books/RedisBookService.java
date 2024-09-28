package server.bibl.books;

import server.bibl.App;
import java.util.logging.Level;
import redis.clients.jedis.Jedis;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class RedisBookService {

  private Jedis jedisAmountDb;
  private Jedis jedisContentDb;
  private Gson gson;

  public RedisBookService() {

    Properties properties = loadProperties();

    String redisUrl = properties.getProperty("jedis.url", "localhost");
    int redisPort = Integer.parseInt(properties.getProperty("jedis.port", "6379"));
    int amountDbIndex = Integer.parseInt(properties.getProperty("jedis.amountDbIndex", "0"));
    int contentDbIndex = Integer.parseInt(properties.getProperty("jedis.contentDbIndex", "1"));

    this.jedisAmountDb = new Jedis(redisUrl, redisPort);
    this.jedisAmountDb.select(amountDbIndex);

    this.jedisContentDb = new Jedis(redisUrl, redisPort);
    this.jedisContentDb.select(contentDbIndex);

    this.gson = new Gson();
  }

  private Properties loadProperties() {
    Properties properties = new Properties();
    try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
      if (input == null) {
        App.logger.severe("Unable to find config.properties");
        return properties;
      }
      properties.load(input);
    } catch (IOException ex) {
      App.logger.log(Level.SEVERE, "An exception occurred", ex);
    }
    return properties;
  }

  public void addBook(BookWithAmount book) {
    String bookJson = gson.toJson(book);
    jedisAmountDb.set(generateKey(book), bookJson);
  }

  public BookWithAmount getBook(String title, String author) {
    String key = title + "-" + author;
    String bookJson = jedisAmountDb.get(key);
    if (bookJson == null) {
      return null;
    }
    return gson.fromJson(bookJson, BookWithAmount.class);
  }

  public boolean deleteBook(String title, String author) {
    String key = title + "-" + author;
    long result = jedisAmountDb.del(key);
    return result > 0;
  }

  public List<BookWithAmount> getAllBooks() {
    List<BookWithAmount> books = new ArrayList<>();
    Set<String> keys = jedisAmountDb.keys("*-*");

    for (String key : keys) {
      String bookJson = jedisAmountDb.get(key);
      BookWithAmount book = gson.fromJson(bookJson, BookWithAmount.class);
      books.add(book);
    }
    return books;
  }

  public void addBookWithContent(BookWithContentAmount book) {
    String bookJson = gson.toJson(book);
    jedisContentDb.set(generateContentKey(book), bookJson);
  }

  public BookWithContentAmount getBookWithContent(String title, String author) {
    String key = title + "-" + author;
    String bookJson = jedisContentDb.get(key);
    if (bookJson == null) {
      return null;
    }
    return gson.fromJson(bookJson, BookWithContentAmount.class);
  }

  public boolean deleteBookWithContent(String title, String author) {
    String key = title + "-" + author;
    long result = jedisContentDb.del(key);
    return result > 0;
  }

  public List<BookWithContentAmount> getAllBooksWithContent() {
    List<BookWithContentAmount> books = new ArrayList<>();
    Set<String> keys = jedisContentDb.keys("*-*");

    for (String key : keys) {
      String bookJson = jedisContentDb.get(key);
      BookWithContentAmount book = gson.fromJson(bookJson, BookWithContentAmount.class);
      books.add(book);
    }
    return books;
  }

  public void close() {
    jedisAmountDb.close();
    jedisContentDb.close();
  }

  private String generateKey(BookWithAmount book) {
    return book.getBook().getTitle() + "-" + book.getBook().getAuthor();
  }

  private String generateContentKey(BookWithContentAmount book) {
    return book.getBook().getBook().getTitle() + "-" + book.getBook().getBook().getAuthor();
  }
}
