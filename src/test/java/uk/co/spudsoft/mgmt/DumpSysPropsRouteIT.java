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
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class DumpSysPropsRouteIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(DumpSysPropsRouteIT.class);
  
  private int port;
    
  public DumpSysPropsRouteIT() {
  }

  @Test
  public void testHandle(Vertx vertx, VertxTestContext testContext) throws Throwable {

    Router router = Router.router(vertx);
    Router mgmtRouter = Router.router(vertx);
    router.route("/manage/*").subRouter(mgmtRouter);
    HttpServerVerticle httperServerVerticle = new HttpServerVerticle(router);
    
    DumpSysPropsRoute.createAndDeploy(mgmtRouter);
    
    vertx
            .deployVerticle(httperServerVerticle)
            .compose(verticleName -> {
                port = httperServerVerticle.getPort();
                RestAssured.port = port;
                logger.debug("Listening on port {}", port);
    
                testContext.verify(() -> {

                  String body = given()
                      .get("/manage/dumpsysprops")
                      .then()
                      .statusCode(200)
                      .extract().body().asString()
                      ;                  
                  logger.debug("System properties (any): {}", body);

                  body = given()
                      .accept(ContentType.HTML)
                      .get("/manage/dumpsysprops")
                      .then()
                      .statusCode(200)
                      .extract().body().asString()
                      ;                  
                  logger.debug("System properties (html): {}", body);

                  body = given()
                      .accept(ContentType.JSON)
                      .get("/manage/dumpsysprops")
                      .then()
                      .statusCode(200)
                      .extract().body().asString()
                      ;                  
                  logger.debug("System properties (json): {}", body);

                  body = given()
                      .accept(ContentType.TEXT)
                      .get("/manage/dumpsysprops")
                      .then()
                      .statusCode(200)
                      .extract().body().asString()
                      ;                  
                  logger.debug("System properties (plain): {}", body);
                });
                        
                testContext.completeNow();
                return Future.succeededFuture();
                
            });
  }
}
