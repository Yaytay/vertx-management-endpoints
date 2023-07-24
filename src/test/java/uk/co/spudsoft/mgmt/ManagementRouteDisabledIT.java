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
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class ManagementRouteDisabledIT {
  
  private static final Logger logger = LoggerFactory.getLogger(ManagementRouteDisabledIT.class);
  
  @Test
  public void testHttpServer(Vertx vertx, VertxTestContext testContext) throws IOException {

    Router rootRouter = Router.router(vertx);
    Router mgmtRouter = Router.router(vertx);
    
    ManagementRoute.deployStandardMgmtEndpoints(mgmtRouter, rootRouter, Collections.emptyList(), new AtomicReference<>("Hello"));
    Future<HttpServer> serverFuture = ManagementRoute.createAndDeploy(vertx, rootRouter, new HttpServerOptions(), -1, null, mgmtRouter, null);
    
    assertNull(serverFuture);

    HttpServerVerticle httperServerVerticle = new HttpServerVerticle(rootRouter);
    
    vertx
            .deployVerticle(httperServerVerticle)
            .compose(verticleName -> {
                int port = httperServerVerticle.getPort();
                RestAssured.port = port;
                logger.debug("Listening on port {}", port);
    
                testContext.verify(() -> {

                  String body = given()
                      .accept("")
                      .get("/manage")
                      .then()
                      .statusCode(404)
                      .log().all()
                      .extract().body().asString()
                      ;                  
                  logger.debug("Response: {}", body);

                });
                        
                testContext.completeNow();
                return Future.succeededFuture();
                
            });
  }
}
