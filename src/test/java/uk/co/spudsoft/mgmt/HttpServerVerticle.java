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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

/**
 *
 * @author jtalbut
 */
public class HttpServerVerticle extends AbstractVerticle {

  private HttpServer httpServer;
  private Router router;

  public HttpServerVerticle(Router router) {
    this.router = router;
  }

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

    httpServer
            .requestHandler(router)
            .listen()
            .map(hs -> (Void) null)
            .onComplete(startPromise);
  }

}
