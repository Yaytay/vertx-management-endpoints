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

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
  * A Vertx HTTP Server route for providing easy access to other "management" routes.
 * 
 * It is strongly recommended that this endpoint is not accessible to end users.
 * 
 * @author jtalbut
 */
public class ManagementRoute implements Handler<RoutingContext> {
  
  /**
   * The path at which the standardDeploy method will put the router.
   */
  public static final String PATH = "manage";
  
  private static final Logger logger = LoggerFactory.getLogger(ManagementRoute.class);
  
  private final Router mgmtRouter;

  /**
   * Constructor.
   * 
   * @param mgmtRouter The router that this route will report.
   */
  public ManagementRoute(Router mgmtRouter) {
    this.mgmtRouter = mgmtRouter;
  }
  
  
  /**
   * Deploy the route to the router passed in at the normal endpoint.
   * 
   * The router passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param router The router that this handler will be attached to.
   */
  public void standardDeploy(Router router) {
    router.route("/manage/*").subRouter(mgmtRouter);
    router.route(HttpMethod.GET, "/" + PATH)
            .handler(this::handle)
            .setName("Management Routes")
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
   * The standard way to configure this router should be something like:
   * 
   * 
   * @param router The router that this handler will be attached to.
   * @param mgmtRouter The router that this route will report.
   */
  public static void createAndDeploy(Router router, Router mgmtRouter) {
    ManagementRoute route = new ManagementRoute(mgmtRouter);
    route.standardDeploy(router);
  }

  @Override
  public void handle(RoutingContext rc) {
    
    HttpServerResponse response = rc.response();
    
    HttpServerRequest request = rc.request();
    
    if (request.method() == HttpMethod.GET) {
      
      ContentTypes.adjustFromParams(rc);
      
      if (ContentTypes.TYPE_HTML.equals(rc.getAcceptableContentType())) {
        returnHtml(request, response);
      } else if (ContentTypes.TYPE_JSON.equals(rc.getAcceptableContentType())) {
        returnJson(request, response);
      } else {
        returnText(request, response);
      }
    }
  }
  
  private void returnJson(HttpServerRequest request, HttpServerResponse response) {
    
    JsonArray result = new JsonArray();
    
    for (Route route : mgmtRouter.getRoutes()) {
      if (route.isExactPath() && route.methods() != null && route.methods().contains(HttpMethod.GET)) {
        JsonObject object = new JsonObject();
        object.put("name", route.getName());
        object.put("url", request.absoluteURI() + route.getPath());
        result.add(object);
      }
    }

    response.setStatusCode(200);
    response.putHeader("Content-Type", ContentTypes.TYPE_JSON);
    response.end(result.toString());    
    
  }

  private void returnText(HttpServerRequest request, HttpServerResponse response) {
    
    StringBuilder result = new StringBuilder();
    for (Route route : mgmtRouter.getRoutes()) {
      if (route.isExactPath() && route.methods() != null && route.methods().contains(HttpMethod.GET)) {
        result.append(route.getName()).append(": ").append(request.absoluteURI()).append(route.getPath());
      }
    }

    response.setStatusCode(200);
    response.putHeader("Content-Type", ContentTypes.TYPE_PLAIN);
    response.end(result.toString());    
    
  }

  private static final String HEAD = "<html><head><title>Management Endpoints</title><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"></head><body><table><tr><th>Route</th><th>Link</th></tr>";
  private static final String TAIL = "</table></body></html>";
  
  private void returnHtml(HttpServerRequest request, HttpServerResponse response) {

    response.putHeader("Content-Type", ContentTypes.TYPE_HTML);
    response.setChunked(true);
    response.write(HEAD);

    for (Route route : mgmtRouter.getRoutes()) {
      if (route.isExactPath() && route.methods() != null && route.methods().contains(HttpMethod.GET)) {
        response.write("<tr><td>" + route.getName() + "</td><td><a href=\"" + request.absoluteURI() + route.getPath() + "\">" + request.absoluteURI() + route.getPath() + "</td></tr>");
      }
    }

    response.end(TAIL);    
    
  }
  
  
  
}
