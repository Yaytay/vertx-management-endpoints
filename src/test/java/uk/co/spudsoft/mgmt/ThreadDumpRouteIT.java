/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.co.spudsoft.mgmt;

import io.restassured.RestAssured;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
public class ThreadDumpRouteIT {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ThreadDumpRouteIT.class);
  
  private int port;
  
  @Test
  public void testHandle(Vertx vertx, VertxTestContext testContext) throws Throwable {

    Router router = Router.router(vertx);
    Router mgmtRouter = Router.router(vertx);
    router.route("/manage/*").subRouter(mgmtRouter);
    HttpServerVerticle httperServerVerticle = new HttpServerVerticle(router);
    
    ThreadDumpRoute.createAndDeploy(mgmtRouter);
    
    vertx
            .deployVerticle(httperServerVerticle)
            .compose(verticleName -> {
                port = httperServerVerticle.getPort();
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

                  given()
                      .log().all()
                      .accept("text/html")
                      .get("/manage/threaddump")
                      .then()
                      .statusCode(200)
                      .log().body()
                      .body(
                          containsString("RUNNABLE")
                          , containsString("TIMED_WAIT")
                      )
                      ;
                  
                  given()
                      .log().all()
                      .accept("text/plain")
                      .get("/manage/threaddump")
                      .then()
                      .statusCode(200)
                      .log().body()
                      .body(
                          containsString("RUNNABLE")
                          , containsString("TIMED_WAIT")
                      )
                      ;
                  
                  given()
                      .log().all()
                      .accept("application/json")
                      .get("/manage/threaddump")
                      .then()
                      .statusCode(200)
                      .log().body()
                      .body(
                          containsString("RUNNABLE")
                          , containsString("TIMED_WAIT")
                      )
                      ;
                  
                  
                  
                });

                testContext.completeNow();
                return Future.succeededFuture();
              });
  };
  
}
