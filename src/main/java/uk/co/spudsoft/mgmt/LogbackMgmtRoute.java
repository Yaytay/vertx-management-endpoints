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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.ParsedHeaderValue;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Vertx HTTP Server route for allowing users to pull and update logback levels.
 *
 * It is strongly recommended that this endpoint be mounted on via a subrouter, the path to which is only accessible from authorised personnel.
 * The integration tests demonstrate the use of a suitable subrouter to locate the endpoint at /manage/logback.
 *
 * Two routes are created:
 * <ul>
 * <LI>GET /logback
 * Downloads a JSON representation of all the registered loggers.
 * If the request specifies an Accept header of "text/html" (before any specification of "application/json") downloads a single HTML page that provides the ability to change log levels via a UI.
 * <LI>PUT /logback/:logger
 * Requires a message body that contains a JSON object with a single element ("level") with a value that is (case insensitive) a valid Logback level.
 * The level of the specified logger is changed to the specified level.
 * </ul>
 * 
 * @author jtalbut
 */
public class LogbackMgmtRoute implements Handler<RoutingContext> {

  /**
   * The path at which the standardDeploy method will put the router.
   */
  public static final String PATH = "logback";

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(LogbackMgmtRoute.class);

  private static final ParsedHeaderValue HTML = new ParsableMIMEValue("text/html");
  private static final ParsedHeaderValue JSON = new ParsableMIMEValue("application/json");
  
  private String htmlContents;    

  /**
   * Constructor.
   */
  public LogbackMgmtRoute() {
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
            .setName("Logback Management")
            .produces(ContentTypes.TYPE_JSON)
            .produces(ContentTypes.TYPE_HTML)
            .produces(ContentTypes.TYPE_PLAIN)
            ;
    router.route(HttpMethod.PUT, "/" + PATH + "/:logger")
            .handler(this::handle)
            .setName("Logback Management")
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
    LogbackMgmtRoute route = new LogbackMgmtRoute();
    route.standardDeploy(router);
  }

  
  @Override
  public void handle(RoutingContext rc) {
    HttpServerRequest request = rc.request();
    if (request.method() == HttpMethod.GET) {
      
      ContentTypes.adjustFromParams(rc);
      
      if (ContentTypes.TYPE_JSON.equals(rc.getAcceptableContentType())) {
        getLogLevelsJson(rc.response());
      } else if (ContentTypes.TYPE_HTML.equals(rc.getAcceptableContentType())) {
        getHtml(request, rc.response());
      } else {
        getLogLevelsText(rc.response());
      }
    } else if (request.method() == HttpMethod.PUT) {
      updateLogLevel(rc.pathParam("logger"), request, rc.response());
    } else {
      rc.next();
    }
  }
  

  /**
   * Get the current log levels as a JsonObject.
   * 
   * This does the bulk of the work of verticle.
   * 
   * @return the current log levels as a JsonObject.
   */
  public static JsonObject getLogLevels() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    JsonObject json = new JsonObject();

    JsonObject appenders = new JsonObject();
    json.put("appenders", appenders);
    JsonObject loggers = new JsonObject();
    json.put("loggers", loggers);
    for (ch.qos.logback.classic.Logger curLogger : loggerContext.getLoggerList()) {
      Iterator<Appender<ILoggingEvent>> appenderIterator = curLogger.iteratorForAppenders();
      List<String> appendersForLogger = new ArrayList<>();
      while (appenderIterator.hasNext()) {
        Appender<ILoggingEvent> appender = appenderIterator.next();
        appendersForLogger.add(appender.getName());
        if (!appenders.containsKey(appender.getName())) {
          JsonObject jsonAppender = new JsonObject();
          jsonAppender.put("name", appender.getName());
          jsonAppender.put("type", appender.getClass().toString());
          jsonAppender.put("started", appender.isStarted());
          appenders.put(appender.getName(), jsonAppender);
        }
      }
      JsonObject jsonLogger = new JsonObject();
      jsonLogger.put("name", curLogger.getName());
      if (curLogger.getLevel() != null) {
        jsonLogger.put("level", curLogger.getLevel().levelStr);
      }
      if (curLogger.getEffectiveLevel() != null) {
        jsonLogger.put("effectiveLevel", curLogger.getEffectiveLevel().levelStr);
      }
      jsonLogger.put("additive", curLogger.isAdditive());
      jsonLogger.put("appenders", appendersForLogger);
      loggers.put(curLogger.getName(), jsonLogger);
    }
    
    return json;
  }

  /**
   * Set the level of the specified logger.
   * 
   * This will only do anything if the LoggerFactory returns a Logback LoggerContext that contains a Logger with the specified name.
   * This prevents a caller from creating new Loggers.
   * 
   * @param loggerName The name of the Logger to be adjusted.
   * @param levelName The desired target level (will default to Debug if not recognised as a valid level).
   */
  public static void setLogLevel(String loggerName, String levelName) {
    Object loggerFactory = LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger lg = null;
    if (loggerFactory instanceof LoggerContext) {
      LoggerContext lc = (LoggerContext) loggerFactory;
      lg = lc.exists(loggerName);
    }
    if (lg != null) {
      Level level = Level.toLevel(levelName);
      logger.info("Changing {} log level to {}", lg, level);
      lg.setLevel(level);
    } else {
      logger.info("Not changing the level of {} because it does not already exist", loggerName);
    }
  }
  
  private void getLogLevelsJson(HttpServerResponse response) {
    try {
      JsonObject json = getLogLevels();
      response.putHeader(HttpHeaderNames.CONTENT_TYPE, ContentTypes.TYPE_JSON);
      response.setStatusCode(200);
      response.end(json.toBuffer());
    } catch (Throwable ex) {
      logger.error("Failed to get logback configuration: ", ex);
      response.setStatusCode(500);
      response.end();
    }
  }
  
  private void getLogLevelsText(HttpServerResponse response) {
    try {
      JsonObject json = getLogLevels();
      
      response.putHeader(HttpHeaderNames.CONTENT_TYPE, ContentTypes.TYPE_PLAIN);
      response.setStatusCode(200);
      response.setChunked(true);

      response.write("appenders:");

      response.end();
    } catch (Throwable ex) {
      logger.error("Failed to get logback configuration: ", ex);
      response.setStatusCode(500);
      response.end();
    }
  }

  private void getHtml(HttpServerRequest request, HttpServerResponse response) {
    Future<String> loadFuture = Future.succeededFuture(htmlContents);
    if (loadFuture.result() == null) {
      loadFuture = Vertx.currentContext().executeBlocking(promise -> {
        try (InputStream stream = this.getClass().getResourceAsStream("/logback.html")) {
          String newHtml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
          this.htmlContents = newHtml;
          promise.complete(newHtml);
        } catch (Throwable ex) {
          logger.error("Failed to load HTML: ", ex);
          getLogLevelsJson(response);
        }
      });
    }
    loadFuture.map(html -> html.replaceAll("URL", "'" + request.absoluteURI() + "'"))
            .compose(html -> {
              response.putHeader(HttpHeaderNames.CONTENT_TYPE, "text/html");
              response.setStatusCode(200);
              response.end(html);
              return Future.succeededFuture();
            });
  }

  private void updateLogLevel(String loggerName, HttpServerRequest request, HttpServerResponse response) {
    request.body()
            .compose(buffer -> {
              JsonObject jo = buffer.toJsonObject();
              String newLevel = jo.getString("level");
              setLogLevel(loggerName, newLevel);
              getLogLevelsJson(response);
              return Future.succeededFuture();
            });
    
  }
  
}
