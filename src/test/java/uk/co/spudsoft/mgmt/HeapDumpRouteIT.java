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
import io.vertx.junit5.Timeout;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class HeapDumpRouteIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(HeapDumpRouteIT.class);
  
  private int port;
    
  public HeapDumpRouteIT() {
  }

  @Test
  @Timeout(value = 6, timeUnit = TimeUnit.MINUTES)
  public void testHandle(Vertx vertx, VertxTestContext testContext) throws Throwable {

    Router router = Router.router(vertx);
    Router mgmtRouter = Router.router(vertx);
    router.route("/manage/*").subRouter(mgmtRouter);
    HttpServerVerticle httperServerVerticle = new HttpServerVerticle(router);
    
    HeapDumpRoute.createAndDeploy(mgmtRouter);
    
    vertx
            .deployVerticle(httperServerVerticle)
            .compose(verticleName -> {
                port = httperServerVerticle.getPort();
                RestAssured.port = port;
                logger.debug("Listening on port {}", port);
    
                testContext.verify(() -> {

                  long start = System.currentTimeMillis();
                  String contentLengthString = given()
                      .get("/manage/" + HeapDumpRoute.PATH)
                      .then()
                      .statusCode(200)
                      .extract().header(HttpHeaderNames.CONTENT_LENGTH.toString())
                      ;                  
                  int length = Integer.parseInt(contentLengthString);
                  logger.debug("First request took {}s to produce a file of {} bytes ({}MB)"
                          , (System.currentTimeMillis() - start) / 1000.0
                          , contentLengthString
                          , length / (1024 * 1024)
                  );
                  assertThat(length, greaterThan(1000000));
                  
                  String body = given()
                          .accept(ContentType.HTML)
                          .get("/manage/" + HeapDumpRoute.PATH)
                          .then()
                          .statusCode(200)
                          .contentType(ContentType.HTML)
                          .extract().body().asString();
                  logger.debug("HTML: {}", body);
                  assertThat(body, containsString("?_fmt=binary"));

                  byte[] bytes = given()
                          .accept(ContentType.HTML)
                          .get("/manage/" + HeapDumpRoute.PATH + "?_fmt=binary")
                          .then()
                          .statusCode(200)
                          .contentType("application/octet-stream")
                          .extract().body().asByteArray();
                  logger.debug("Downloaded {} bytes", bytes.length);

                });
                        
                testContext.completeNow();
                return Future.succeededFuture();
                
            });
  }
}
