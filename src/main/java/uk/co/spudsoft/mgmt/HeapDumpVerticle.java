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

import com.sun.management.HotSpotDiagnosticMXBean;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class HeapDumpVerticle extends AbstractVerticle implements Handler<RoutingContext> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(HeapDumpVerticle.class);
  
  public HeapDumpVerticle(Router router) {
    router.route(HttpMethod.GET, "/heapdump").handler(this);
  }
  
  @Override
  public void handle(RoutingContext rc) {
  
    HttpServerResponse response = rc.response();

    String processName = getProcessName();
    String timestamp = LocalDateTime
            .now(Clock.systemUTC())
            .withNano(0)
            .toString()
            .replace(":", "-")
            ;
    
    String filename = processName + "-" + timestamp;
    
    File tempFile;
    try {
      tempFile = File.createTempFile(filename, ".hprof");
      tempFile.delete();
    } catch (IOException ex) {
      reportError("Failed to create temporary file: ", ex, response);
      return ;
    }
      
    try {
      HotSpotDiagnosticMXBean mxBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
      mxBean.dumpHeap(tempFile.getAbsolutePath(), false);
    } catch (Throwable ex) {
      reportError("Failed to generate heap dump: ", ex, response);
      return ;
    }
    
    response.putHeader(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
    response.putHeader("Content-Disposition", "attachment; filename=\"" + filename + ".hprof\"");

    response.sendFile(tempFile.getAbsolutePath(), (ar) -> {
      if (!tempFile.delete()) {
        logger.error("Failed to delete temporary files: {}", tempFile);
      } else {
        logger.debug("Deleted temporary file: {}", tempFile);
      }
    });
  }

  static void reportError(String message, Throwable ex, HttpServerResponse response) {
    logger.error(message, ex);
    response.setStatusCode(500);
    response.end("Failed to generate heap dump");
  }

  static String getProcessName() {
    return getProcessName(System.getProperty("sun.java.command"), () -> ProcessHandle.current());
  }
  
  static String getProcessName(String sunJavaCommand, Supplier<ProcessHandle> processHandleSupplier) {
    String processName = sunJavaCommand;
    int idx = processName == null ? 0 : processName.indexOf(" ");
    if (idx > 0) {
      processName = processName.substring(0, idx);
    }
    if (processName == null || processName.isEmpty()) {
      processName = processHandleSupplier.get().info().command().orElse("heap");
    }
    idx = processName.lastIndexOf("/");
    if (idx > 0) {
      processName = processName.substring(idx + 1);
    }
    idx = processName.lastIndexOf("\\");
    if (idx > 0) {
      processName = processName.substring(idx + 1);
    }
    idx = processName.lastIndexOf(".jar");
    if (idx > 0) {
      processName = processName.substring(0, idx);
    }
    return processName;
  }
}
