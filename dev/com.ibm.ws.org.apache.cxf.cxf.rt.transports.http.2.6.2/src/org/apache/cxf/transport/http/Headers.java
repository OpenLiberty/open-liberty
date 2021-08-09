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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.common.logging.LogUtils;
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
    public static final String PROTOCOL_HEADERS_CONTENT_TYPE = Message.CONTENT_TYPE.toLowerCase();
    private static final String HTTP_HEADERS_SETCOOKIE = "Set-Cookie";
    private static final String ADD_HEADERS_PROPERTY = "org.apache.cxf.http.add-headers";             
    
    private static final Logger LOG = LogUtils.getL7dLogger(Headers.class);
    private final Message message;
    private final Map<String, List<String>> headers;

    public Headers(Message message) {
        this.message = message;
        this.headers = getSetProtocolHeaders(message);
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
            cookies = new ArrayList<String>();
        } else {
            cookies = new ArrayList<String>(cookies);
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
            headers.put("BrowserType",
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
            headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER); 
            message.put(Message.PROTOCOL_HEADERS, headers);
        } else if (headers instanceof HashMap) {
            Map<String, List<String>> headers2
                = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
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
        for (String key : connection.getHeaderFields().keySet()) {
            if (key != null) {
                headers.put(HttpHeaderHelper.getHeaderKey(key), 
                    origHeaders.get(key));
            }
        }
    }

    private static List<String> createMutableList(String val) {
        return new ArrayList<String>(Arrays.asList(new String[] {val}));
    }
    
    /**
     * This procedure logs the PROTOCOL_HEADERS from the 
     * Message at the specified logging level.
     * 
     * @param level   The Logging Level.
     * @param headers The Message protocol headers.
     */
    void logProtocolHeaders(Level level) {
        for (String header : headers.keySet()) {
            List<String> headerList = headers.get(header);
            for (String value : headerList) {
                LOG.log(level, header + ": " + value);
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
        String ct = determineContentType();
        connection.setRequestProperty(HttpHeaderHelper.CONTENT_TYPE, ct);
        transferProtocolHeadersToURLConnection(connection);
        logProtocolHeaders(Level.FINE);
    }

    private String determineContentType() {
        String ct  = (String)message.get(Message.CONTENT_TYPE);
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
        boolean addHeaders = MessageUtils.isTrue(
                message.getContextualProperty(ADD_HEADERS_PROPERTY));
        for (String header : headers.keySet()) {
            List<String> headerList = headers.get(header);
            if (HttpHeaderHelper.CONTENT_TYPE.equalsIgnoreCase(header)) {
                continue;
            }
            if (addHeaders || HttpHeaderHelper.COOKIE.equalsIgnoreCase(header)) {
                for (String s : headerList) {
                    connection.addRequestProperty(HttpHeaderHelper.COOKIE, s);
                }
            } else {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < headerList.size(); i++) {
                    b.append(headerList.get(i));
                    if (i + 1 < headerList.size()) {
                        b.append(',');
                    }
                }
                connection.setRequestProperty(header, b.toString());
            }
        }
        // make sure we don't add more than one User-Agent header
        if (connection.getRequestProperty("User-Agent") == null) {
            connection.addRequestProperty("User-Agent", Version.getCompleteVersionString());
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
            List<String> values;
            if (headers.containsKey(mappedName)) {
                values = headers.get(mappedName);
            } else {
                values = new ArrayList<String>();
                headers.put(mappedName, values);
            }
            for (Enumeration<String> e2 = req.getHeaders(fname); e2.hasMoreElements();) {
                String val = e2.nextElement();
                values.add(val);
            }
        }
        if (!headers.containsKey(Message.CONTENT_TYPE)) {
            headers.put(Message.CONTENT_TYPE, Collections.singletonList(req.getContentType()));
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Request Headers: " + headers.toString());
        }
    }

    private String getContentTypeFromMessage() {
        final String ct  = (String)message.get(Message.CONTENT_TYPE);
        final String enc = (String)message.get(Message.ENCODING);
        
        if (null != ct 
            && null != enc
            && ct.indexOf("charset=") == -1
            && !ct.toLowerCase().contains("multipart/related")) {
            return ct + "; charset=" + enc;
        } else {
            return ct;
        }
    }
    
    // Assumes that response body is not available only
    // if Content-Length is available and set to 0
    private boolean isResponseBodyAvailable() {
        List<String> ctLen = headers.get("Content-Length");
        if (ctLen == null || ctLen.size() != 1) {
            return true;
        }
        try {
            //Liberty code change start
            if (Integer.parseInt(ctLen.get(0)) == 0) {
                return false;
            }
            //Liberty code change end
        } catch (NumberFormatException e) {
            // ignore
        }
        return true;
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

        boolean addHeaders = MessageUtils.isTrue(
                message.getContextualProperty(ADD_HEADERS_PROPERTY));
        for (Iterator<?> iter = headers.keySet().iterator(); iter.hasNext();) {
            String header = (String)iter.next();
            List<?> headerList = headers.get(header);
            
            if (addHeaders || HTTP_HEADERS_SETCOOKIE.equals(header)) {
                for (int i = 0; i < headerList.size(); i++) {
                    response.addHeader(header, headerList.get(i).toString());
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < headerList.size(); i++) {
                    sb.append(headerList.get(i));
                    if (i + 1 < headerList.size()) {
                        sb.append(',');
                    }
                }
                response.addHeader(header, sb.toString());
            }

            
        }
    }
    
    void removeContentType() {
        if (headers.containsKey(PROTOCOL_HEADERS_CONTENT_TYPE)) {
            headers.remove(PROTOCOL_HEADERS_CONTENT_TYPE);
        }
    }

    public String getAuthorization() {
        if (headers.containsKey("Authorization")) {
            List<String> authorizationLines = headers.get("Authorization"); 
            return authorizationLines.get(0);
        } else {
            return null;
        }
    }

}
