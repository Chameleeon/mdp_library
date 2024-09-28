package dobavljac.server.books;

public class BookWithContent {
  private Book book;
  private String content;

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
}
