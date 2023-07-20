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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Vertx HTTP Server route for allowing users to download a thread dump of the process.
 *
 * It is strongly recommended that this endpoint be mounted on via a subrouter, the path to which is only accessible from authorised personnel.
 * The integration tests demonstrate the use of a suitable subrouter to locate the endpoint at /manage/threaddump.
 * 
 * @author jtalbut
 */
public class DumpSysPropsRoute implements Handler<RoutingContext> {

  private static final String TYPE_JSON = "application/json";
  private static final String TYPE_HTML = "text/html";
  private static final String TYPE_PLAIN = "text/plain";
  
  /**
   * Constructor.
   */
  public DumpSysPropsRoute() {
  }

  /**
   * Deploy the route to the router passed in at the normal endpoint.
   * 
   * The router passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param router The router that this handler will be attached to.
   */
  public void standardDeploy(Router router) {
    router.route(HttpMethod.GET, "/dumpsysprops")
            .handler(this::handle)
            .setName("System Properties")
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
   */
  public static void createAndDeploy(Router router) {
    DumpSysPropsRoute route = new DumpSysPropsRoute();
    route.standardDeploy(router);
  }
  
  static class Property {
    
    private final String name;
    private final String value;

    Property(String name, String value) {
      this.name = name;
      this.value = value;
    }

    @JsonProperty(value = "name")
    String getName() {
      return name;
    }

    @JsonProperty(value = "value")
    String getValue() {
      return value;
    }
  }
  
  
  @Override
  public void handle(RoutingContext rc) {
    
    HttpServerRequest request = rc.request();
    
    if (request.method() == HttpMethod.GET) {
      
      List<Property> properties = getProperties();
      
      if (TYPE_JSON.equals(rc.getAcceptableContentType())) {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, TYPE_JSON);
        response.end(Json.encode(properties));
      } else if (TYPE_HTML.equals(rc.getAcceptableContentType())) {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, TYPE_HTML);
        response.setChunked(true);
        
        response.write("<html>");
        response.write("<head>");
        response.write("</head>");
        response.write("<body>");
        
        response.write("<table>");
        
        response.write("<thead>");        
        response.write("<tr>");
        response.write("<th>Name</th><th>Value</th>");
        response.write("</tr>");
        response.write("</thead>");
        
        response.write("<tbody>");
        
        for (Property prop : properties) {
          response.write("<tr><td>");
          response.write(prop.name);
          response.write("</td><td>");
          response.write(prop.value);
          response.write("</td>");
          response.write("</tr>");
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
        
        for (Property prop : properties) {
          response.write(prop.name);
          response.write(": ");
          response.write(prop.value);
          response.write("\n");
        }
        
        response.end();
      }
    }
  }
  
  static String toString(Object value) {
    if (value == null) {
      return "<null>";
    } else if (value instanceof String) {
      return (String) value;
    } else {
      return value.toString();
    }
  }
  
  static List<Property> getProperties() {
    return System.getProperties()
            .entrySet()
            .stream()
            .map((entry) -> new Property(toString(entry.getKey()), toString(entry.getValue())))
            .sorted((a, b) -> a.name.compareTo(b.name))
            .collect(Collectors.toList());
  }


}
