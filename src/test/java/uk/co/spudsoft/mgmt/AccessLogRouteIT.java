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

import io.restassured.RestAssured;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class AccessLogRouteIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AccessLogRouteIT.class);
  
  private int port;
    
  public AccessLogRouteIT() {
  }

  @Test
  public void testHandle(Vertx vertx, VertxTestContext testContext) throws Throwable {

    AccessLogCaptureRoute capture = new AccessLogCaptureRoute(4);
    
    Router router = Router.router(vertx);
    router.route("/*").handler(capture);

    Router mgmtRouter = Router.router(vertx);
    router.route("/manage/*").subRouter(mgmtRouter);
    HttpServerVerticle httperServerVerticle = new HttpServerVerticle(router);
    
    AccessLogOutputRoute.createAndDeploy(mgmtRouter, capture.getBuffer());
    
    vertx
            .deployVerticle(httperServerVerticle)
            .compose(verticleName -> {
                port = httperServerVerticle.getPort();
                RestAssured.port = port;
                logger.debug("Listening on port {}", port);
    
                testContext.verify(() -> {

                  String body = given()
                      .get("/manage/" + AccessLogOutputRoute.PATH)
                      .then()
                      .statusCode(200)
                      .extract().body().asString()
                      ;                  
                  logger.debug("Access log (any): {}", body);

                  body = given()
                      .accept(ContentType.HTML)
                      .get("/manage/" + AccessLogOutputRoute.PATH)
                      .then()
                      .statusCode(200)
                      .extract().body().asString()
                      ;                  
                  logger.debug("Access log (html): {}", body);

                  body = given()
                      .accept(ContentType.JSON)
                      .get("/manage/" + AccessLogOutputRoute.PATH)
                      .then()
                      .statusCode(200)
                      .extract().body().asString()
                      ;                  
                  logger.debug("Access log (json): {}", body);

                  body = given()
                      .accept(ContentType.TEXT)
                      .get("/manage/" + AccessLogOutputRoute.PATH)
                      .then()
                      .statusCode(200)
                      .extract().body().asString()
                      ;                  
                  logger.debug("Access log (plain): {}", body);

                  body = given()
                      .accept(ContentType.HTML)
                      .get("/manage/" + AccessLogOutputRoute.PATH)
                      .then()
                      .statusCode(200)
                      .extract().body().asString()
                      ;                  
                  logger.debug("Access log (html): {}", body);

                });
                        
                testContext.completeNow();
                return Future.succeededFuture();
                
            });
  }
}
