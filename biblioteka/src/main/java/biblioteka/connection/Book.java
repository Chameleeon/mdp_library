package biblioteka.connection;

import java.util.Date;
import java.util.Objects;

public class Book {
  private String title;
  private String author;
  private String translators;
  private String releaseDate;
  private String language;
  private String credits;
  private String coverLink;

  public Book() {
  }

  public Book(String title, String author, String translators, String releaseDate, String language, String credits,
      String coverLink) {
    this.title = title;
    this.author = author;
    this.translators = translators;
    this.releaseDate = releaseDate;
    this.language = language;
    this.credits = credits;
    this.coverLink = coverLink;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getTranslators() {
    return translators;
  }

  public void setTranslators(String translators) {
    this.translators = translators;
  }

  public String getReleaseDate() {
    return releaseDate;
  }

  public void setReleaseDate(String releaseDate) {
    this.releaseDate = releaseDate;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getCredits() {
    return credits;
  }

  public void setCredits(String credits) {
    this.credits = credits;
  }

  public String getCoverLink() {
    return coverLink;
  }

  public void setCoverLink(String coverLink) {
    this.coverLink = coverLink;
  }

  @Override
  public String toString() {
    return "Title: " + title + "\nAuthor: " + author + "\nTranslators: " + translators + "\nRelease date: "
        + releaseDate + "\nLanguage: " + language + "\nCredits: " + credits;
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, author);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    Book book = (Book) obj;
    return Objects.equals(title, book.title) && Objects.equals(author, book.author);
  }
}
