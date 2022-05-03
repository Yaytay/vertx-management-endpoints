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
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.ParsedHeaderValue;
import io.vertx.ext.web.ParsedHeaderValues;
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
 *
 * @author jtalbut
 */
public class LogbackMgmtVerticle extends AbstractVerticle implements Handler<RoutingContext> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(LogbackMgmtVerticle.class);

  private static final ParsedHeaderValue HTML = new ParsableMIMEValue("text/html");
  private static final ParsedHeaderValue JSON = new ParsableMIMEValue("application/json");
  
  private String htmlContents;

  public LogbackMgmtVerticle(Router router) {
    router.route(HttpMethod.GET, "/logback").handler(this);
    router.route(HttpMethod.PUT, "/logback/:logger").handler(this);
  }
    
  @Override
  public void handle(RoutingContext event) {
    HttpServerRequest request = event.request();
    if (request.method() == HttpMethod.GET) {
      if (wantsHtml(event)) {
        getHtml(request, event.response());
      } else {
        getLogLevelsJson(event.response());
      }
    } else if (request.method() == HttpMethod.PUT) {
      updateLogLevel(event.pathParam("logger"), request, event.response());
    } else {
      event.next();
    }
  }
  
  static boolean wantsHtml(RoutingContext event) {
    try {
      ParsedHeaderValues phv = event.parsedHeaders();
      List<MIMEHeader> types = phv.accept();
      for (MIMEHeader type : types) {
        if (type.isMatchedBy(JSON)) {
          return false;
        } else if (type.isMatchedBy(HTML)) {
          return true;
        }
      }    
      return false;
    } catch (Throwable ex) {
      logger.debug("Failed to determine whether request wants HTML: ", ex);
      return false;
    }
  }

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

  public static void setLogLevel(String loggerName, String levelName) {
    Object loggerFactory = LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger lg;
    if (loggerFactory instanceof LoggerContext lc) {
      lg = lc.exists(loggerName);
    } else {
      lg = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);
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
      response.putHeader(HttpHeaderNames.CONTENT_TYPE, "application/json");
      response.setStatusCode(200);
      response.end(json.toBuffer());
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
