/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commentcap;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.params4j.Comment;
import uk.co.spudsoft.params4j.JavadocCapture;
import uk.co.spudsoft.params4j.impl.HtmlAnchorElement;

/**
 * Test and demonstration class for the capabilities of Params4J documentation.
 * 
 * In case it is not obvious the documentation on the members of this class are total nonsense.
 * 
 * @author jtalbut
 */
@JavadocCapture
public class Parameters {

  // Demonstrate an alien member that cannot be set using a single string value
  // and that we do not want to document all the internal members of.
  private HtmlAnchorElement alien;
  // Demonstrate an alien member that has a specific custom comment.
  private HtmlAnchorElement documentedAlien;
  private boolean exitOnRun;
  // File has setReadable(boolean) which should not be documented
  private File baseConfigPath;
  // DataSource has nested setters, which should be documented.
  private DataSource auditDataSource = DataSource.builder().build();
  // LocalDateTime is terminal, but should have a specific default value
  private LocalDateTime when = LocalDateTime.of(1971, 06, 05, 14, 0);
  // A key/value pair parameter
  private Map<String, String> translations;
  /**
   * The login for a system.
   */
  private Map<String, Credentials> logins;
  // An array parameter
  private List<String> names;

  /**
   * alien value that cannot be documented further in this codebase
   * @return an alien value that cannot be documented further in this codebase
   */
  public HtmlAnchorElement getAlien() {
    return alien;
  }

  /**
   * alien value that cannot be documented further in this codebase
   * @param alien alien value that cannot be documented further in this codebase
   */
  public void setAlien(HtmlAnchorElement alien) {
    this.alien = alien;
  }

  /**
   * alien value that cannot be documented further in this codebase
   * @return an alien value that cannot be documented further in this codebase
   */
  public HtmlAnchorElement getDocumentedAlien() {
    return documentedAlien;
  }

  /**
   * alien value that cannot be documented further in this codebase
   * Note that the URL in the comment is wrong, can't use an actual HTMLAnchorElement because they are interfaces.
   * @param documentedAlien alien value that cannot be documented further in this codebase
   */
  @Comment("configure the alien properties as documented at https://docs.oracle.com/en/java/javase/17/docs/api/jdk.xml.dom/org/w3c/dom/html/HTMLAnchorElement.html")
  public void setDocumentedAlien(HtmlAnchorElement documentedAlien) {
    this.documentedAlien = documentedAlien;
  }
  
  /**
   * if true the process will end rather than waiting for requests
   * This is expected to be useful for things such as JIT compilers or CDS preparation.
   * @return the exitOnRun value.
   */
  public boolean isExitOnRun() {
    return exitOnRun;
  }

  /**
   * if true the process will end rather than waiting for requests
   * This is expected to be useful for things such as JIT compilers or CDS preparation.
   * @param exitOnRun the exitOnRun value.
   */
  public void setExitOnRun(boolean exitOnRun) {
    this.exitOnRun = exitOnRun;
  }

  /**
   * The path to the root of the configuration files.
   * @return the path to the root of the configuration files.
   */
  public File getBaseConfigPath() {
    return baseConfigPath;
  }

  /**
   * The path to the root of the configuration files.
   * @param baseConfigPath the path to the root of the configuration files.
   */
  public void setBaseConfigPath(File baseConfigPath) {
    this.baseConfigPath = baseConfigPath;
  }

  /**
   * The datasource used for recording activity.
   * @return The datasource used for recording activity.
   */
  public DataSource getAuditDataSource() {
    return auditDataSource;
  }

  /**
   * The datasource used for recording activity.
   * @param auditDataSource The datasource used for recording activity.
   */
  public void setAuditDataSource(DataSource auditDataSource) {
    this.auditDataSource = auditDataSource;
  }

  /**
   * when it happened.
   * @return when it happened.
   */
  public LocalDateTime getWhen() {
    return when;
  }

  /**
   * when it happened.
   * @param when when it happened.
   */
  public void setWhen(LocalDateTime when) {
    this.when = when;
  }

  public Map<String, String> getTranslations() {
    return translations;
  }

  /**
   * translations from one word to another
   * @param translations 
   */
  public void setTranslations(Map<String, String> translations) {
    this.translations = translations;
  }

  public List<String> getNames() {
    return names;
  }

  /**
   * names that are recognised by the process
   * @param names 
   */
  public void setNames(List<String> names) {
    this.names = names;
  }

  public Map<String, Credentials> getLogins() {
    return logins;
  }

  public void setLogins(Map<String, Credentials> logins) {
    this.logins = logins;
  }

}
