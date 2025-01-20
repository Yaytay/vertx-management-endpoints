package uk.co.spudsoft.mgmt.sandbox;

import static com.jayway.awaitility.Awaitility.await;
import io.restassured.RestAssured;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.mgmt.HeapDumpRoute;
import uk.co.spudsoft.mgmt.HttpServerVerticle;
import uk.co.spudsoft.mgmt.InFlightRoute;
import uk.co.spudsoft.mgmt.LogbackMgmtRoute;
import uk.co.spudsoft.mgmt.ManagementRoute;
import uk.co.spudsoft.mgmt.ThreadDumpRoute;

/*
 * Copyright (C) 2025 njt
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

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
public class RunIT {
  
  private static final Logger logger = LoggerFactory.getLogger(RunIT.class);
  
  private int port;
  
  @Test
  public void runManagementEndpoints(Vertx vertx) {
    
    Router router = Router.router(vertx);
    Router mgmtRouter = Router.router(vertx);
    
    InFlightRoute.createAndDeploy(router, mgmtRouter);
    HeapDumpRoute.createAndDeploy(mgmtRouter);
    LogbackMgmtRoute.createAndDeploy(mgmtRouter);    
    ThreadDumpRoute.createAndDeploy(mgmtRouter);
    ManagementRoute.createAndDeploy(null, router, null, null, null, mgmtRouter, null);
    
    
    HttpServerVerticle httperServerVerticle = new HttpServerVerticle(router);
    
    Future<Void> ready = vertx
            .deployVerticle(httperServerVerticle)
            .compose(verticleName -> {
                port = httperServerVerticle.getPort();
                RestAssured.port = port;
                logger.debug("Listening at http://localhost:{}/manage", port);
                return Future.succeededFuture();
              });

    await().until(() -> ready.isComplete());
    
    assertTrue(ready.succeeded());
            
    for (int i = 0; i < 14400; ++i) {
      try {
        Thread.sleep(1000);
      } catch(InterruptedException ex) {
        
      }
    }    
  }
  
}
