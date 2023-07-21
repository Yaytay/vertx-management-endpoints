/*
 * Copyright (C) 2023 jtalbut
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

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Vertx HTTP Server route capturing requests and storing them in a RingBuffer.
 * 
 * The captured routes can be output via the AccessLogOutputRoute.
 *
 * @author jtalbut
 */
public class AccessLogCaptureRoute implements Handler<RoutingContext> {

  private static final Logger logger = LoggerFactory.getLogger(AccessLogCaptureRoute.class);
  
  private final RingBuffer<AccessLogData> buffer;
  
  /**
   * POD for holding captured data relating to a request.
   */
  public static class AccessLogData {
    private final long timestamp;
    private final HttpServerRequest request;
    private long endTimestamp;
    private HttpServerResponse response;

    /**
     * Constructor.
     * @param timestamp The timestamp of the request.
     * @param request The details of the request.
     */
    public AccessLogData(long timestamp, HttpServerRequest request) {
      this.timestamp = timestamp;
      this.request = request;
    }

    /**
     * Get the timestamp of the request.
     * @return the timestamp of the request.
     */
    public long getTimestamp() {
      return timestamp;
    }

    /**
     * Get the details of the request.
     * @return the details of the request.
     */
    public HttpServerRequest getRequest() {
      return request;
    }

    /**
     * Get the timestamp of the end of the response.
     * @return the timestamp of the end of the response.
     */
    public long getEndTimestamp() {
      return endTimestamp;
    }

    /**
     * Get the details of the response.
     * @return the details of the response.
     */
    public HttpServerResponse getResponse() {
      return response;
    }

    /**
     * Set the timestamp of the end of the response.
     * @param endTimestamp the timestamp of the end of the response.
     */
    public void setEndTimestamp(long endTimestamp) {
      this.endTimestamp = endTimestamp;
    }

    /**
     * Set the details of the response.
     * @param response the details of the response.
     */
    public void setResponse(HttpServerResponse response) {
      this.response = response;
    }
    
  }
  
  /**
   * Constructor.
   * 
   * @param bufferSize The number of requests to keep in the buffer.
   */
  public AccessLogCaptureRoute(int bufferSize) {
    this.buffer = new RingBuffer<>(bufferSize);
  }

  /**
   * Get the buffer.
   * @return the buffer.
   */
  public RingBuffer<AccessLogData> getBuffer() {
    return buffer;
  }
  
  @Override
  public void handle(RoutingContext context) {
    AccessLogData ald = new AccessLogData(System.currentTimeMillis(), context.request());
    
    buffer.add(ald);

    context.addBodyEndHandler(v -> {
      ald.setEndTimestamp(System.currentTimeMillis());
      ald.setResponse(context.response());
    });

    context.next();
  }

}
