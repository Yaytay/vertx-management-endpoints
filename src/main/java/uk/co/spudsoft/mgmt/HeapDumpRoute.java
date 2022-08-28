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
 * A Vertx HTTP Server route for allowing users to download a heap dump over HTTP.
 * 
 * It is vital, for obvious security reasons, that this endpoint is not accessible to end users.
 * It is strongly recommended that this endpoint be mounted on via a subrouter, the path to which is only accessible from authorised personnel.
 * The integration tests demonstrate the use of a suitable subrouter to locate the endpoint at /manage/heapdump.
 * 
 * The heapdump generated is written to disk and then streamed from there to the client.
 * The resulting hprof file is likely to be large (the integration test routinely generates a 40MB file), avoid downloading it using any client that will try to store it in memory.
 * 
 * @author jtalbut
 */
public class HeapDumpRoute implements Handler<RoutingContext> {

  private static final Logger logger = LoggerFactory.getLogger(HeapDumpRoute.class);

  /**
   * Constructor.
   */
  public HeapDumpRoute() {
  }  
  
  /**
   * Deploy the route to the router passed in at the normal endpoint.
   * 
   * The router passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param router The router that this handler will be attached to.
   */
  public void standardDeploy(Router router) {
    router.route(HttpMethod.GET, "/heapdump").handler(this::handle);
  }
  
  /**
   * Factory method to do standard deployment on newly constructed route.
   * 
   * The router passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param router The router that this handler will be attached to.
   */
  public static void createAndDeploy(Router router) {
    HeapDumpRoute route = new HeapDumpRoute();
    route.standardDeploy(router);
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
