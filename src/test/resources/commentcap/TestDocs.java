package commentcap;

import java.io.File;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.params4j.ConfigurationProperty;
import uk.co.spudsoft.params4j.Params4J;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/*
 * Copyright (C) 2022 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 *
 * @author jtalbut
 */
public class TestDocs {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TestDocs.class);
  
  private final List<ConfigurationProperty> docs;
  
  public TestDocs() {
    Params4J<Parameters> params4j = Params4J.<Parameters>factory().withConstructor(() -> new Parameters()).create();
    docs = params4j.getDocumentation(new Parameters(), "--", null, Arrays.asList(Pattern.compile(".*\\.Html.*")));
    
    int maxNameLen = docs.stream().map(p -> p.name.length()).max(Integer::compare).get();
    
    StringBuilder usageBuilder = new StringBuilder();
    for (ConfigurationProperty prop : docs) {
      usageBuilder.append("    ")
              .append(prop.name)
              .append(" ".repeat(maxNameLen + 1 - prop.name.length()))
              .append(prop.comment)
              .append('\n');

      String typeName = prop.type.getSimpleName();
      usageBuilder.append("        ")
              .append(typeName);
      
      if (prop.defaultValue != null) {
        usageBuilder.append(" ".repeat(typeName.length() + 4 > maxNameLen ? 1 : maxNameLen - typeName.length() - 3))
                .append("default: ")
                .append(prop.defaultValue);
      }
      usageBuilder.append('\n');
    }
    logger.debug("Usage:\n{}", usageBuilder);
  }

  public List<ConfigurationProperty> getDocs() {
    return docs;
  }
  
  public void testProcessArgs() {
    String[] args = {
      "--names[0]=Name1"
      , "--names[1]=Name2"
      , "--auditDataSource.schema=AuditSchema"
      , "--auditDataSource.user.username=AuditNormalUser"
      , "--auditDataSource.user.password=AuditNormalPass"
      , "--auditDataSource.adminUser.username=AuditAdminUser"
      , "--auditDataSource.adminUser.password=AuditAdminPass"
      , "--auditDataSource.url=AuditDbUrl"
      , "--logins.System1.username=System1User"
      , "--logins.System1.password=System1Pass"
      , "--logins.System2.username=System2User"
      , "--logins.System2.password=System2Pass"
      , "--baseConfigPath=target/dir"
      , "--documentedAlien.href=https://www.java.com/"
      , "--documentedAlien.textContent=Java"
      , "--alien.href=https://dev.java/"
      , "--alien.textContent=Dev Java"
      , "--when=1981-07-06T14:00"
      , "--translations.Big=Huge"
      , "--translations.Small=Tiny"
    };
    
    Params4J<Parameters> params4j = Params4J.<Parameters>factory()
            .withCommandLineArgumentsGatherer(args, "--")
            .withConstructor(() -> new Parameters())
            .create();
    Parameters params = params4j.gatherParameters();
    
    assertNotNull(params);
    assertThat(params.getNames(), hasSize(2));
    assertEquals("Name1", params.getNames().get(0));
    assertEquals("Name2", params.getNames().get(1));
    assertEquals("AuditSchema", params.getAuditDataSource().getSchema());
    assertEquals("AuditNormalUser", params.getAuditDataSource().getUser().getUsername());
    assertEquals("AuditNormalPass", params.getAuditDataSource().getUser().getPassword());
    assertEquals("AuditAdminUser", params.getAuditDataSource().getAdminUser().getUsername());
    assertEquals("AuditAdminPass", params.getAuditDataSource().getAdminUser().getPassword());
    assertEquals("AuditDbUrl", params.getAuditDataSource().getUrl());
    assertNotNull(params.getLogins());
    assertEquals(2, params.getLogins().size());
    assertEquals("System1User", params.getLogins().get("System1").getUsername());
    assertEquals("System1Pass", params.getLogins().get("System1").getPassword());
    assertEquals("System2User", params.getLogins().get("System2").getUsername());
    assertEquals("System2Pass", params.getLogins().get("System2").getPassword());
    assertEquals(new File("target/dir"), params.getBaseConfigPath());
    assertEquals("https://dev.java/", params.getAlien().getHref());
    assertEquals("Dev Java", params.getAlien().getTextContent());
    assertEquals("https://www.java.com/", params.getDocumentedAlien().getHref());
    assertEquals("Java", params.getDocumentedAlien().getTextContent());
    assertEquals(LocalDateTime.of(1981, Month.JULY, 6, 14, 0), params.getWhen());
    assertNotNull(params.getTranslations());
    assertEquals(2, params.getTranslations().size());
    assertEquals("Huge", params.getTranslations().get("Big"));
    assertEquals("Tiny", params.getTranslations().get("Small"));
  }
}
