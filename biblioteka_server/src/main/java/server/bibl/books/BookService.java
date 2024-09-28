package server.bibl.books;

import server.bibl.email.GMailer;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import server.bibl.users.UserActions;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Path("/books")
public class BookService {

  private RedisBookService redisBookService = new RedisBookService();
  private UserActions userService = new UserActions();

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addBook(BookWithAmount book) {
    redisBookService.addBook(book);
    return Response.status(Response.Status.CREATED).entity(book).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getBooks(@QueryParam("title") String title, @QueryParam("author") String author) {
    if (title != null && author != null) {

      BookWithAmount book = redisBookService.getBook(title, author);
      if (book != null) {
        return Response.ok(book).build();
      } else {
        return Response.status(Response.Status.NOT_FOUND)
            .entity("Book not found.")
            .build();
      }
    } else {

      List<BookWithAmount> books = redisBookService.getAllBooks();
      return Response.ok(books).build();
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteBook(@QueryParam("title") String title, @QueryParam("author") String author) {
    if (title == null || author == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Title and author query parameters are required.")
          .build();
    }

    boolean deleted = redisBookService.deleteBook(title, author);

    if (deleted) {
      return Response.status(Response.Status.NO_CONTENT).build();
    } else {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("Book not found.")
          .build();
    }
  }

  @POST
  @Path("/order")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response orderBooks(OrderRequest orderRequest) {
    List<Book> books = orderRequest.getBooks();
    List<BookWithContent> booksWithContent = orderRequest.getBooksWithContent();
    String username = orderRequest.getUsername();

    if ((books == null || books.isEmpty()) && (booksWithContent == null || booksWithContent.isEmpty())) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("No books or books with content provided.")
          .build();
    }

    if (username == null || username.isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("No username provided.")
          .build();
    }

    String email;
    try {
      email = userService.getUserEmail(username);
      if (email == null || email.isEmpty()) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity("Email not found for the provided username.")
            .build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error retrieving email: " + e.getMessage())
          .build();
    }

    Properties properties = new Properties();
    try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
      if (input == null) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("config.properties file not found.")
            .build();
      }
      properties.load(input);
    } catch (IOException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error loading config.properties: " + e.getMessage())
          .build();
    }

    String dobavljacHost = properties.getProperty("dobavljacServer.host");
    if (dobavljacHost == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("dobavljacServer not specified in config.properties.")
          .build();
    }

    int dobavljacPort = Integer.parseInt(properties.getProperty("dobavljacServer.port"));

    List<File> receivedBooks = new ArrayList<>();
    StringBuilder emailBodyBuilder = new StringBuilder();

    if (booksWithContent != null) {
      for (BookWithContent bookWithContent : booksWithContent) {
        String title = bookWithContent.getBook().getTitle();

        String sanitizedTitle = bookWithContent.getBook().getTitle().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        try {
          File tempBookFile = File.createTempFile(sanitizedTitle + "_", ".txt");
          try (FileWriter fileWriter = new FileWriter(tempBookFile)) {
            fileWriter.write(bookWithContent.getContent());
            receivedBooks.add(tempBookFile);

            emailBodyBuilder.append("Naslov: ").append(bookWithContent.getBook().getTitle())
                .append(", Autor: ").append(bookWithContent.getBook().getAuthor())
                .append(", Prevod: ").append(bookWithContent.getBook().getTranslators())
                .append(", Datum Izdanja: ").append(bookWithContent.getBook().getReleaseDate())
                .append(", Jezik: ").append(bookWithContent.getBook().getLanguage())
                .append("\n");

          }
        } catch (IOException e) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Error processing book with content '" + title + "': " + e.getMessage())
              .build();
        }
      }
    }

    if (books != null) {
      for (Book book : books) {
        String title = book.getTitle();
        BookWithContent bookWithContent = getBookWithContent(book);

        String sanitizedTitle = bookWithContent.getBook().getTitle().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        try {
          File tempBookFile = File.createTempFile(sanitizedTitle + "_", ".txt");
          try (FileWriter fileWriter = new FileWriter(tempBookFile)) {
            fileWriter.write(bookWithContent.getContent());

            receivedBooks.add(tempBookFile);

            emailBodyBuilder.append("Naslov: ").append(bookWithContent.getBook().getTitle())
                .append(", Autor: ").append(bookWithContent.getBook().getAuthor())
                .append(", Prevod: ").append(bookWithContent.getBook().getTranslators())
                .append(", Datum Izdanja: ").append(bookWithContent.getBook().getReleaseDate())
                .append(", Jezik: ").append(bookWithContent.getBook().getLanguage())
                .append("\n");

          }
        } catch (IOException e) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Error downloading book '" + title + "': " + e.getMessage())
              .build();
        }
      }
    }

    File zipFile;
    try {
      zipFile = File.createTempFile("books_", ".zip");
      try (FileOutputStream fos = new FileOutputStream(zipFile);
          ZipOutputStream zos = new ZipOutputStream(fos)) {

        for (File bookFile : receivedBooks) {
          ZipEntry zipEntry = new ZipEntry(bookFile.getName());
          zos.putNextEntry(zipEntry);

          try (FileInputStream fis = new FileInputStream(bookFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
              zos.write(buffer, 0, bytesRead);
            }
          }
          zos.closeEntry();
        }
      }
    } catch (IOException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error creating zip file: " + e.getMessage())
          .build();
    }

    try {
      GMailer gmailer = new GMailer();

      String subject = "Naručene knjige";
      String emailBody = emailBodyBuilder.toString();

      String fromEmail = properties.getProperty("gmail.address");
      if (fromEmail == null || fromEmail.isEmpty()) {
        fromEmail = "mdp.biblioteka@gmail.com";
      }

      gmailer.sendMail(subject, emailBody, fromEmail, email, zipFile.getAbsolutePath());

    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error sending email: " + e.getMessage())
          .build();
    } finally {
      zipFile.delete();
      for (File bookFile : receivedBooks) {
        bookFile.delete();
      }
    }

    return Response.ok("Order processed successfully.").build();
  }

  private String getFirst100LinesOfBook(Book book) {
    BookWithContent bookWithContent = getBookWithContent(book);

    if (bookWithContent == null || bookWithContent.getContent() == null) {
      return "Book content not found.";
    }

    String content = bookWithContent.getContent();

    String[] lines = content.split(System.lineSeparator());
    StringBuilder first100Lines = new StringBuilder();

    for (int i = 0; i < Math.min(100, lines.length); i++) {
      first100Lines.append(lines[i]).append(System.lineSeparator());
    }

    return first100Lines.toString();
  }

  @POST
  @Path("/first100lines")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public Response getFirst100Lines(Book book) {
    String first100Lines = getFirst100LinesOfBook(book);
    return Response.ok(first100Lines).build();
  }

  private BookWithContent getBookWithContent(Book book) {
    Properties properties = new Properties();
    try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
      if (input == null) {
        return null;
      }
      properties.load(input);
    } catch (IOException e) {
      return null;
    }

    String dobavljacHost = properties.getProperty("dobavljacServer.host");
    if (dobavljacHost == null) {
      return null;
    }

    int dobavljacPort = Integer.parseInt(properties.getProperty("dobavljacServer.port"));
    String title = book.getTitle();

    try (Socket socket = new Socket(dobavljacHost, dobavljacPort);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      writer.write("GET " + title + "\n");
      writer.flush();

      StringBuilder responseBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null && !line.equals("END_OF_BOOK")) {
        responseBuilder.append(line);
        responseBuilder.append(System.lineSeparator());
      }

      writer.write("END");
      writer.flush();
      String jsonResponse = responseBuilder.toString();

      Gson gson = new Gson();
      BookWithContent bookWithContent = gson.fromJson(jsonResponse, BookWithContent.class);

      return bookWithContent;
    } catch (IOException e) {
      return null;
    }

  }

  @POST
  @Path("/content")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addBookWithContent(BookWithContentAmount book) {
    redisBookService.addBookWithContent(book);
    return Response.status(Response.Status.CREATED).entity(book).build();
  }

  @GET
  @Path("/content")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getBookWithContent(@QueryParam("title") String title, @QueryParam("author") String author) {
    if (title == null || author == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Title and author query parameters are required.")
          .build();
    }

    BookWithContentAmount book = redisBookService.getBookWithContent(title, author);
    if (book != null) {
      return Response.ok(book).build();
    } else {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("Book with content not found.")
          .build();
    }
  }

  @PUT
  @Path("/content")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateBookWithContent(BookWithContentAmount book) {
    String title = book.getBook().getBook().getTitle();
    String author = book.getBook().getBook().getAuthor();
    BookWithContentAmount existingBook = redisBookService.getBookWithContent(title, author);
    if (existingBook == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("Book with content not found.")
          .build();
    }

    redisBookService.addBookWithContent(book);
    return Response.ok(book).build();
  }

  @DELETE
  @Path("/content")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteBookWithContent(@QueryParam("title") String title, @QueryParam("author") String author) {
    if (title == null || author == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Title and author query parameters are required.")
          .build();
    }

    boolean deleted = redisBookService.deleteBookWithContent(title, author);
    if (deleted) {
      return Response.status(Response.Status.NO_CONTENT).build();
    } else {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("Book with content not found.")
          .build();
    }
  }

  @GET
  @Path("/content/all")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllBooksWithContent() {
    List<BookWithContentAmount> books = redisBookService.getAllBooksWithContent();
    return Response.ok(books).build();
  }
}
