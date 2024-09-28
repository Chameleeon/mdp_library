package server.bibl.users;

public class User {
  private String firstName;
  private String lastName;
  private Address address;
  private String username;
  private String passwordHash;
  private String email;
  private UserStatus status;

  public User() {
  }

  public User(String firstName, String lastName, Address address, String username, String passwordHash, String email,
      UserStatus status) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.address = address;
    this.username = username;
    this.passwordHash = passwordHash;
    this.email = email;
    this.status = status;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return this.passwordHash;
  }

  public void setPasswordHash(String passwd) {
    this.passwordHash = passwd;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public UserStatus getStatus() {
    return status;
  }
}
