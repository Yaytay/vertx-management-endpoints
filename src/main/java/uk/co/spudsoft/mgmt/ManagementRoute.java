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

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
  * A Vertx HTTP Server route for providing easy access to other "management" routes.
 * <p>
 * It is strongly recommended that this endpoint is not accessible to end users.
 * <p>
 * A typical deployment of the management endpoints should look something like this:
 * <pre>
     // Create the root router
     Router router = Router.router(vertx);
    
    // Create the management endpoints router
    Router mgmtRouter = Router.router(vertx);
     
    // Deploy the management endpoints to the management endpoints router (potentially deploying capturing routes to the root router)
    ManagementRoute.deployStandardMgmtEndpoints(mgmtRouter, router, params.getManagementEndpoints(), params);
    // Add custom management endpoints.
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "up")) {
      mgmtRouter.get("/up").handler(upCheckHandler).setName("Up");
    }
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "health")) {
      mgmtRouter.get("/health").handler(healthCheckHandler).setName("Health");
    }
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "prometheus")) {
      mgmtRouter.get("/prometheus").handler(new PrometheusScrapingHandlerImpl()).setName("Prometheus");
    }
    
    // Set up CORS for the system
    CorsHandler corsHandler = null;
    if (!Strings.isNullOrEmpty(params.getCorsAllowedOriginRegex())) {
      corsHandler = CorsHandler.create()
              .addRelativeOrigin(params.getCorsAllowedOriginRegex());
      corsHandler.allowedMethods(
              ImmutableSet
                      .&lt;HttpMethod>builder()
                      .add(HttpMethod.GET)
                      .add(HttpMethod.PUT)
                      .add(HttpMethod.DELETE)
                      .add(HttpMethod.POST)
                      .build()
      );
      router.route("/*").handler(corsHandler); 
    }
    
    // Deploy the management router, potentially creating a new HttpServer
    ManagementRoute.createAndDeploy(this.vertx
            , router
            , params.getHttperServerOptions()
            , params.getManagementEndpointPort()
            , corsHandler
            , mgmtRouter
            , params.getManagementEndpointUrl()
    );

    // Deploy other routes
    router.route("/api/*").handler(...);
    router.route("/ui/*").handler(...);
    router.getWithRegex("/openapi\\..*").blockingHandler(openApiHandler);
    router.get("/openapi").handler(openApiHandler.getUiHandler());
    router.route("/").handler(rc -> {
      rc.response().setStatusCode(307);
      rc.redirect("/ui/");
    });

    // Start the primary HttpServer
    httpServer
            .requestHandler(router)
            .listen()            
 * </pre>
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
    router.route("/" + PATH + "/*").subRouter(mgmtRouter);
    router.route(HttpMethod.GET, "/" + PATH)
            .handler(this::handle)
            .setName("Management Routes")
            .produces(ContentTypes.TYPE_JSON)
            .produces(ContentTypes.TYPE_HTML)
            .produces(ContentTypes.TYPE_PLAIN)            
            ;
  }
  
  /**
   * Factory method to do standard deployment.
   * <p>
   * If the mgmtPort is null the management route will simply be created and added to the rootRouter.
   * If the mgmtPort is positive a new HttpServer will be created on that port purely to serve the management endpoints.
   * If the mgmtPort is not null or positive then the management route will not be created at all.
   * <p>
   * If a positive value is provided for mgmtPort a new route may be added to the rootRouter to tell client requests the 
   * alternate location for the management endpoints.
   * This cannot be done by a standard HTTP redirect because the client will not handle CORS in a usable manner on HTTP redirects
   * , so it is instead done by providing a JSON object with a single 'location' value.
   * The value used for the new URL is the mgmtEndpointUrl parameter and this redirection only happens if this value is not null (or empty).
   * 
   * @param vertx Vertx instance, needed for the creation of a new HttpServer.
   * @param rootRouter The root router for the primary HttpServer.
   * @param httperServerOptions HttpServerOptions used for the creation of a new HttpServer (will be copied and have the port set correctly).
   * @param mgmtPort The port that should be used for the management endpoint - the key control value for this method.
   * @param corsHandler The {@link io.vertx.ext.web.handler.CorsHandler} that can be added to the router of the new HttpServer.
   * @param mgmtRouter The router that contains the management endpoints.
   * @param mgmtEndpointUrl The URL that clients should use (instead of /manage on the standard port) for accessing the management endpoints.
   * @return HttpServer A {@link io.vertx.core.Future} containing created HttpServer, if any (otherwise null).
   */
  public static Future<HttpServer> createAndDeploy(Vertx vertx, Router rootRouter, HttpServerOptions httperServerOptions, Integer mgmtPort, CorsHandler corsHandler, Router mgmtRouter, String mgmtEndpointUrl) {
    if (mgmtPort == null) {
      ManagementRoute route = new ManagementRoute(mgmtRouter);
      route.standardDeploy(rootRouter);
      return null;
    } else if (mgmtPort > 0) {
      Router mgmtParentRouter = Router.router(vertx);
      if (corsHandler != null) {
        mgmtParentRouter.route("/*").handler(corsHandler);
      }
      ManagementRoute route = new ManagementRoute(mgmtRouter);
      route.standardDeploy(mgmtParentRouter);
      HttpServerOptions options = new HttpServerOptions(httperServerOptions);
      options.setPort(mgmtPort);
      HttpServer mgmtHttpServer = vertx.createHttpServer(options);
      mgmtHttpServer.requestHandler(mgmtParentRouter);
      if (mgmtEndpointUrl != null && !mgmtEndpointUrl.isEmpty() && rootRouter != null) {
        rootRouter.get("/" + PATH).handler(rc -> {
          HttpServerResponse response = rc.response();
          response.setStatusCode(200);
          response.putHeader("Content-Type", "application/json");
          JsonObject data = new JsonObject();
          data.put("location", mgmtEndpointUrl);
          rc.end(data.toBuffer());
        });
      }
      return mgmtHttpServer.listen(mgmtPort);
    } else {
      return null;
    }
  }
  
  /**
   * Simple helper method to determine whether path is in the enabledEndpoints.
   * <p>
   * Returns true if either enabledEndpoints is empty or it contains path.
   * @param enabledEndpoints The list of enabled endpoints (may not be null, but if empty all endpoints are enabled).
   * @param path The path being tested.
   * @return true if either enabledEndpoints is empty or it contains path.
   */
  public static boolean mgmtEndpointPermitted(List<String> enabledEndpoints, String path) {
    if (enabledEndpoints.isEmpty()) {
      return true;
    } else {
      return enabledEndpoints.contains(path);
    }
  }
  
  /**
   * Helper method that creates all the standard management routes.
   * <p>
   * Use of this method is entirely optional, but you are likely to end up with a similar implementation if you don't use it.
   * <p>
   * This should be called as the first thing you do after creating the rootRouter in order that the logging routes can capture all traffic.
   * <p>
   * It is not necessary for {@link createAndDeploy} to be called early, that can be left until later in your setup process.
   * <p>
   * This method primarily makes changes to the mgmtRouter, but routes are added the rootRouter for capturing purposes.
   * <p>
   * The enabledEndpoints parameter can be used to control which routes are enabled.
   * If the list is empty all routes are enabled, otherwise only those routes whose sub path is in the list are enabled.
   * The available values are:
   * <ul>
   * <li>parameters
   * Dumps the full set of configuration parameters.
   * <li>envvars
   * Dumps all environment variables.
   * <li>sysprops
   * Dumps all system properties.
   * <li>accesslog
   * Reports the past few requests to the system.
   * <li>inflight
   * Reports all requests made to the system that have not yet completed.
   * <li>threads
   * Dump stack traces from all threads.
   * <li>heapdump
   * Download a heap dump.
   * </ul>
   * <p>
   * It is encouraged to add more routes to the mgmtRouter outside of this method for service-specific management endpoint 
   * (such as health and metrics).
   * 
   * @param mgmtRouter The router that will have additional output routes added.
   * @param rootRouter The root router on the primary endpoint for the service, this will have capturing routes added to it.
   * @param enabledEndpoints A {@link java.util.List} of Strings that are the endpoints that should be enabled.
   * @param params {@link java.util.concurrent.atomic.AtomicReference} to the parameters object that will be reported by the 'parameters' endpoint.
   * If the parameters endpoint is enabled this must be a valid object that can be processed by the Vertx JSON object mapper.
   */
  public static void deployStandardMgmtEndpoints(Router mgmtRouter, Router rootRouter, List<String> enabledEndpoints, AtomicReference<Object> params) {
    
    AccessLogCaptureRoute capture = null;
    if (mgmtEndpointPermitted(enabledEndpoints, AccessLogOutputRoute.PATH)) {
      capture = new AccessLogCaptureRoute(30);
      rootRouter.route("/*").handler(capture); 
    }
    
    if (mgmtEndpointPermitted(enabledEndpoints, HeapDumpRoute.PATH)) {
      HeapDumpRoute.createAndDeploy(mgmtRouter);
    }
    if (mgmtEndpointPermitted(enabledEndpoints, InFlightRoute.PATH)) {
      InFlightRoute.createAndDeploy(rootRouter, mgmtRouter);
    }
    if (mgmtEndpointPermitted(enabledEndpoints, LogbackMgmtRoute.PATH)) {
      LogbackMgmtRoute.createAndDeploy(mgmtRouter);
    }
    if (mgmtEndpointPermitted(enabledEndpoints, ThreadDumpRoute.PATH)) {
      ThreadDumpRoute.createAndDeploy(mgmtRouter);
    }
    if (capture != null) {
      AccessLogOutputRoute.createAndDeploy(mgmtRouter, capture.getBuffer());
    }
    if (mgmtEndpointPermitted(enabledEndpoints, DumpEnvRoute.PATH)) {
      DumpEnvRoute.createAndDeploy(mgmtRouter);
    }
    if (mgmtEndpointPermitted(enabledEndpoints, DumpSysPropsRoute.PATH)) {
      DumpSysPropsRoute.createAndDeploy(mgmtRouter);
    }
    if (mgmtEndpointPermitted(enabledEndpoints, ParametersRoute.PATH) && params != null) {
      ParametersRoute.createAndDeploy(mgmtRouter, params);
    }
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
