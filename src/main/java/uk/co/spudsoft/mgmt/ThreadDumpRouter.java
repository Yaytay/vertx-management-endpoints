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

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 *
 * @author jtalbut
 */
public class ThreadDumpRouter implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext rc) {
    
    HttpServerRequest request = rc.request();
    
    if (request.method() == HttpMethod.GET) {

      String stackTraceString = buildStackTrace();

      HttpServerResponse response = rc.response();
      response.setStatusCode(200);
      response.putHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain");
      response.end(stackTraceString);
    } else {
      rc.next();
    }
  }

  String buildStackTrace() {
    ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] threadInfo = threadMxBean.dumpAllThreads(true, true);

    StringBuilder stackTraceString = new StringBuilder();
    for (ThreadInfo t : threadInfo) {
      StackTraceElement[] stackTrace = t.getStackTrace();
      stackTraceString.append(t.getThreadName())
              .append(" (").append(t.getThreadState()).append(")")
              .append(t.isDaemon() ? " Daemon" : "")
              .append("\n");
      if (t.isSuspended()) {
        stackTraceString.append("Suspended\n");
      }
      String lockName = t.getLockName();
      if (lockName != null) {
        stackTraceString
                .append("Waiting for ")
                .append(lockName)
                ;
        if (t.getLockOwnerId() >= 0) {
          stackTraceString
                  .append(" held by ")
                  .append(t.getLockOwnerId())
                  .append(" (")
                  .append(t.getLockOwnerName())
                  .append(")")
                  ;
        }
        stackTraceString
                .append("\n")
                ;
      }
      for (StackTraceElement s : stackTrace) {
        stackTraceString.append("  ").append(s.toString()).append("\n");
      }
      stackTraceString.append("\n");
    }
    return stackTraceString.toString();
  }

}
