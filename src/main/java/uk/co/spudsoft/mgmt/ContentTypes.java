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

import io.vertx.ext.web.RoutingContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for handling content types consistently across management routes.
 * 
 * @author jtalbut
 */
public final class ContentTypes {
  
  /**
   * Query string parameter that can be used to override Accept header for management routes.
   */
  public static final String PARAM = "_fmt";
  
  /**
   * JSON content type.
   */
  public static final String TYPE_JSON = "application/json";
  /**.
   * HTML content type.
   */
  public static final String TYPE_HTML = "text/html";
  /**.
   * Plain text content type.
   */
  public static final String TYPE_PLAIN = "text/plain";
  /**.
   * Binary stream content type.
   */
  public static final String TYPE_BINARY = "application/octet-stream";
  
  private static final Map<String, String> PARAM_TO_TYPE = buildParamTypeMap();

  private static Map<String, String> buildParamTypeMap() {
    Map<String, String> result = new HashMap<>();
    result.put("html", TYPE_HTML);
    result.put("text", TYPE_PLAIN);
    result.put("json", TYPE_JSON);
    result.put("binary", TYPE_JSON);
    return Collections.unmodifiableMap(result);
  }

  private ContentTypes() {
  }
  
  /**
   * If the PARAM ('_fmt') argument is found in the query string parameters and the value of that parameter is found in PARAM_TO_TYPE then replace
   * the AcceptableContentType with the value from the PARAM_TO_TYPE map.
   * 
   * The there are multiple '_fmt' arguments then only the first usable one is used.
   * 
   * @param rc The routing context.
   */
  public static void adjustFromParams(RoutingContext rc) {
    
    List<String> values = rc.queryParam(PARAM);
    if (values != null) {
      for (String value : values) {
        String type = PARAM_TO_TYPE.get(value);
        if (type != null) {
          rc.setAcceptableContentType(type);
          return ;
        }
      }
    }
    
  }
  
}
