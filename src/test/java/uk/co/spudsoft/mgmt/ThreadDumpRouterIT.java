/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.co.spudsoft.mgmt;

import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class ThreadDumpRouterIT {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ThreadDumpRouterIT.class);
  
  private int port;
  
  @Test
  public void testHandle(Vertx vertx, VertxTestContext testContext) throws Throwable {

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));
    Router router = Router.router(vertx);
    router.route("/manage/threaddump").handler(new ThreadDumpRouter());
    
    httpServer
            .requestHandler(router)
            .listen()
            .onFailure(ex -> {
              testContext.failNow(ex);
            })
            .onSuccess(hs -> {
              vertx.executeBlocking(p -> {
                port = httpServer.actualPort();
                RestAssured.port = port;
                logger.debug("Listening on port {}", port);

                testContext.verify(() -> {
                  long start = System.currentTimeMillis();
                  given()
                      .log().all()
                      .get("/manage/threaddump")
                      .then()
                      .statusCode(200)
                      .log().body()
                      .body(
                          containsString("RUNNABLE")
                          , containsString("TIMED_WAIT")
                      )
                      ;
                  logger.debug("First request took {}s", (System.currentTimeMillis() - start) / 1000.0);
                });

                testContext.completeNow();
              });
            });
  };
  
}
