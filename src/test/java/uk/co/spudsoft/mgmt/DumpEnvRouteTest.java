/*
 * Copyright (C) 2023 njt
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class DumpEnvRouteTest {
  
  private static final Logger logger = LoggerFactory.getLogger(DumpEnvRouteTest.class);
  
  /**
   * Test of getVariables method, of class DumpEnvRoute.
   */
  @Test
  public void testGetVariables() {
    DumpEnvRoute.getVariables().forEach((v) -> {
      logger.info("Variable: {} = {}", v.getName(), v.getValue());
    });
    assertThat(DumpEnvRoute.getVariables().size(), greaterThan(1));
  }
  
}
