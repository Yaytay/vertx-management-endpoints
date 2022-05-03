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

import io.vertx.core.http.HttpServerResponse;
import java.lang.ProcessHandle.Info;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author jtalbut
 */
public class HeapDumpVerticleTest {
  
  @Test
  public void testReportError() {
    HttpServerResponse response = mock(HttpServerResponse.class);
    HeapDumpVerticle.reportError("message", new IllegalArgumentException("ExceptionMessage"), response);
    verify(response).setStatusCode(500);
    verify(response).end("Failed to generate heap dump");
  }
  
  @Test
  public void testGetProcessName() {
    String processName = HeapDumpVerticle.getProcessName();
    assertThat(processName, startsWith("surefirebooter"));
    
    assertEquals("bob", HeapDumpVerticle.getProcessName("bob fred", () -> null));
    assertEquals("bob", HeapDumpVerticle.getProcessName("bob", () -> null));
    ProcessHandle ph = mock(ProcessHandle.class);
    Info i = mock(Info.class);
    when(ph.info()).thenReturn(i);
    
    Optional<String> command = Optional.empty();
    when(i.command()).thenReturn(command);
    assertEquals("heap", HeapDumpVerticle.getProcessName(null, () -> ph));
    
    command = Optional.of("carol");
    when(i.command()).thenReturn(command);
    assertEquals("carol", HeapDumpVerticle.getProcessName("", () -> ph));
    
    assertEquals("ted", HeapDumpVerticle.getProcessName("bob/carol/ted", () -> null));
    assertEquals("ted", HeapDumpVerticle.getProcessName("bob\\carol\\ted", () -> null));
    assertEquals("ted", HeapDumpVerticle.getProcessName("bob\\carol\\ted.jar", () -> null));
    
  }
  
}
