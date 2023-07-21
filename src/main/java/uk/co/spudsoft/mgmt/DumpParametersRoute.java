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
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Vertx HTTP Server route for allowing users to download a thread dump of the process.
 *
 * It is strongly recommended that this endpoint be mounted on via a subrouter, the path to which is only accessible from authorised personnel.
 * The integration tests demonstrate the use of a suitable subrouter to locate the endpoint at /manage/threaddump.
 * 
 * @author jtalbut
 */
public class DumpParametersRoute implements Handler<RoutingContext> {

  /**
   * The path at which the standardDeploy method will put the router.
   */
  public static final String PATH = "parameters";
  
  private static final String TYPE_JSON = "application/json";
  private static final String TYPE_HTML = "text/html";
  private static final String TYPE_PLAIN = "text/plain";
  
  private final AtomicReference<Object> reference;
  
  /**
   * Constructor.
   * @param reference to the service, as a single object that will be JSON encoded.
   * The reference is passed in as an AtomicReference so that it may be changed.
   */
  public DumpParametersRoute(AtomicReference<Object> reference) {
    this.reference = reference;
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
            .setName("Parameters")
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
   * @param reference to the service, as a single object that will be JSON encoded.
   * The reference is passed in as an AtomicReference so that it may be changed.
   */
  public static void createAndDeploy(Router router, AtomicReference<Object> reference) {
    DumpParametersRoute route = new DumpParametersRoute(reference);
    route.standardDeploy(router);
  }  
  
  @Override
  public void handle(RoutingContext rc) {
    
    HttpServerRequest request = rc.request();
    
    Object value = reference.get();
    
    if (request.method() == HttpMethod.GET) {
      
      if (TYPE_JSON.equals(rc.getAcceptableContentType())) {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, TYPE_JSON);
        response.end(Json.encode(value));
      } else if (TYPE_HTML.equals(rc.getAcceptableContentType())) {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, TYPE_HTML);
        response.setChunked(true);
        
        response.write("<html>");
        response.write("<head>");
        response.write("</head>");
        response.write("<body>");
        
        response.write("<pre>");
        
        response.write(Json.encodePrettily(value));
        
        response.write("</pre>");
        
        response.write("</body>");
        response.write("</html>");
        
        response.end();
      } else {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, TYPE_PLAIN);
        
        response.end(Json.encodePrettily(value));
      }
    } else {
      rc.next();
    }
  }

}
