/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.cors;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.cors.config.CorsConfig;

@Component(service = { CorsHelper.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class CorsHelper {
    private static final TraceComponent tc = Tr.register(CorsHelper.class);

    // --------------------------------------------------------
    // --- Constants from http://www.w3.org/TR/cors/#syntax ---

    // -- Request
    /** Request from preFlight(OPTIONS) or cross-origin */
    private static final String REQUEST_HEADER_ORIGIN = "Origin";

    /** Request from preFlight(OPTIONS) */
    private static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

    /** Request from preFlight(OPTIONS) */
    private static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    // -- Response
    /** Response to preFlight(OPTIONS) or cross-origin. Values can be Origin, * or "null" */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /** Response to preFlight(OPTIONS) or cross-origin. Value can be true */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    /**
     * Optional. Response to preFlight(OPTIONS) or cross-origin. Allowlist of custom headers that browsers are allowed to access.
     */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    /** Optional. Response to preFlight(OPTIONS). Indicates how long the results of a preFlight request can be cached. */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    /** Response to preFlight(OPTIONS). Which methods can be used in the actual request. */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    /** Response to preFlight(OPTIONS). Which headers can be used in the actual request. */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    // --- http://www.w3.org/TR/2014/REC-cors-20140116/#terminology ---
    private static final List<String> SIMPLE_REQUEST_HEADER_CONTENT_TYPE_VALUES = Arrays.asList("application/x-www-form-urlencoded", "multipart/form-data", "text/plain");

    /*
     * CORS Spec doesn't say anything about the list of approved. Therefore, comment out this line.
     * private static final List<String> COMPLEX_HTTP_METHOD_VALUES = Arrays.asList("PUT", "DELETE", "CONNECT", "TRACE");
     */

    private enum CorsRequestType {
        SIMPLE,
        PRE_FLIGHT,
        ACTUAL,
        OTHER
    }

    // Holds a list of CORS configuration elements
    List<CorsConfig> configurations = new CopyOnWriteArrayList<CorsConfig>();

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Reference(service = CorsConfig.class, name = "corsConfig", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    protected void setCorsConfig(CorsConfig corsConfig) {
        configurations.add(corsConfig);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Added CorsConfig : " + corsConfig);
        }
    }

    protected void unsetCorsConfig(CorsConfig corsConfig) {
        configurations.remove(corsConfig);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Removed CorsConfig : " + corsConfig);
        }
    }

    public boolean handleCorsRequest(HttpServletRequest request, HttpServletResponse response) {
        boolean continueHandlingRequest = false;

        // 6.1.1 and 6.2.1: If the Origin header is not present terminate this set of steps. The request is outside the scope of the CORS specification.
        if (request.getHeader(REQUEST_HEADER_ORIGIN) != null) {
            logCorsRequestInfo(request);

            CorsRequestType type = getRequestType(request);
            if (type == CorsRequestType.PRE_FLIGHT) {
                continueHandlingRequest = handlePreflightRequest(request, response);
            } else if (type == CorsRequestType.SIMPLE || type == CorsRequestType.ACTUAL) {
                continueHandlingRequest = handleSimpleCrossOriginRequest(request, response);
            }

            logCorsResponseInfo(response);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Request is fully handled and processed? " + continueHandlingRequest);
        }

        return continueHandlingRequest;
    }

    private boolean handleSimpleCrossOriginRequest(HttpServletRequest request, HttpServletResponse response) {
        String requestOrigin = request.getHeader(REQUEST_HEADER_ORIGIN);

        // 6.1.1: If the Origin header is not present terminate this set of steps. The request is outside the scope of the CORS specification.
        if (requestOrigin == null) {
            return false;
        }

        // 6.1.2: If the value of the Origin header is not a case-sensitive match for any of the values in list of origins,
        // do not set any additional headers and terminate this set of steps.
        CorsConfig config = matchCorsConfig(request.getRequestURI());
        if (config == null) {
            return false;
        }

        if (!isMatch(config, requestOrigin)) {
            return false;
        }

        // 6.1.3: If the resource supports credentials add a single Access-Control-Allow-Origin header,
        // with the value of the Origin header as value, and add a single Access-Control-Allow-Credentials
        // header with the case-sensitive string "true" as value.
        // Otherwise, add a single Access-Control-Allow-Origin header, with either the value of the Origin header or the string "*" as value.
        boolean allowCredentials = config.getAllowCredentials();
        if (allowCredentials) {
            response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        if (isWildcardPath(config.getAllowedOrigins()) && !allowCredentials) {
            // 6.1.3: The string "*" cannot be used for a resource that supports credentials.
            response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        } else {
            response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, filterHttpHeader(requestOrigin));
        }

        // 6.1.4: If the list of exposed headers is not empty add one or more Access-Control-Expose-Headers headers,
        // with as values the header field names given in the list of exposed headers.
        String exposeHeaders = config.getExposedHeaders();
        if (exposeHeaders != null) {
            response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders);
        }

        return false;
    }

    private boolean handlePreflightRequest(HttpServletRequest request, HttpServletResponse response) {
        String requestOrigin = request.getHeader(REQUEST_HEADER_ORIGIN);

        // 6.2.1: If the Origin header is not present terminate this set of steps. The request is outside the scope of the CORS specification.
        if (requestOrigin == null) {
            return false;
        }

        // 6.2.2: If the value of the Origin header is not a case-sensitive match for any of the values in list of origins,
        // do not set any additional headers and terminate this set of steps.
        CorsConfig config = matchCorsConfig(request.getRequestURI());
        if (config == null) {
            return false;
        }

        if (!isMatch(config, requestOrigin)) {
            return false;
        }

        // 6.2.3 Let method be the value as result of parsing the Access-Control-Request-Method header.
        // If there is no Access-Control-Request-Method header or if parsing failed, do not set any additional
        // headers and terminate this set of steps. The request is outside the scope of this specification.
        String accessControlRequestMethod = request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
        if (accessControlRequestMethod == null || accessControlRequestMethod.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Value of the request header 'Access-Control-Request-Method' is '" + String.valueOf(accessControlRequestMethod) + "'.");
            }

            return false;
        } else {
            accessControlRequestMethod = accessControlRequestMethod.trim();
        }

        // 6.2.4: Let header field-names be the values as result of parsing the Access-Control-Request-Headers headers.
        // If there are no Access-Control-Request-Headers headers let header field-names be the empty list.
        // If parsing failed do not set any additional headers and terminate this set of steps. The request is outside the scope of this specification.
        String accessControlRequestHeadersHeader = request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
        List<String> accessControlRequestHeaders;
        if (accessControlRequestHeadersHeader == null || accessControlRequestHeadersHeader.isEmpty()) {
            accessControlRequestHeaders = Collections.emptyList();
        } else {
            accessControlRequestHeaders = Arrays.asList(accessControlRequestHeadersHeader.trim().split("\\s*,\\s*"));
        }

        // 6.2.5: If method is not a case-sensitive match for any of the values in list of methods do not set any additional headers and terminate this set of steps.
        final List<String> allowedMethods = config.getAllowedMethods();
        if (!allowedMethods.contains(accessControlRequestMethod)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this,
                         tc,
                         "Access-Control-Request-Method '" + accessControlRequestMethod + "' is not included in the list of allowed methods: "
                             + Arrays.toString(allowedMethods.toArray()));
            }

            return false;
        }

        // 6.2.6: If any of the header field-names is not a ASCII case-insensitive match for any of the values in
        // list of headers do not set any additional headers and terminate this set of steps.
        final List<String> allowedHeaders = config.getAllowedHeaders();
        if (!accessControlRequestHeaders.isEmpty() && !isWildcardHeader(allowedHeaders)) {
            Set<String> allowedHeadersSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            Set<String> accessControlRequestHeaderSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

            allowedHeadersSet.addAll(allowedHeaders);
            accessControlRequestHeaderSet.addAll(accessControlRequestHeaders);
            if (!allowedHeadersSet.containsAll(accessControlRequestHeaderSet)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Access-Control-Request-Method '" + Arrays.toString(accessControlRequestHeaders.toArray())
                                       + "' is not included in the list of allowed headers: " + Arrays.toString(allowedHeaders.toArray()));
                }
                return false;
            }
        }

        // 6.2.7: If the resource supports credentials add a single Access-Control-Allow-Origin header, with the value of the Origin header as value, and add a single
        // Access-Control-Allow-Credentials header with the case-sensitive string "true" as value.
        // Otherwise, add a single Access-Control-Allow-Origin header, with either the value of the Origin header or the string "*" as value.
        boolean allowCredentials = config.getAllowCredentials();
        if (allowCredentials) {
            response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        if (isWildcardPath(config.getAllowedOrigins()) && !allowCredentials) {
            // 6.2.7: The string "*" cannot be used for a resource that supports credentials.
            response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        } else {
            response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, filterHttpHeader(requestOrigin));
        }

        // 6.2.8: Optionally add a single Access-Control-Max-Age header with as value the amount of seconds the user agent is allowed to cache the result of the request.
        Long maxAge = config.getMaxAge();
        if (maxAge != null) {
            response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE, String.valueOf(maxAge));
        }

        // 6.2.9: If method is a simple method this step may be skipped.
        // Add one or more Access-Control-Allow-Methods headers consisting of (a subset of) the list of methods.
        response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS, join(", ", allowedMethods));

        // 6.2.10: If each of the header field-names is a simple header and none is Content-Type, this step may be skipped.
        // Add one or more Access-Control-Allow-Headers headers consisting of (a subset of) the list of headers.
        if (isWildcardHeader(allowedHeaders)) {
            // If the value of allowed headers is wildcard, then set the value of request headers (if not empty), because
            // some browsers don't accept wildcard value for allowed headers.
            if (!accessControlRequestHeaders.isEmpty()) {
                response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS, join(", ", accessControlRequestHeaders));
            }
        } else if (!allowedHeaders.isEmpty()) {
            response.setHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS, join(", ", allowedHeaders));
        }

        return true;
    }

    private CorsRequestType getRequestType(HttpServletRequest request) {
        CorsRequestType type = CorsRequestType.OTHER;

        String origin = request.getHeader(REQUEST_HEADER_ORIGIN);
        if (origin != null && !origin.isEmpty()) {
            String requestMethod = request.getMethod();
            if ("OPTIONS".equals(requestMethod)) {
                // 7.1.5 Cross-Origin Request with Preflight: preflight requests will always include an Access-Control-Request-Method header.
                String accessControlRequestMethod = request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                if (accessControlRequestMethod != null && !accessControlRequestMethod.isEmpty()) {
                    type = CorsRequestType.PRE_FLIGHT;
                } else if (accessControlRequestMethod == null) {
                    type = CorsRequestType.ACTUAL;
                }

            } else if ("GET".equals(requestMethod) || "HEAD".equals(requestMethod)) {
                type = CorsRequestType.SIMPLE;
            } else if ("POST".equals(requestMethod)) {
                if (SIMPLE_REQUEST_HEADER_CONTENT_TYPE_VALUES.contains(getType(request.getContentType()))) {
                    type = CorsRequestType.SIMPLE;
                } else {
                    type = CorsRequestType.ACTUAL;
                }
            } else /* if (COMPLEX_HTTP_METHOD_VALUES.contains(requestMethod)) */ {
                // CORS spec does not mention anything about the list of approved HTTP methods.
                // Therefore, this ocndition needs to be removed.
                type = CorsRequestType.ACTUAL;
            }
        }

        return type;
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

    private CorsConfig matchCorsConfig(String path) {
        CorsConfig matchingConfig = null;
        if (!configurations.isEmpty()) {
            int length = -1;
            Iterator<CorsConfig> iter = configurations.iterator();
            while (iter.hasNext()) {
                CorsConfig config = iter.next();
                String domain = config.getDomain();
                if (path.startsWith(domain) && (domain.length() > length)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Found " + (matchingConfig == null ? "first" : "a better") + " match. path=" + path + " : corsConfig=" + config);
                    }
                    length = domain.length();
                    matchingConfig = config;
                }
            }
        }

        if (matchingConfig == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Found no match for path " + path);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Found a match. path=" + path + " : matchingConfig=" + matchingConfig);
            }
        }

        return matchingConfig;
    }

    private static boolean isWildcardHeader(List<String> allowedHeaders) {
        return !allowedHeaders.isEmpty() && "*".equals(allowedHeaders.get(0));
    }

    // Filtering incoming header to avoid http splitting attacks
    private static String filterHttpHeader(String original) {
        return original.replaceAll("(\\n|\\r|\\u0085|\\u2028)", "");
    }

    private static boolean isMatch(final CorsConfig config, String path) {
        if (config == null) {
            return false;
        }

        List<String> allowedOrigins = config.getAllowedOrigins();

        // An empty allowedOrigin means we don't want to support CORS for this URL
        if (allowedOrigins.get(0).equals("null")) {
            return false;
        }

        // A wildcard means any origin is allowed.
        if (isWildcardPath(allowedOrigins)) {
            return true;
        }

        // Search for a match.
        for (String origin : allowedOrigins) {
            if (origin.equals(path)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isWildcardPath(List<String> allowedOrigins) {
        return "*".equals(allowedOrigins.get(0));
    }

    public boolean isCorsSupportEnabled() {
        return !configurations.isEmpty();
    }

    private static String join(final String delimiter, final List<String> elements) {
        if (elements == null || delimiter == null) {
            throw new NullPointerException();
        }

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String element : elements) {
            if (!isFirst) {
                sb.append(delimiter);
            } else {
                isFirst = false;
            }

            if (element != null) {
                sb.append(element);
            }
        }

        return sb.toString();
    }

    private void logCorsRequestInfo(HttpServletRequest request) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Request :: Method: " + request.getMethod() +
                               " | Origin: " + String.valueOf(request.getHeader(REQUEST_HEADER_ORIGIN)) +
                               " | Request Method: " + String.valueOf(request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD)) +
                               " | Request Headers: " + String.valueOf(request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS)));
        }
    }

    private void logCorsResponseInfo(HttpServletResponse response) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Respond :: Allow Origin: " + String.valueOf(response.getHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)) +
                               " | Allow Credential: " + String.valueOf(response.getHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS)) +
                               " | Expose Headers: " + String.valueOf(response.getHeader(RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS)) +
                               " | Max Age: " + String.valueOf(response.getHeader(RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE)) +
                               " | Allow Methods: " + String.valueOf(response.getHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS)) +
                               " | Allow Headers :" + String.valueOf(response.getHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS)));
        }
    }
}
