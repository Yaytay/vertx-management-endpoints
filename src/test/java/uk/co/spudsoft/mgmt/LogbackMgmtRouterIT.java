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
import io.restassured.http.ContentType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@Timeout(60000)
public class LogbackMgmtRouterIT {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(LogbackMgmtRouterIT.class);
  
  private int port;
  
  private static class ServerVerticle extends AbstractVerticle {
    
    private HttpServer httpServer;
    private Router router;

    public int getPort() {
      return httpServer.actualPort();
    }
    
    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
      httpServer.close().onComplete(stopPromise);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
      httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));
      router = Router.router(vertx);
      router.route("/manage/logback*").handler(new LogbackMgmtRouter());
  
      httpServer
              .requestHandler(router)
              .listen()
              .map(hs -> (Void) null)
              .onComplete(startPromise)
              ;
    }
  }
  
  @Test
  public void testHandle(Vertx vertx, VertxTestContext testContext) throws Throwable {

    ServerVerticle verticle = new ServerVerticle();
    
    vertx
            .deployVerticle(verticle)
            .compose(verticleName -> {
                port = verticle.getPort();
                RestAssured.port = port;
                logger.debug("Listening on port {}", port);

                testContext.verify(() -> {
                  long start = System.currentTimeMillis();
                  given()
                      .log().all()
                      .get("/manage/logback")
                      .then()
                      .statusCode(200)
                      .body(
                          containsString("\"uk.co.spudsoft.mgmt.LogbackMgmtRouterIT\":{\"name\":\"uk.co.spudsoft.mgmt.LogbackMgmtRouterIT\",\"effectiveLevel\":\"TRACE\",\"additive\":true,\"appenders\":[]}")
                      );
                  logger.debug("First request took {}s", (System.currentTimeMillis() - start) / 1000.0);
                  start = System.currentTimeMillis();
                  given()
                      .accept(ContentType.HTML)
                      .log().all()
                      .get("/manage/logback")
                      .then()
                      .statusCode(200)
                      .body(
                          not(containsString("\"uk.co.spudsoft.mgmt.LogbackMgmtRouterIT\":{\"name\":\"uk.co.spudsoft.mgmt.LogbackMgmtRouterIT\",\"effectiveLevel\":\"TRACE\",\"additive\":true,\"appenders\":[]}"))
                      );
                  logger.debug("Second request took {}s", (System.currentTimeMillis() - start) / 1000.0);
                  start = System.currentTimeMillis();
                  given()
                      .body("{\"level\":\"WARN\"}")
                      .log().all()
                      .put("/manage/logback/uk.co.spudsoft.mgmt.LogbackMgmtRouterIT")
                      .then()
                      .statusCode(200)
                      .body(
                          containsString("\"uk.co.spudsoft.mgmt.LogbackMgmtRouterIT\":{\"name\":\"uk.co.spudsoft.mgmt.LogbackMgmtRouterIT\",\"level\":\"WARN\",\"effectiveLevel\":\"WARN\",\"additive\":true,\"appenders\":[]}")
                      );                          
                  logger.debug("Third request took {}s", (System.currentTimeMillis() - start) / 1000.0);
                  start = System.currentTimeMillis();
                  given()
                      .accept(ContentType.HTML)
                      .log().all()
                      .get("/manage/logback")
                      .then()
                      .statusCode(200)
                      .body(
                          not(containsString("\"uk.co.spudsoft.mgmt.LogbackMgmtRouterIT\":{\"name\":\"uk.co.spudsoft.mgmt.LogbackMgmtRouterIT\",\"effectiveLevel\":\"TRACE\",\"additive\":true,\"appenders\":[]}"))
                      );
                  logger.debug("Fourth request took {}s", (System.currentTimeMillis() - start) / 1000.0);
                });

                testContext.completeNow();
                return Future.succeededFuture();
              });
  }
}
