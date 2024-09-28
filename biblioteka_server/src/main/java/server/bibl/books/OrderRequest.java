package server.bibl.books;

import java.util.List;

public class OrderRequest {
  private List<Book> books;
  private List<BookWithContent> booksWithContent;
  private String username;

  public List<Book> getBooks() {
    return books;
  }

  public void setBooks(List<Book> books) {
    this.books = books;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public List<BookWithContent> getBooksWithContent() {
    return booksWithContent;
  }

  public void setBooksWithContent(List<BookWithContent> booksWithContents) {
    this.booksWithContent = booksWithContents;
  }
}
