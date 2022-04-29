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

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 * @author jtalbut
 */
public class ThreadDumpRouterTest {
  
  @Test
  public void testHandler() {
    RoutingContext rc = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(rc.response()).thenReturn(response);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(rc.request()).thenReturn(request);
    when(request.method()).thenReturn(HttpMethod.GET);
    ThreadDumpRouter handler = new ThreadDumpRouter();
    handler.handle(rc);
  }
  
  @Test
  public void testBuildStackTrace() {
    ThreadDumpRouter handler = new ThreadDumpRouter();
    String stackTrace = handler.buildStackTrace();
    assertThat(stackTrace, startsWith("main (RUNNABLE)"));
    assertThat(stackTrace, containsString("WAITING"));
    assertThat(stackTrace, containsString("Daemon"));
  }
  
}
