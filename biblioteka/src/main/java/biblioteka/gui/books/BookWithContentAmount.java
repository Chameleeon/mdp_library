package biblioteka.gui.books;

public class BookWithContentAmount {
  private BookWithAmount book;
  private String content;

  public BookWithContentAmount() {

  }

  public BookWithContentAmount(BookWithAmount book, String content) {
    this.book = book;
    this.content = content;
  }

  public BookWithAmount getBook() {
    return book;
  }

  public String getContent() {
    return content;
  }

  public void setBook(BookWithAmount book) {
    this.book = book;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
