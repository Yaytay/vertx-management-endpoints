/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commentcap;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.co.spudsoft.params4j.SecretsSerializer;

/**
 *
 * @author jtalbut
 */
public class Credentials {
  
  /**
   * The username.
   */
  protected String username;
  /**
   * The password.
   */
  protected String password;

  /**
   * A property that can be set, but not got
   */
  protected String group;
  
  /**
   * Constructor.
   */
  public Credentials() {
  }

  /**
   * Constructor.
   * @param username The username to use, if any.
   * @param password The password to use, if any.
   */
  public Credentials(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (username == null || username.isEmpty()) {
      sb.append("username=").append(username);
    }
    sb.append("}");
    return sb.toString();
  }
  
  /**
   * The username.
   * @return The username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * The username.
   * @param username the username.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * The password.
   * @return the password.
   */
  @JsonSerialize(using = SecretsSerializer.class)
  public String getPassword() {
    return password;
  }

  /**
   * The password.
   * @param password the password.
   */
  @JsonSerialize(using = SecretsSerializer.class)
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * The group.
   * @param group the group.
   */
  public void setGroup(String group) {
    this.group = group;
  }
  
}
