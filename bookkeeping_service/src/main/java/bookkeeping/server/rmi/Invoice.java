package bookkeeping.server.rmi;

import java.util.HashMap;
import java.util.Date;
import java.util.Map;
import bookkeeping.server.books.BookWithAmount;
import java.io.Serializable;

public class Invoice implements Serializable {
  public Invoice() {

  }

  private Float total;

  private HashMap<BookWithAmount, Float> books;
  private Date date;

  public Float getTotal() {
    return total;
  }

  public void setTotal(Float total) {
    this.total = total;
  }

  public HashMap<BookWithAmount, Float> getBooks() {
    return books;
  }

  public void setBooks(HashMap<BookWithAmount, Float> books) {
    this.books = books;
  }

  public void addBook(BookWithAmount book, float price) {
    if (books.containsKey(book)) {
      for (Map.Entry<BookWithAmount, Float> entry : books.entrySet()) {
        BookWithAmount key = entry.getKey();
        Float value = entry.getValue();

        if (key.equals(book)) {
          key.setAmount(key.getAmount() + book.getAmount());
          books.put(key, value + price);
          return;
        }
      }
    } else {
      books.put(book, price);
    }
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }
}
