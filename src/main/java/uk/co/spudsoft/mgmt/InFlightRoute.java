/*
 * Copyright (C) 2022 njt
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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Vertx Verticle for allowing users to download information about all HTTP requests currently being processed by the server.
 * 
 * It is probably very important, for obvious security reasons, that this endpoint is not accessible to end users.
 * It is strongly recommended that this endpoint be mounted on via a subrouter, the path to which is only accessible from authorised personnel.
 * The integration tests demonstrate the use of a suitable subrouter to locate the endpoint at /manage/heapdump.
 * 
 * @author njt
 */
public class InFlightRoute implements Handler<RoutingContext> {
  
  /**
   * The key value that will be used for storing the timestamp of the start of processing in the RoutingContext.
   */
  public static final String TIMESTAMP_KEY = InFlightRoute.class.getCanonicalName() + "_StartTimestamp";
          
  private static final Logger logger = LoggerFactory.getLogger(InFlightRoute.class);

  private final Map<SocketAddress, RoutingContext> map = new HashMap<>();  

  /**
   * Constructor.
   */
  public InFlightRoute() {
  }
  
  /**
   * Deploy the route to the router passed in at the normal endpoint.
   * 
   * The manageRouter passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param rootRouter The top level router for the HttpServer that will be monitored.
   * @param manageRouter The router that this handler will be attached to.
   */
  public void standardDeploy(Router rootRouter, Router manageRouter) {
    rootRouter.route().handler(this::record);
    manageRouter.route().handler(this::record);
    manageRouter.route(HttpMethod.GET, "/inflight").handler(this::handle);
  }
  
  /**
   * Factory method to do standard deployment on newly constructed route.
   * 
   * The manageRouter passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param rootRouter The top level router for the HttpServer that will be monitored.
   * @param manageRouter The router that this handler will be attached to.
   */
  public static void createAndDeploy(Router rootRouter, Router manageRouter) {
    InFlightRoute route = new InFlightRoute();
    route.standardDeploy(rootRouter, manageRouter);
  }
  
  
  @Override
  public void handle(RoutingContext event) {
    JsonArray result = new JsonArray();
    synchronized (map) {
      for (RoutingContext rc : map.values()) {
        try {
          JsonObject data = new JsonObject();
          if (rc.get(TIMESTAMP_KEY) instanceof Long timestamp) {
            data.put("StartTimestamp", Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).toLocalDateTime().toString());
            data.put("SecondsSoFar", (System.currentTimeMillis() - timestamp) / 1000.0);
          } 
          data.put("LocalAddress", Objects.toString(event.request().localAddress()));
          data.put("RemoteAddress", Objects.toString(event.request().remoteAddress()));
          data.put("AbsoluteUri", event.request().absoluteURI());
          data.put("Query", event.request().query());
          result.add(data);
        } catch (Throwable ex) {
          logger.warn("Failed to generate JSON for RoutingContext ({}): ", rc, ex);
        }
      }
    }
    event.end(result.toString());    
  }

  private void record(RoutingContext event) {
    SocketAddress localAddress = event.request().localAddress();
    synchronized (map) {
      event.put(TIMESTAMP_KEY, System.currentTimeMillis());
      map.put(localAddress, event);
    }
    event.addEndHandler(ar -> {
      synchronized (map) {
        map.remove(localAddress);
      }
    });
    event.next();
  }
  

}
