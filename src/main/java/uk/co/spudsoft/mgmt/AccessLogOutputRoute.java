/*
 * Copyright (C) 2023 jtalbut
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
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import static io.vertx.core.http.HttpVersion.HTTP_1_0;
import static io.vertx.core.http.HttpVersion.HTTP_1_1;
import static io.vertx.core.http.HttpVersion.HTTP_2;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Utils;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Vertx HTTP Server route for outputting HTTP requests captured by AccessLogCaptureRoute.
 *
 * @author jtalbut
 */
public class AccessLogOutputRoute implements Handler<RoutingContext> {

  /**
   * The path at which the standardDeploy method will put the router.
   */
  public static final String PATH = "accesslog";
  
  private static final String TYPE_JSON = "application/json";
  private static final String TYPE_HTML = "text/html";
  private static final String TYPE_PLAIN = "text/plain";
  
  private final RingBuffer<AccessLogCaptureRoute.AccessLogData> buffer;

  /**
   * Constructor.
   * @param buffer The buffer from the AccessLogCaptureRoute.
   */
  public AccessLogOutputRoute(RingBuffer<AccessLogCaptureRoute.AccessLogData> buffer) {
    this.buffer = buffer;
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
            .setName("Access Log")
            .produces(TYPE_JSON)
            .produces(TYPE_HTML)
            .produces(TYPE_PLAIN)
            ;
  }
  
  /**
   * Factory method to do standard deployment on newly constructed route.
   * 
   * The router passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param router The router that this handler will be attached to.
   * @param buffer The buffer from the AccessLogCaptureRoute.
   */
  public static void createAndDeploy(Router router, RingBuffer<AccessLogCaptureRoute.AccessLogData> buffer) {
    AccessLogOutputRoute route = new AccessLogOutputRoute(buffer);
    route.standardDeploy(router);
  }
  
  private static JsonObject toJson(AccessLogCaptureRoute.AccessLogData record) {
    JsonObject jo = new JsonObject();
    
    jo.put("timestamp", ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.getTimestamp()), ZoneOffset.UTC).toString());
    if (record.getTimestamp() > 0) {
      jo.put("endTimestamp", ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.getEndTimestamp()), ZoneOffset.UTC).toString());
    }
    HttpServerRequest request = record.getRequest();    
    jo.put("headers", request.headers());
    jo.put("url", request.absoluteURI());    
    jo.put("bytesRead", request.bytesRead());
    HttpServerResponse response = record.getResponse();
    if (response != null) {
      jo.put("responseHeaders", response.headers());
      jo.put("statusCode", response.getStatusCode());
      jo.put("bytesWritten", response.bytesWritten());
    }
    return jo;
  }
  
  @Override
  public void handle(RoutingContext rc) {
    
    HttpServerRequest request = rc.request();
    
    if (request.method() == HttpMethod.GET) {
      
      AccessLogCaptureRoute.AccessLogData data[] = buffer.toArray(i -> new AccessLogCaptureRoute.AccessLogData[i]);
      
      if (TYPE_JSON.equals(rc.getAcceptableContentType())) {
        
        JsonArray ja = new JsonArray();
        for (AccessLogCaptureRoute.AccessLogData record : data) {
          ja.add(toJson(record));
        }
        
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, TYPE_JSON);
        response.end(Json.encode(ja));
      } else if (TYPE_HTML.equals(rc.getAcceptableContentType())) {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, TYPE_HTML);
        response.setChunked(true);
        
        response.write("<html>");
        response.write("<head>");
        response.write("<style>table.top,th.top,td.top { border: 1px solid black; border-collapse: collapse; padding-left: 10px; padding-right: 10px; } td.number { text-align: right; }</style>");
        response.write("<script type=\"text/javascript\">\n    function flip(id) {\n      var el = document.getElementById(id);\n      if (el) {\n        if (el.style.display == 'none') {\n          el.style.display = '';\n        } else {\n          el.style.display = 'none';\n        }\n      }\n    }\n  </script>");
        response.write("</head>");
        response.write("<body>");
        
        response.write("<table style=\"border: 1px solid black; border-collapse: collapse;\" class=\"top\">");
        response.write("<thead><tr><th class=\"top\">Time</th><th class=\"top\">Method</th><th class=\"top\">URL</th><th class=\"top\">Status</th><th class=\"top\">Duration</th><th class=\"top\">Bytes Written</th></tr></thead>\n");
        
        response.write("<tbody>\n");
        
        int id = 0;
        for (AccessLogCaptureRoute.AccessLogData record : data) {
          ++id;
          response.write("<tr id=\"row-" + id + "\" onclick=\"flip('headers-" + id + "')\"><td class=\"top\">");
          response.write(ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.getTimestamp()), ZoneOffset.UTC).toString());
          response.write("</td><td class=\"top\">");
          response.write(record.getRequest().method().toString());
          response.write("</td><td class=\"top\">");
          response.write(record.getRequest().absoluteURI());
          response.write("</td><td class=\"top\">");
          if (record.getResponse() != null) {
            response.write(Integer.toString(record.getResponse().getStatusCode()));
            response.write("</td><td class=\"number top\">");
            response.write(Long.toString(record.getEndTimestamp() - record.getTimestamp()));
            response.write(" ms");
            response.write("</td><td class=\"number top\">");
            response.write(Long.toString(record.getResponse().bytesWritten()));
            response.write(" B");
          } else {
            response.write("</td><td class=\"top\">");
            response.write("</td><td class=\"top\">");
          }
          response.write("</td></tr>");
          
          response.write("<tr id=\"headers-" + id + "\" style=\"display: none;\"><td colspan=\"6\">");
          response.write("<table style=\"width: 100%;\">");
          response.write("<thead>");
          response.write("<tr>");
          response.write("<th style=\"width: 50%;\">Request Headers</th>");
          response.write("<th style=\"width: 50%;\">Response Headers</th>");
          response.write("</tr>");          
          response.write("</thead>");
          response.write("<tr>");

          response.write("<td style=\"width: 50%; vertical-align: top;\">");
          response.write("<table style=\"width: 100%;\">");          
          List<String> keys = new ArrayList<>(request.headers().names());
          keys.sort(String.CASE_INSENSITIVE_ORDER);          
          for (String key : keys) {
            response.write("<tr><td><pre>");
            response.write(key);
            response.write("</pre></td><td><pre>");
            List<String> values = request.headers().getAll(key);
            boolean first = true;
            for (String value : values) {
              if (!first) {
                response.write("\n");
              }
              first = false;
              response.write(value);
            }
            response.write("</pre></td></tr>");
          }
          response.write("</table></td>");

          response.write("<td style=\"width: 50%; vertical-align: top;\">");
          response.write("<table style=\"width: 100%;\">");
          if (record.getResponse() != null) {
            keys = new ArrayList<>(record.getResponse().headers().names());
            keys.sort(String.CASE_INSENSITIVE_ORDER);          
            for (String key : keys) {
              response.write("<tr><td><pre>");
              response.write(key);
              response.write("</pre></td><td><pre>");
              List<String> values = record.getResponse().headers().getAll(key);
              boolean first = true;
              for (String value : values) {
                if (!first) {
                  response.write("\n");
                }
                first = false;
                response.write(value);
              }
              response.write("</pre></td></tr>");
            }
          }
          response.write("</table></td></tr>");

          response.write("</table></td></tr>\n");
        }
        
        response.write("</tbody>");
        
        response.write("</table>");
        
        response.write("</body>");
        response.write("</html>");
        
        response.end();
      } else {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, TYPE_PLAIN);
        response.setChunked(true);
        
        for (AccessLogCaptureRoute.AccessLogData record : data) {
          response.write(buildStringLog(record.getRequest(), record.getResponse(), record.getTimestamp()));
          response.write("\n");
        }
        
        response.end();
      }
    } else {
      rc.next();
    }
  }
  
  private String getClientAddress(SocketAddress inetSocketAddress) {
    if (inetSocketAddress == null) {
      return null;
    }
    return inetSocketAddress.host();
  }
  
  private String buildStringLog(HttpServerRequest request, HttpServerResponse response, long timestamp) {
    
    String versionFormatted = getVersionFormatted(request);
    
    Integer status = null;
    Long contentLength = null;
    
    if (response != null) {
      status = response.getStatusCode();
      contentLength = response.bytesWritten();
    }
    
    MultiMap headers = request.headers();

    // as per RFC1945 the header is referer but it is not mandatory some implementations use referrer
    String referrer = headers.contains("referrer") ? headers.get("referrer") : headers.get("referer");
    String userAgent = request.headers().get("user-agent");
    referrer = referrer == null ? "-" : referrer;
    userAgent = userAgent == null ? "-" : userAgent;

    return String.format("%s - - [%s] \"%s %s %s\" %d %d \"%s\" \"%s\"",
      getClientAddress(request.remoteAddress()),
      Utils.formatRFC1123DateTime(timestamp),
      request.method(),
      request.absoluteURI(),
      versionFormatted,
      status,
      contentLength,
      referrer,
      userAgent);
  }

  String getVersionFormatted(HttpServerRequest request) {
    String versionFormatted;
    switch (request.version()){
      case HTTP_1_0:
        versionFormatted = "HTTP/1.0";
        break;
      case HTTP_1_1:
        versionFormatted = "HTTP/1.1";
        break;
      case HTTP_2:
        versionFormatted = "HTTP/2.0";
        break;
      default:
        versionFormatted = "-";
    }
    return versionFormatted;
  }


}
