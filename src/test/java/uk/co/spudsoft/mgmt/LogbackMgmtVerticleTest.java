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
package uk.co.spudsoft.mgmt;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author jtalbut
 */
public class LogbackMgmtVerticleTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(LogbackMgmtVerticleTest.class);
  
  @Test
  public void testGetLogLevels() {
    JsonObject json = LogbackMgmtVerticle.getLogLevels();
    assertNotNull(json);
    logger.debug("Loggers: {}", json);
    assertThat(json.getJsonObject("appenders").size(), equalTo(1));
    assertThat(json.getJsonObject("loggers").size(), greaterThan(4));    
    assertThat(json.getJsonObject("loggers").getJsonObject(getClass().getCanonicalName()).getString("effectiveLevel"), equalTo("TRACE"));
    
    LogbackMgmtVerticle.setLogLevel(getClass().getCanonicalName(), "DEBUG");
    json = LogbackMgmtVerticle.getLogLevels();
    assertThat(json.getJsonObject("loggers").getJsonObject(getClass().getCanonicalName()).getString("effectiveLevel"), equalTo("DEBUG"));
  }
  
}
