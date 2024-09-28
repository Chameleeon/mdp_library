package client.books;

public class BookWithAmount {
  private Book book;
  private int amount;

  public BookWithAmount() {
  }

  public BookWithAmount(Book book, int amount) {
    this.book = book;
    this.amount = amount;
  }

  public Book getBook() {
    return book;
  }

  public int getAmount() {
    return amount;
  }

  public void setBook(Book book) {
    this.book = book;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }
}
