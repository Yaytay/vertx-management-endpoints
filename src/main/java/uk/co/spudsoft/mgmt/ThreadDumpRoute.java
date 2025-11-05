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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * A Vertx HTTP Server route for allowing users to download a thread dump of the process.
 *
 * It is strongly recommended that this endpoint be mounted on via a subrouter, the path to which is only accessible from authorised personnel.
 * The integration tests demonstrate the use of a suitable subrouter to locate the endpoint at /manage/threaddump.
 * 
 * @author jtalbut
 */
public class ThreadDumpRoute implements Handler<RoutingContext> {

  /**
   * The path at which the standardDeploy method will put the router.
   */
  public static final String PATH = "threads";
  
  /**
   * Constructor.
   */
  public ThreadDumpRoute() {
  }

  /**
   * Deploy the route to the router passed in at the normal endpoint.
   * 
   * The router passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param router The router that this handler will be attached to.
   */
  public void standardDeploy(Router router) {
    router.route(HttpMethod.GET, "/" + PATH)
            .handler(this::handle)
            .setName("Thread Dump")
            .produces(ContentTypes.TYPE_JSON)
            .produces(ContentTypes.TYPE_HTML)
            .produces(ContentTypes.TYPE_PLAIN)
            ;
  }
  
  /**
   * Factory method to do standard deployment on newly constructed route.
   * 
   * The router passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param router The router that this handler will be attached to.
   */
  public static void createAndDeploy(Router router) {
    ThreadDumpRoute route = new ThreadDumpRoute();
    route.standardDeploy(router);
  }
  
  private boolean opt(HttpServerRequest request, String paramName) {
    String param = request.getParam(paramName);
    if (param == null) {
      return false;
    } else if (param.isEmpty()) {
      return true;
    } else {
      return "true".equalsIgnoreCase(param);
    }
  }
  
  @Override
  public void handle(RoutingContext rc) {
    
    HttpServerRequest request = rc.request();
    
    if (request.method() == HttpMethod.GET) {
      
      ContentTypes.adjustFromParams(rc);

      if (ContentTypes.TYPE_JSON.equals(rc.getAcceptableContentType())) {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, ContentTypes.TYPE_JSON);
        response.end(buildStackTraceJson().toBuffer());
      } else if (ContentTypes.TYPE_HTML.equals(rc.getAcceptableContentType())) {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, ContentTypes.TYPE_HTML);
        response.end(buildStackTraceHtml());
      } else {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, ContentTypes.TYPE_PLAIN);
        response.end(buildStackTraceText(opt(request, "simple")));
      }
    } else {
      rc.next();
    }
  }

  static String buildStackTraceHtml() {
    ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] threadInfo = threadMxBean.dumpAllThreads(true, true);

    StringBuilder stackTraceString = new StringBuilder();
    
    stackTraceString.append("<html><head>");
    stackTraceString.append("<title>").append(HeapDumpRoute.getProcessName()).append(" @ ").append(ZonedDateTime.now(ZoneOffset.UTC).toString()).append("</title>");
    stackTraceString.append("</head><body>");
    stackTraceString.append("<table>");

    for (ThreadInfo t : threadInfo) {
      stackTraceString.append("<tr>");

      stackTraceString.append("<td><b>");
      stackTraceString.append(t.getThreadName());
      stackTraceString.append("</b></td>");
      stackTraceString.append("<td>");
      stackTraceString.append(t.getThreadId());
      stackTraceString.append("</td>");
      stackTraceString.append("<td>");
      stackTraceString.append(t.getThreadState());
      stackTraceString.append("</td>");
      stackTraceString.append("<td>");
      stackTraceString.append(t.isDaemon() ? "daemon" : "");
      stackTraceString.append("</td>");
      stackTraceString.append("<td>");
      stackTraceString.append(t.isSuspended() ? "suspended" : "");
      stackTraceString.append("</td>");
      stackTraceString.append("<td>");
      stackTraceString.append("</td>");

      stackTraceString.append("</tr>");
      
      String lockName = t.getLockName();
      if (lockName != null) {
        stackTraceString
                .append("<tr><td colspan=\"6\" style=\"padding-left: 20px;\">Waiting for ")
                .append(lockName);
        if (t.getLockOwnerId() >= 0) {
          stackTraceString
                  .append("<br/>Held by ")
                  .append(t.getLockOwnerId())
                  .append(" (")
                  .append(t.getLockOwnerName())
                  .append(")")
                  ;
        }
        stackTraceString.append("</td></tr>");
      }
      
      
      stackTraceString.append("<tr><td colspan=\"6\" style=\"padding-left: 40px;\"><pre>");
      for (StackTraceElement s : t.getStackTrace()) {
        stackTraceString.append(s.toString()).append("\n");
      }      
      stackTraceString.append("</pre></td></tr>\n");
    }
    stackTraceString.append("</table><body></html>");
    
    return stackTraceString.toString();
  }

  static JsonObject buildStackTraceJson() {
    ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] threadInfo = threadMxBean.dumpAllThreads(true, true);

    JsonObject result = new JsonObject();
    for (ThreadInfo t : threadInfo) {
      JsonObject thread = new JsonObject();
      result.put(t.getThreadName(), thread);
      thread.put("id", t.getThreadId());
      thread.put("daemon", t.isDaemon());
      thread.put("suspended", t.isSuspended());
      thread.put("state", t.getThreadState());
      addNonNullObjectToJson(thread, "lockInfo", t.getLockInfo());
      addNonNullObjectToJson(thread, "lockOwnerName", t.getLockOwnerName());
      long lockOwnerId = t.getLockOwnerId();      
      if (lockOwnerId > 0) {
        thread.put("lockOwnerId", lockOwnerId);
      }
      thread.put("blockedCount", t.getBlockedCount());
      thread.put("blockedTime", t.getBlockedTime());
      
      StackTraceElement[] stackTrace = t.getStackTrace();
      JsonArray jsonStack = new JsonArray();
      thread.put("stackTrace", jsonStack);
      for (StackTraceElement s : stackTrace) {
        JsonObject stackTraceElement = new JsonObject();
        addNonNullObjectToJson(stackTraceElement, "classLoaderName", s.getClassLoaderName());
        addNonNullObjectToJson(stackTraceElement, "className", s.getClassName());
        addNonNullObjectToJson(stackTraceElement, "fileName", s.getFileName());
        addNonNullObjectToJson(stackTraceElement, "lineNumber", s.getLineNumber());
        addNonNullObjectToJson(stackTraceElement, "methodName", s.getMethodName());
        addNonNullObjectToJson(stackTraceElement, "moduleName", s.getModuleName());
        addNonNullObjectToJson(stackTraceElement, "moduleVersion", s.getModuleVersion());
        jsonStack.add(stackTraceElement);
      }
    }
    return result;
  }

  static void addNonNullObjectToJson(JsonObject parent, String name, Object value) {
    if (name != null) {
      parent.put(name, value);
    }
  }

  static String buildStackTraceText(boolean simple) {
    ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] threadInfo = threadMxBean.dumpAllThreads(true, true);

    StringBuilder stackTraceString = new StringBuilder();
    for (ThreadInfo t : threadInfo) {
      StackTraceElement[] stackTrace = t.getStackTrace();
      if (simple) {
        stackTraceString.append(t.getThreadName())
                .append("\t(").append(t.getThreadState()).append(")")
                .append(t.isDaemon() ? " Daemon" : " User")
                ;
        if (stackTrace.length > 0) {
          stackTraceString.append("\t").append(stackTrace[0].toString());
        }
        if (stackTrace.length > 1) {
          stackTraceString.append("\t").append(stackTrace[1].toString());
        }
      } else {
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
      }
      stackTraceString.append("\n");
    }
    return stackTraceString.toString();
  }
  
}
