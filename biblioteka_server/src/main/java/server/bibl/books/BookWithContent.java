package server.bibl.books;

public class BookWithContent {
  private Book book;
  private String content;

  public BookWithContent() {

  }

  public BookWithContent(Book book, String content) {
    this.book = book;
    this.content = content;
  }

  public Book getBook() {
    return book;
  }

  public String getContent() {
    return content;
  }

  public void setBook(Book book) {
    this.book = book;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
