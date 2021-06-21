/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.version.Version;

public class Headers {
    /**
     *  This constant is the Message(Map) key for the HttpURLConnection that
     *  is used to get the response.
     */
    public static final String KEY_HTTP_CONNECTION = "http.connection";
    /**
     * Each header value is added as a separate HTTP header, example, given A header with 'a' and 'b'
     * values, two A headers will be added as opposed to a single A header with the "a,b" value.
     */
    public static final String ADD_HEADERS_PROPERTY = "org.apache.cxf.http.add-headers";

    public static final String PROTOCOL_HEADERS_CONTENT_TYPE = Message.CONTENT_TYPE.toLowerCase();
    public static final String HTTP_HEADERS_SETCOOKIE = "Set-Cookie";
    public static final String HTTP_HEADERS_LINK = "Link";
    public static final String EMPTY_REQUEST_PROPERTY = "org.apache.cxf.empty.request";
    private static final String SET_EMPTY_REQUEST_CT_PROPERTY = "set.content.type.for.empty.request";
    private static final TimeZone TIME_ZONE_GMT = TimeZone.getTimeZone("GMT");
    private static final Logger LOG = LogUtils.getL7dLogger(Headers.class);

    private static final List<String> SENSITIVE_HEADERS = Arrays.asList("Authorization", "Proxy-Authorization");
    private static final List<Object> SENSITIVE_HEADER_MARKER = Arrays.asList("***");
    private static final String ALLOW_LOGGING_SENSITIVE_HEADERS = "allow.logging.sensitive.headers";
    private static final String USER_AGENT = initUserAgent();

    private final Message message;
    private final Map<String, List<String>> headers;

    public Headers(Message message) {
        this.message = message;
        this.headers = getSetProtocolHeaders(message);
    }
    public Headers() {
        this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.message = null;
    }

    public static String getUserAgent() {
        return USER_AGENT;
    }

    private static String initUserAgent() {
        String name = Version.getName();
        if ("Apache CXF".equals(name)) {
            name = "Apache-CXF";
        }
        String version = Version.getCurrentVersion();
        return name + "/" + version;
    }

    /**
     * Returns a traceable string representation of the passed-in headers map.
     * The value for any keys in the map that are in the <code>SENSITIVE_HEADERS</code>
     * array will be filtered out of the returned string.
     * Note that this method is expensive as it will copy the map (except for the
     * filtered keys), so it should be used sparingly - i.e. only when debug is
     * enabled.
     */
    static String toString(Map<String, List<Object>> headers, boolean logSensitiveHeaders) {
        Map<String, List<Object>> filteredHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        filteredHeaders.putAll(headers);
        if (!logSensitiveHeaders) {
            for (String filteredKey : SENSITIVE_HEADERS) {
                filteredHeaders.put(filteredKey, SENSITIVE_HEADER_MARKER);
            }
        }
        return filteredHeaders.toString();
    }

    public Map<String, List<String>> headerMap() {
        return headers;
    }


    /**
     * Write cookie header from given session cookies
     *
     * @param sessionCookies
     */
    public void writeSessionCookies(Map<String, Cookie> sessionCookies) {
        List<String> cookies = null;
        for (String s : headers.keySet()) {
            if (HttpHeaderHelper.COOKIE.equalsIgnoreCase(s)) {
                cookies = headers.remove(s);
                break;
            }
        }
        if (cookies == null) {
            cookies = new ArrayList<>();
        } else {
            cookies = new ArrayList<>(cookies);
        }
        headers.put(HttpHeaderHelper.COOKIE, cookies);
        for (Cookie c : sessionCookies.values()) {
            cookies.add(c.requestCookieHeader());
        }
    }

    /**
     * This call places HTTP Header strings into the headers that are relevant
     * to the ClientPolicy that is set on this conduit by configuration.
     *
     * REVISIT: A cookie is set statically from configuration?
     */
    void setFromClientPolicy(HTTPClientPolicy policy) {
        if (policy == null) {
            return;
        }
        if (policy.isSetCacheControl()) {
            headers.put("Cache-Control",
                    createMutableList(policy.getCacheControl()));
        }
        if (policy.isSetHost()) {
            headers.put("Host",
                    createMutableList(policy.getHost()));
        }
        if (policy.isSetConnection()) {
            headers.put("Connection",
                    createMutableList(policy.getConnection().value()));
        }
        if (policy.isSetAccept()) {
            headers.put("Accept",
                    createMutableList(policy.getAccept()));
        } else if (!headers.containsKey("Accept")) {
            headers.put("Accept", createMutableList("*/*"));
        }
        if (policy.isSetAcceptEncoding()) {
            headers.put("Accept-Encoding",
                    createMutableList(policy.getAcceptEncoding()));
        }
        if (policy.isSetAcceptLanguage()) {
            headers.put("Accept-Language",
                    createMutableList(policy.getAcceptLanguage()));
        }
        if (policy.isSetContentType()) {
            message.put(Message.CONTENT_TYPE, policy.getContentType());
        }
        if (policy.isSetCookie()) {
            headers.put("Cookie",
                    createMutableList(policy.getCookie()));
        }
        if (policy.isSetBrowserType()) {
            headers.put("User-Agent",
                    createMutableList(policy.getBrowserType()));
        }
        if (policy.isSetReferer()) {
            headers.put("Referer",
                    createMutableList(policy.getReferer()));
        }
    }

    void setFromServerPolicy(HTTPServerPolicy policy) {
        if (policy.isSetCacheControl()) {
            headers.put("Cache-Control",
                        createMutableList(policy.getCacheControl()));
        }
        if (policy.isSetContentLocation()) {
            headers.put("Content-Location",
                        createMutableList(policy.getContentLocation()));
        }
        if (policy.isSetContentEncoding()) {
            headers.put("Content-Encoding",
                        createMutableList(policy.getContentEncoding()));
        }
        if (policy.isSetContentType()) {
            headers.put(HttpHeaderHelper.CONTENT_TYPE,
                        createMutableList(policy.getContentType()));
        }
        if (policy.isSetServerType()) {
            headers.put("Server",
                        createMutableList(policy.getServerType()));
        }
        if (policy.isSetHonorKeepAlive() && !policy.isHonorKeepAlive()) {
            headers.put("Connection",
                        createMutableList("close"));
        } else if (policy.isSetKeepAliveParameters()) {
            headers.put("Keep-Alive", createMutableList(policy.getKeepAliveParameters()));
        }


    /*
     * TODO - hook up these policies
    <xs:attribute name="SuppressClientSendErrors" type="xs:boolean" use="optional" default="false">
    <xs:attribute name="SuppressClientReceiveErrors" type="xs:boolean" use="optional" default="false">
    */
    }

    public void removeAuthorizationHeaders() {
        headers.remove("Authorization");
        headers.remove("Proxy-Authorization");
    }

    public void setAuthorization(String authorization) {
        headers.put("Authorization",
                createMutableList(authorization));
    }

    public void setProxyAuthorization(String authorization) {
        headers.put("Proxy-Authorization",
                createMutableList(authorization));
    }


    /**
     * While extracting the Message.PROTOCOL_HEADERS property from the Message,
     * this call ensures that the Message.PROTOCOL_HEADERS property is
     * set on the Message. If it is not set, an empty map is placed there, and
     * then returned.
     *
     * @param message The outbound message
     * @return The PROTOCOL_HEADERS map
     */
    public static Map<String, List<String>> getSetProtocolHeaders(final Message message) {
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        //Liberty code change start
        if (null == headers) {
            headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            message.put(Message.PROTOCOL_HEADERS, headers);
        } else if (headers instanceof HashMap) {
            Map<String, List<String>> headers2
                = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headers2.putAll(headers);
            message.put(Message.PROTOCOL_HEADERS, headers2);
            headers = headers2;
        }
        //Liberty code change end
        return headers;
    }

    public void readFromConnection(HttpURLConnection connection) {
        Map<String, List<String>> origHeaders = connection.getHeaderFields();
        headers.clear();
        for (Entry<String, List<String>> entry : origHeaders.entrySet()) {
            if (entry.getKey() != null) {
                String key = HttpHeaderHelper.getHeaderKey(entry.getKey());
                List<String> old = headers.get(key);
                if (old != null) {
                    List<String> nl = new ArrayList<>(old.size() + entry.getValue().size()); 
                    nl.addAll(old);
                    nl.addAll(entry.getValue());
                    headers.put(key, nl);
                } else {
                    headers.put(key, entry.getValue());
                }
            }
        }
    }

    private static List<String> createMutableList(String val) {
        return new ArrayList<>(Arrays.asList(val));
    }

    /**
     * This procedure logs the PROTOCOL_HEADERS from the
     * Message at the specified logging level.
     *
     * @param logger     The Logger to log to.
     * @param level   The Logging Level.
     * @param headers The Message protocol headers.
     */
    static void logProtocolHeaders(Logger logger, Level level,
                                   Map<String, List<Object>> headersMap,
                                   boolean logSensitiveHeaders) {
        if (logger.isLoggable(level)) {
            for (Map.Entry<String, List<Object>> entry : headersMap.entrySet()) {
                String key = entry.getKey();
                boolean sensitive = !logSensitiveHeaders && SENSITIVE_HEADERS.contains(key);
                List<Object> headerList = sensitive ? SENSITIVE_HEADER_MARKER : entry.getValue();
                for (Object value : headerList) {
                    logger.log(level, key + ": "
                        + (value == null ? "<null>" : value.toString()));
                }
            }
        }
    }

    /**
     * Set content type and protocol headers (Message.PROTOCOL_HEADERS) headers into the URL
     * connection.
     * Note, this does not mean they immediately get written to the output
     * stream or the wire. They just just get set on the HTTP request.
     *
     * @param connection
     * @throws IOException
     */
    public void setProtocolHeadersInConnection(HttpURLConnection connection) throws IOException {
        // If no Content-Type is set for empty requests then HttpUrlConnection:
        // - sets a form Content-Type for empty POST
        // - replaces custom Accept value with */* if HTTP proxy is used
        boolean contentTypeSet = headers.containsKey(Message.CONTENT_TYPE);
        if (!contentTypeSet) {
            // if CT is not set then assume it has to be set by default
            boolean dropContentType = false;
            boolean getRequest = "GET".equals(message.get(Message.HTTP_REQUEST_METHOD));
            boolean emptyRequest = getRequest || PropertyUtils.isTrue(message.get(EMPTY_REQUEST_PROPERTY));
            // If it is an empty request (without a request body) then check further if CT still needs be set
            if (emptyRequest) {
                Object setCtForEmptyRequestProp = message.getContextualProperty(SET_EMPTY_REQUEST_CT_PROPERTY);
                if (setCtForEmptyRequestProp != null) {
                    // If SET_EMPTY_REQUEST_CT_PROPERTY is set then do as a user prefers.
                    // CT will be dropped if setting CT for empty requests was explicitly disabled
                    dropContentType = PropertyUtils.isFalse(setCtForEmptyRequestProp);
                } else if (getRequest) {
                    // otherwise if it is GET then just drop it
                    dropContentType = true;
                }
            }
            if (!dropContentType) {
                String ct = emptyRequest && !contentTypeSet ? "*/*" : determineContentType();
                connection.setRequestProperty(HttpHeaderHelper.CONTENT_TYPE, ct);
            }
        } else {
            connection.setRequestProperty(HttpHeaderHelper.CONTENT_TYPE, determineContentType());
        }

        transferProtocolHeadersToURLConnection(connection);

        Map<String, List<Object>> theHeaders = CastUtils.cast(headers);
        logProtocolHeaders(LOG, Level.FINE, theHeaders, logSensitiveHeaders());
    }

    public String determineContentType() {
        String ct = null;
        List<Object> ctList = CastUtils.cast(headers.get(Message.CONTENT_TYPE));
        if (ctList != null && ctList.size() == 1 && ctList.get(0) != null) {
            ct = ctList.get(0).toString();
        } else {
            ct = (String)message.get(Message.CONTENT_TYPE);
        }

        String enc = (String)message.get(Message.ENCODING);

        if (null != ct) {
            if (enc != null
                && ct.indexOf("charset=") == -1
                && !ct.toLowerCase().contains("multipart/related")) {
                ct = ct + "; charset=" + enc;
            }
        } else if (enc != null) {
            ct = "text/xml; charset=" + enc;
        } else {
            ct = "text/xml";
        }
        return ct;
    }

    /**
     * This procedure sets the URLConnection request properties
     * from the PROTOCOL_HEADERS in the message.
     */
    private void transferProtocolHeadersToURLConnection(URLConnection connection) {
        boolean addHeaders = MessageUtils.getContextualBoolean(message, ADD_HEADERS_PROPERTY, false);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String header = entry.getKey();
            if (HttpHeaderHelper.CONTENT_TYPE.equalsIgnoreCase(header)) {
                continue;
            }

            List<String> headerList = entry.getValue();
            if (addHeaders || HttpHeaderHelper.COOKIE.equalsIgnoreCase(header)) {
                headerList.forEach(s -> connection.addRequestProperty(header, s));
            } else {
                connection.setRequestProperty(header, String.join(",", headerList));
            }
        }
        // make sure we don't add more than one User-Agent header
        if (connection.getRequestProperty("User-Agent") == null) {
            connection.addRequestProperty("User-Agent", USER_AGENT);
        }
    }

    /**
     * Copy the request headers into the message.
     *
     * @param message the current message
     * @param headers the current set of headers
     */
    protected void copyFromRequest(HttpServletRequest req) {
        //TODO how to deal with the fields
        for (Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements();) {
            String fname = e.nextElement();
            String mappedName = HttpHeaderHelper.getHeaderKey(fname);
            List<String> values = headers.get(mappedName);
            if (values == null) {
                values = new ArrayList<>();
                headers.put(mappedName, values);
            }
            for (Enumeration<String> e2 = req.getHeaders(fname); e2.hasMoreElements();) {
                String val = e2.nextElement();
                if ("Accept".equals(mappedName) && values.size() > 0) {
                    //ensure we collapse Accept into first line
                    String firstAccept = values.get(0);
                    firstAccept = firstAccept + ", " + val;
                    values.set(0, firstAccept);
                }
                values.add(val);
            }
        }
        if (!headers.containsKey(Message.CONTENT_TYPE)) {
            headers.put(Message.CONTENT_TYPE, Collections.singletonList(req.getContentType()));
        }
        if (LOG.isLoggable(Level.FINE)) {
            Map<String, List<Object>> theHeaders = CastUtils.cast(headers);
            LOG.log(Level.FINE, "Request Headers: " + toString(theHeaders,
                                                               logSensitiveHeaders()));
        }
    }

    private boolean logSensitiveHeaders() {
        // Not allowed by default
        return PropertyUtils.isTrue(message.getContextualProperty(ALLOW_LOGGING_SENSITIVE_HEADERS));
    }

    private String getContentTypeFromMessage() {
        final String ct = (String)message.get(Message.CONTENT_TYPE);
        final String enc = (String)message.get(Message.ENCODING);

        if (null != ct
            && null != enc
            && ct.indexOf("charset=") == -1
            && !ct.toLowerCase().contains("multipart/related")) {
            return ct + "; charset=" + enc;
        }
        return ct;
    }

    // Assumes that response body is not available only
    // if Content-Length is available and set to 0
    private boolean isResponseBodyAvailable() {
        List<String> ctLen = headers.get("Content-Length");
        if (ctLen == null || ctLen.size() != 1) {
            return true;
        }
        try {
            if (Integer.parseInt(ctLen.get(0)) == 0) {
                return false;
            }
        } catch (NumberFormatException ex) {
            // ignore
        }
        return true;
    }

    private boolean isSingleHeader(String header) {
        return HTTP_HEADERS_SETCOOKIE.equalsIgnoreCase(header) || HTTP_HEADERS_LINK.equalsIgnoreCase(header);
    }
    
    /**
     * Copy the response headers into the response.
     *
     * @param message the current message
     * @param headers the current set of headers
     */
    protected void copyToResponse(HttpServletResponse response) {
        String contentType = getContentTypeFromMessage();

        if (!headers.containsKey(Message.CONTENT_TYPE) && contentType != null
            && isResponseBodyAvailable()) {
            response.setContentType(contentType);
        }

        boolean addHeaders = MessageUtils.getContextualBoolean(message, ADD_HEADERS_PROPERTY, false);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String header = entry.getKey();
            List<?> headerList = entry.getValue();

            if (addHeaders || isSingleHeader(header)) {
                for (int i = 0; i < headerList.size(); i++) {
                    Object headerObject = headerList.get(i);
                    if (headerObject != null) {
                        response.addHeader(header, headerObjectToString(headerObject));
                    }
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < headerList.size(); i++) {
                    Object headerObject = headerList.get(i);
                    if (headerObject != null) {
                        sb.append(headerObjectToString(headerObject));
                    }

                    if (i + 1 < headerList.size()) {
                        sb.append(',');
                    }
                }
                response.setHeader(header, sb.toString());
            }
        }
    }

    private String headerObjectToString(Object headerObject) {
        if (headerObject.getClass() == String.class) {
            // Most likely
            return headerObject.toString();
        }
        String headerString;
        if (headerObject instanceof Date) {
            headerString = toHttpDate((Date)headerObject);
        } else if (headerObject instanceof Locale) {
            headerString = toHttpLanguage((Locale)headerObject);
        } else {
            headerString = headerObject.toString();
        }
        return headerString;
    }

    void removeContentType() {
        headers.remove(PROTOCOL_HEADERS_CONTENT_TYPE);
    }

    public String getAuthorization() {
        List<String> authorizationLines = headers.get("Authorization");
        if (authorizationLines != null && !authorizationLines.isEmpty()) {
            return authorizationLines.get(0);
        }
        return null;
    }

    public static SimpleDateFormat getHttpDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        dateFormat.setTimeZone(TIME_ZONE_GMT);
        return dateFormat;
    }

    public static String toHttpDate(Date date) {
        SimpleDateFormat format = getHttpDateFormat();
        return format.format(date);
    }

    public static String toHttpLanguage(Locale locale) {
        return locale.toString().replace('_', '-');
    }
}
