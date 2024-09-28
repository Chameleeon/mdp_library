package biblioteka.gui.users;

public enum UserStatus {
  PENDING("pending"),
  ACTIVATED("activated"),
  SUSPENDED("suspended");

  private String status;

  private UserStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return status;
  }
}
