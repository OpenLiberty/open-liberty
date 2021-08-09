/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.rest.handler.helper.ServletRESTRequestWithParams;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.helper.DefaultRoutingHelper;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMissingRequiredParam;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerUnsupportedMediaType;

/**
 *
 */
public class RESTHelper {
    private static final TraceComponent tc = Tr.register(RESTHelper.class,
                                                         APIConstants.TRACE_GROUP,
                                                         APIConstants.TRACE_BUNDLE_FILE_TRANSFER);

    @FFDCIgnore({ MalformedObjectNameException.class, NullPointerException.class })
    public static ObjectName objectNameConverter(String name, boolean needDecoding, JSONConverter converter) {
        try {
            if (needDecoding) {
                name = URLDecoder.decode(name, "UTF-8");
            }
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (NullPointerException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (UnsupportedEncodingException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    public static String URLDecoder(String name, JSONConverter converter) {
        try {
            return URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    public static String URLEncoder(String name, JSONConverter converter) {
        try {
            return URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    //Filtering incoming header to avoid http splitting attacks
    public static String filterHttpHeader(String original) {
        return original.replaceAll("(\\n|\\r|\\u0085|\\u2028)", "");
    }

    /**
     * This method should be called from an OPTIONS http request, for a preFlight handshake
     * before a CORS invocation can be made when using PUT/POST/DELETE. ex of caller:
     *
     * @OPTIONS
     *          public void preFlight() {
     *          RESTHelper.handlePreFlight(httpServletRequest, httpServletResponse);
     *          }
     */
    public static void handlePreFlight(HttpServletRequest request, HttpServletResponse response) {
        /**
         * NOTE: The following block of code should be re-enabled once we support CORS in the JMX REST Connector
         * This should be off by default and users should have the option to configure the header values.
         */
        /**
         * response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
         *
         * String requestHeaders = request.getHeader("Access-Control-Request-Headers");
         *
         * if (requestHeaders != null) {
         * response.addHeader("Access-Control-Allow-Headers",
         * "Origin, Content-Type, Accept, " +
         * ConnectorSettings.ROUTING_KEY_HOST_NAME + ", " +
         * ConnectorSettings.ROUTING_KEY_SERVER_NAME + ", " +
         * ConnectorSettings.ROUTING_KEY_SERVER_USER_DIR);
         *
         * if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
         * Tr.event("RESTHelper", tc, "Requested headers:" + requestHeaders);
         * }
         * }
         *
         * handleCrossOrigin(request, response);
         */
    }

    /**
     * This method should be called from GET CORS invocations. It is also called from the preFlight handshake method above.
     *
     * @param request
     * @param response
     */
    public static void handleCrossOrigin(HttpServletRequest request, HttpServletResponse response) {
        /**
         * NOTE: The following block of code should be re-enabled once we support CORS in the JMX REST Connector
         * This should be off by default and users should have the option to configure the header values.
         */
        /**
         * String requestOrigin = request.getHeader("Origin");
         *
         * if (requestOrigin != null) {
         * //TODO: Once we have an OSGi component handling the CORS-related metadata from server.xml
         * //then the line below should be changed to return the configured value (with default of empty)
         * //For now, just allow any hosts.
         * response.addHeader("Access-Control-Allow-Origin", filterHttpHeader(requestOrigin));
         * response.addHeader("Access-Control-Allow-Credentials", "true");
         * }
         */
    }

    /**
     * Quick check for multiple-target routing context, without actually fetching all pieces
     */
    public static boolean containsMultipleRoutingContext(RESTRequest request) {
        //TODO: add a check for query string

        if (request instanceof ServletRESTRequestWithParams) {
            ServletRESTRequestWithParams req = (ServletRESTRequestWithParams) request;
            return (req.getParam(ClientProvider.COLLECTIVE_HOST_NAMES) != null || request.getHeader(ClientProvider.COLLECTIVE_HOST_NAMES) != null);
        }
        return request.getHeader(ClientProvider.COLLECTIVE_HOST_NAMES) != null;
    }

    /**
     * Quick check for multiple routing context, without actually fetching all pieces
     */
    public static boolean containsSingleRoutingContext(RESTRequest request) {
        //TODO: add a check for query string
        return request.getHeader(ClientProvider.ROUTING_KEY_HOST_NAME) != null;
    }

    /**
     * This helper method looks for the routing keys in the HTTP headers first, and then fallsback into looking at the query string.
     *
     * @param request of the current request
     * @return a 3-sized String array containing hostName, userDir and serverName respectively, or null if no routing context was found.
     */
    public static String[] getRoutingContext(RESTRequest request, boolean errorIfNull) {
        //Look for headers first
        String targetHost = request.getHeader(ClientProvider.ROUTING_KEY_HOST_NAME);
        if (targetHost != null) {
            targetHost = URLDecoder(targetHost, null);
            String targetUserDir = request.getHeader(ClientProvider.ROUTING_KEY_SERVER_USER_DIR);
            String targetServer = request.getHeader(ClientProvider.ROUTING_KEY_SERVER_NAME);

            targetUserDir = (targetUserDir == null) ? null : URLDecoder(targetUserDir, null);
            targetServer = (targetServer == null) ? null : URLDecoder(targetServer, null);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event("RESTHelper", tc, "Found routing context in headers.  Host:" + targetHost + " | UserDir:" + targetUserDir + " | Server:" + targetServer);
            }

            return new String[] { targetHost, targetUserDir, targetServer };

        } else {

            //TODO: re-visit once a decision is made on the value of these keys (current values are too big

            //Look for query strings (note: query params are not automatically decoded when returned from getQueryString())
            final String queryStr = request.getQueryString();

            //Optimization:  Do a quick lookup to see if the raw queryStr contains a routing context key
            if (queryStr == null || !queryStr.contains(ClientProvider.ROUTING_KEY_HOST_NAME)) {
                if (errorIfNull) {
                    //TODO: make real translated message
                    throw ErrorHelper.createRESTHandlerJsonException(new IOException("routing context was not present in the request!"), null, APIConstants.STATUS_BAD_REQUEST);
                }

                return null;
            }

            //We know it contains at least the host, so split it
            String[] queryParts = queryStr.split("[&=]");
            String[] routingParams = new String[3];

            final int size = queryParts.length;
            for (int i = 0; i < size; i++) {
                if (ClientProvider.ROUTING_KEY_HOST_NAME.equals(queryParts[i])) {
                    //The value will be at i + 1
                    routingParams[0] = URLDecoder(queryParts[i + 1], null);
                    //Move to next key
                    i++;
                    continue;
                } else if (ClientProvider.ROUTING_KEY_SERVER_USER_DIR.equals(queryParts[i])) {
                    //The value will be at i + 1
                    routingParams[1] = URLDecoder(queryParts[i + 1], null);
                    //Move to next key
                    i++;
                    continue;
                } else if (ClientProvider.ROUTING_KEY_SERVER_NAME.equals(queryParts[i])) {
                    //The value will be at i + 1
                    routingParams[2] = URLDecoder(queryParts[i + 1], null);
                    //Move to next key
                    i++;
                    continue;
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event("RESTHelper", tc, "Found routing context in queryStr.  Host:" + routingParams[0] + " | UserDir:" + routingParams[1] + " | Server:" + routingParams[2]);
            }

            return routingParams;
        }
    }

    public static boolean isGetMethod(String method) {
        return APIConstants.METHOD_GET.equals(method);
    }

    public static boolean isPutMethod(String method) {
        return APIConstants.METHOD_PUT.equals(method);
    }

    public static boolean isPostMethod(String method) {
        return APIConstants.METHOD_POST.equals(method);
    }

    public static boolean isDeleteMethod(String method) {
        return APIConstants.METHOD_DELETE.equals(method);
    }

    public static String getRequiredParam(RESTRequest request, String paramName) {
        return getRequiredParam(request, paramName, true);
    }

    private static String getRequiredParam(RESTRequest request, String paramName, boolean decode) {
        String param = request.getPathVariable(paramName);
        if (param == null) {
            throw new RESTHandlerMissingRequiredParam(paramName);
        }

        if (decode) {
            param = URLDecoder(param, null);
        }

        return param;
    }

    public static String getQueryParam(RESTRequest request, String paramName) {
        return getQueryParam(request, paramName, true);
    }

    public static String getQueryParam(RESTRequest request, String paramName, boolean decode) {
        // POST request's body can be read once. Use getQueryParameterValue to avoid re-reading
        // the request's body
        String param = DefaultRoutingHelper.getQueryParameterValue(request, paramName);

        if (decode && (param != null && !param.isEmpty())) {
            param = URLDecoder(param, null);
        }

        return param;
    }

    public static List<String> getQueryParams(RESTRequest request, String paramName) {
        return getQueryParams(request, paramName, true);
    }

    private static List<String> getQueryParams(RESTRequest request, String paramName, boolean decode) {
        // POST request's body can be read once. Use getQueryParameterValues to avoid re-reading
        // the request's body
        List<String> params = asList(DefaultRoutingHelper.getQueryParameterValues(request, paramName));

        if (decode) {
            ArrayList<String> decodedParams = new ArrayList<String>();
            for (String param : params) {
                if (param != null && !param.isEmpty()) {
                    decodedParams.add(URLDecoder(param, null));
                }
            }

            return decodedParams;
        }

        return params;
    }

    public static InputStream getInputStream(RESTRequest request) {
        try {
            return request.getInputStream();
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }
    }

    public static <T> List<T> asList(T[] a) {
        if (a == null || a.length == 0) {
            return new ArrayList<T>();
        } else {
            return Arrays.asList(a);
        }
    }

    public static void ensureConsumesJson(RESTRequest request) {
        String contentType = getType(request.getContentType());
        if (!APIConstants.MEDIA_TYPE_APPLICATION_JSON.equalsIgnoreCase(contentType)) {
            throw new RESTHandlerUnsupportedMediaType(request.getContentType());
        }
    }

    private static String getType(String contentType) {
        // From http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17
        // "Content-Type" ":" media-type
        // media-type = type "/" subtype *( ";" parameter )
        String type;

        if (contentType == null) {
            return null;
        }

        int firstIndex = contentType.indexOf(";");
        if (firstIndex == -1) {
            type = contentType;
        } else {
            type = contentType.substring(0, firstIndex);
        }

        return type.trim().toLowerCase(Locale.ENGLISH);
    }
}
