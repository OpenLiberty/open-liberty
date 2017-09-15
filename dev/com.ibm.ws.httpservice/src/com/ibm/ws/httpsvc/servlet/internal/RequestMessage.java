/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.httpsvc.servlet.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.felix.http.base.internal.dispatch.FilterPipeline;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.httpsvc.session.internal.SessionInfo;
import com.ibm.ws.httpsvc.session.internal.SessionManager;
import com.ibm.wsspi.http.EncodingUtils;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpRequest;
import com.ibm.wsspi.http.SSLContext;

/**
 * HTTP bundle's implementation of a servlet request message.
 */
public class RequestMessage implements HttpServletRequest {
    /** Debug variable */
    private static final TraceComponent tc = Tr.register(RequestMessage.class);

    // servlet spec has special case attribute names used in getAttribute

    private static final String PEER_CERTS = "javax.net.ssl.peer_certificates";
    private static final String SERVLET_CERTS = "javax.servlet.request.X509Certificate";
    private static final String CIPHER_SUITE = "javax.net.ssl.cipher_suite";
    private static final String SSL_SESSION = "javax.net.ssl.session";

    /** Wrapped connection object */
    private HttpInboundConnection connection = null;
    /** Wrapped HTTP request message */
    protected HttpRequest request = null;
    /** URI stripped of any session info */
    private String strippedURI = null;
    /** Encoding used for the possible incoming request body */
    private String encoding = null;
    /** List of locales */
    private List<Locale> locales = null;
    /** Optional stream interface for a request body */
    protected RequestBody inStream = null;
    /** Optional reader interface for a request body */
    private BufferedReader inReader = null;
    /** Flag on whether the body stream interface is active or not */
    private boolean streamActive = false;
    /** Request attribute storage */
    private final Map<String, Object> attributes = new HashMap<String, Object>();
    /** Request query parameter map */
    private Map<String, String[]> queryParameters = null;
    /** Session information from client */
    private SessionInfo sessionData = null;
    /** Servlet context instance */
    private ServletContext srvContext = null;
    /** Serlvet path of the request URI */
    private String srvPath = null;
    /** Optional path info of the request URI */
    private String pathInfo = null;
    /** Flag on whether the path info has been extracted yet */
    private boolean pathInfoComputed = false;
    /** Necessary filter reference for forward/include handling */
    private FilterPipeline filters = null;

    /**
     * Constructor.
     * 
     * @param conn
     */
    public RequestMessage(HttpInboundConnection conn, SessionManager sessMgr) {
        init(conn, sessMgr);
    }

    /**
     * Initialize this request with the input link.
     * 
     * @param conn
     */
    public void init(HttpInboundConnection conn, SessionManager sessMgr) {
        this.connection = conn;
        this.request = conn.getRequest();
        this.sessionData = new SessionInfo(this, sessMgr);
    }

    /**
     * Clear all the temporary variables of this request.
     */
    public void clear() {
        this.attributes.clear();
        this.queryParameters = null;
        this.inReader = null;
        this.streamActive = false;
        this.inStream = null;
        this.encoding = null;
        this.sessionData = null;
        this.strippedURI = null;
        this.srvContext = null;
        this.srvPath = null;
        this.pathInfo = null;
        this.pathInfoComputed = false;
        this.filters = null;
    }

    /*
     * @see javatp.HttpServletRequest#getAuthType()
     */
    @Override
    public String getAuthType() {
        String type = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAuthType: " + type);
        }
        return type;
    }

    /*
     * @see javax.sHttpServletRequest#getContextPath()
     */
    @Override
    public String getContextPath() {
        // context-path only makes sense for full webapps
        return "";
    }

    /*
     * @see javttp.HttpServletRequest#getCookies()
     */
    @Override
    public Cookie[] getCookies() {
        List<HttpCookie> cookies = this.request.getCookies();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCookies: " + cookies.size());
        }
        if (cookies.isEmpty()) {
            return null;
        }
        Cookie[] output = new Cookie[cookies.size()];
        int i = 0;
        for (HttpCookie cookie : cookies) {
            output[i++] = new Cookie(cookie.getName(), cookie.getValue());
        }
        return output;
    }

    /*
     * @see javax.servlet.http.Httest#getDateHeader(java.lang.String)
     */
    @Override
    public long getDateHeader(String hdr) {
        try {
            String value = this.request.getHeader(hdr);
            if (null == value) {
                return -1L;
            }
            Date rc = connection.getDateFormatter().parseTime(value);
            if (null != rc) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getDateHeader: " + hdr + " " + rc.getTime());
                }
                return rc.getTime();
            }
            return -1L;
        } catch (ParseException e) {
            throw new IllegalArgumentException(hdr, e);
        }
    }

    /*
     * @see javax.servlet.httpRequest#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String hdr) {
        return this.request.getHeader(hdr);
    }

    /*
     * @see javax.sHttpServletRequest#getHeaderNames()
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = this.request.getHeaderNames();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getHeaderNames: " + names.size());
        }
        return Collections.enumeration(names);
    }

    /*
     * @see javax.servlet.http.equest#getHeaders(java.lang.String)
     */
    @Override
    public Enumeration<String> getHeaders(String hdr) {
        List<String> rc = this.request.getHeaders(hdr);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getHeaders: " + hdr + " " + rc.size());
        }
        return Collections.enumeration(rc);
    }

    /*
     * @see javax.servlet.http.Htuest#getIntHeader(java.lang.String)
     */
    @Override
    public int getIntHeader(String name) {
        int rc = -1;
        String value = this.request.getHeader(name);
        if (null != value) {
            rc = Integer.parseInt(value);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getIntHeader: " + name + " " + rc);
        }
        return rc;
    }

    /*
     * @see jahttp.HttpServletRequest#getMethod()
     */
    @Override
    public String getMethod() {
        String method = this.request.getMethod();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getMethod: " + method);
        }
        return method;
    }

    /*
     * @see javatp.HttpServletRequest#getPathInfo()
     */
    @Override
    public String getPathInfo() {
        if (!this.pathInfoComputed) {
            final String uri = getRequestURI();
            final int servletPathLength = getServletPath().length();
            if (uri.length() == servletPathLength) {
                this.pathInfo = null;
            } else {
                this.pathInfo = uri.replaceAll("[/]{2,}", "/").substring(servletPathLength);
                if ("".equals(this.pathInfo) && servletPathLength != 0) {
                    this.pathInfo = null;
                }
            }

            this.pathInfoComputed = true;
        }

        return this.pathInfo;
    }

    /*
     * @see javax.servpServletRequest#getPathTranslated()
     */
    @Override
    public String getPathTranslated() {
        final String info = getPathInfo();
        return (null == info) ? null : getServletContext().getRealPath(info);
    }

    /*
     * @see javax.sHttpServletRequest#getQueryString()
     */
    @Override
    public String getQueryString() {
        String query = this.request.getQuery();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getQueryString: " + query);
        }
        return query;
    }

    /*
     * @see javax..HttpServletRequest#getRemoteUser()
     */
    @Override
    public String getRemoteUser() {
        Principal principal = getUserPrincipal();
        String user = principal == null ? null : principal.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { // 306998.15
            Tr.debug(tc, "getRemoteUser: " + user);
        }
        return user;
    }

    /**
     * Acces the raw request URI information.
     * 
     * @return String
     */
    public String getRawRequestURI() {
        return this.request.getURI();
    }

    /*
     * @see javax..HttpServletRequest#getRequestURI()
     */
    @Override
    public String getRequestURI() {
        if (null == this.strippedURI) {
            this.strippedURI = SessionInfo.stripURL(this.request.getURI(), this.sessionData);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getRequestURI: " + this.strippedURI);
        }
        return this.strippedURI;
    }

    /*
     * @see javax..HttpServletRequest#getRequestURL()
     */
    @Override
    public StringBuffer getRequestURL() {
        // strip off any session id from the url here
        String rc = SessionInfo.stripURL(this.request.getURL(),
                                         this.sessionData);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getRequestURL: " + rc);
        }
        return new StringBuffer(rc);
    }

    /*
     * @see javax.servlet.vletRequest#getRequestedSessionId()
     */
    @Override
    public String getRequestedSessionId() {
        return this.sessionData.getID();
    }

    /**
     * Set the servlet path of this request to the input value.
     * 
     * @param path
     */
    public void setServletPath(String path) {
        this.srvPath = path;
    }

    /*
     * @see javax.sHttpServletRequest#getServletPath()
     */
    @Override
    public String getServletPath() {
        return this.srvPath;
    }

    /**
     * Access the possible session information wrapper. This will be null
     * if no session has been queried or created for this request.
     * 
     * @return SessionInfo, may be null
     */
    SessionInfo getSessionInfo() {
        return this.sessionData;
    }

    /*
     * @see javttp.HttpServletRequest#getSession()
     */
    @Override
    public HttpSession getSession() {
        return getSession(false);
    }

    /*
     * @see javax.servpServletRequest#getSession(boolean)
     */
    @Trivial
    @Override
    public HttpSession getSession(boolean create) {
        HttpSession session = this.sessionData.getSession(create);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getSession(" + create + "): " + session);
        }
        return session;
    }

    /*
     * @see javax.sertpServletRequest#getUserPrincipal()
     */
    @Override
    public Principal getUserPrincipal() {
        // TODO: this is not implemented, do we need it to be?
        return null;
    }

    /*
     * @see javax.servlet.http.Httpst#isRequestedSessionIdFromCookie()
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return this.sessionData.isFromCookie();
    }

    /*
     * @see javax.servlet.http.Hquest#isRequestedSessionIdFromURL()
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {
        return this.sessionData.isFromURL();
    }

    /**
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
     *      d - use isRequestedSessionIdFromURL
     */
    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    /*
     * @see javax.servlet.httpRequest#isRequestedSessionIdValid()
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        return this.sessionData.isValid();
    }

    /*
     * @see javax.servlet.http.Htuest#isUserInRole(java.lang.String)
     */
    @Override
    public boolean isUserInRole(String role) {
        // TODO security collaborator
        return false;
    }

    /*
     * @see javax.servleuest#getAttribute(java.lang.String)
     */
    @Override
    public Object getAttribute(String name) {
        Object value = this.attributes.get(name);
        if (null == value && null != this.connection.getSSLContext()) {
            // special case logic for SSL based connections
            if (PEER_CERTS.equals(name)
                || SERVLET_CERTS.equals(name)) {
                value = getPeerCertificates();
            } else if (CIPHER_SUITE.equals(name)) {
                value = getCipherSuite();
            } else if (SSL_SESSION.equals(name)) {
                value = this.connection.getSSLContext().getSession();
            }
        }
        return value;
    }

    /*
     * @see javax.servlet.ServletRequest#getAttributeNames()
     */
    @Trivial
    @Override
    public Enumeration<String> getAttributeNames() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Getting attribute names");
        }
        return Collections.enumeration(this.attributes.keySet());
    }

    /*
     * @see javax.servlet.ServletRequest#getCharacterEncoding()
     */
    @Trivial
    @Override
    public String getCharacterEncoding() {
        if (null != this.encoding) {
            return this.encoding;
        }

        EncodingUtils encodingUtils = connection.getEncodingUtils();

        String enc = null;
        // parse content-type to see if it's set there
        String type = this.request.getHeader("Content-Type");
        if (null != type) {
            enc = encodingUtils.getCharsetFromContentType(type);
            if (!encodingUtils.isCharsetSupported(enc)) {
                enc = null;
            }
        }

        // now get the appropriate match (may be set in the properties)
        this.encoding = encodingUtils.getJvmConverter(enc);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCharacterEncoding: set to " + this.encoding);
        }
        return this.encoding;
    }

    /*
     * @see javax.servlet.ServletRequest#getContentLength()
     */
    @Trivial
    @Override
    public int getContentLength() {
        long rc = this.request.getContentLength();
        // TODO what should we be doing here?
        if (rc > Integer.MAX_VALUE) {
            rc = Integer.MAX_VALUE;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getContentLength: " + rc);
        }
        return (int) rc;

    }

    /*
     * @see javax.servlet.ServletRequest#getContentType()
     */
    @Trivial
    @Override
    public String getContentType() {
        String type = this.request.getHeader("Content-Type");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getContentType: " + type);
        }
        return type;
    }

    /*
     * @see javax.servlet.ServletRequest#getInputStream()
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (null != this.inReader) {
            throw new IllegalStateException("Input reader already obtained");
        }
        this.streamActive = true;
        if (null == this.inStream) {
            this.inStream = new RequestBody(this.request.getBody());
        }
        return this.inStream;
    }

    /*
     * @see javax.servlet.ServletRequest#getLocalAddr()
     */
    @Trivial
    @Override
    public String getLocalAddr() {
        String addr = this.connection.getLocalHostAddress();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getLocalAddr: " + addr);
        }
        return addr;
    }

    /*
     * @see javax.servlet.ServletRequest#getLocalName()
     */
    @Trivial
    @Override
    public String getLocalName() {
        String addr = this.connection.getLocalHostName(true);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getLocalName: " + addr);
        }
        return addr;
    }

    /*
     * @see javax.servlet.ServletRequest#getLocalPort()
     */
    @Trivial
    @Override
    public int getLocalPort() {
        int port = this.connection.getLocalPort();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getLocalPort: " + port);
        }
        return port;
    }

    /*
     * @see javax.servlet.ServletRequest#getLocale()
     */
    @Override
    public Locale getLocale() {
        if (null == this.locales) {
            this.locales = connection.getEncodingUtils().getLocales(this.request.getHeader("Accept-Language"));
        }
        if (this.locales.isEmpty()) {
            return null;
        }
        return this.locales.get(0);
    }

    /*
     * @see javax.servlet.ServletRequest#getLocales()
     */
    @Override
    public Enumeration<Locale> getLocales() {
        if (null == this.locales) {
            this.locales = connection.getEncodingUtils().getLocales(this.request.getHeader("Accept-Language"));
        }
        return Collections.enumeration(this.locales);
    }

    /**
     * Handle parsing the request query parameters. These may exist in the
     * URL based querystring and/or in certain types of POST body streams.
     * 
     * This will always result in a query parameter map being created, even
     * if it's empty with no query data found in the request.
     */
    private void parseParameters() {
        if (null != this.queryParameters) {
            return;
        }
        // access the underlying pre-parsed query string values
        Map<String, String[]> params = parseQueryString(this.request.getQuery());
        if ("POST".equalsIgnoreCase(this.request.getMethod())) {
            String type = this.request.getHeader("Content-Type");
            if (null != type && type.startsWith("application/x-www-form-urlencoded")) {
                // parse the form data in the POST body
                if (!this.streamActive && null == this.inReader) {
                    try {
                        parseQueryFormData();
                        // now merge the URL params into the map
                        mergeQueryParams(params);
                    } catch (IOException ioe) {
                        FFDCFilter.processException(ioe,
                                                    getClass().getName(), "parseForm",
                                                    new Object[] { this, this.request });
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Error reading POST form body: " + ioe);
                        }
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "POST form body already being read");
                    }
                }
            }
        }
        if (null == this.queryParameters) {
            // default to the URL params
            this.queryParameters = params;
        }
    }

    /**
     * Read and parse the POST body data that represents query data.
     * 
     * @throws IOException
     */
    private void parseQueryFormData() throws IOException {
        int size = getContentLength();
        if (0 == size) {
            // no body present
            this.queryParameters = new HashMap<String, String[]>();
            return;
        } else if (-1 == size) {
            // chunked encoded perhaps
            size = 1024;
        }
        StringBuilder sb = new StringBuilder(size);
        char[] data = new char[size];
        BufferedReader reader = getReader();
        int len = reader.read(data);
        while (-1 != len) {
            sb.append(data, 0, len);
            len = reader.read(data);
        }
        this.queryParameters = parseQueryString(sb.toString());
    }

    /**
     * Parse a query parameter's name out of the input character array. This
     * will undo any URLEncoding in the data (%20 is a space for example) and
     * return the resulting string.
     * 
     * Input must be in ASCII encoding.
     * 
     * @param ch
     * @param start
     * @param end
     * @return String
     * @throws IllegalArgumentException
     */
    private String parseName(char[] ch, int start, int end) {
        int len = 0;
        char[] name = new char[end - start];

        for (int i = start; i < end; i++) {
            switch (ch[i]) {
                case '+':
                    // translate plus symbols to spaces
                    name[len++] = ' ';
                    break;
                case '%':
                    // translate "%xx" to appropriate character (i.e. %20 is space)
                    if ((i + 2) < end) {
                        int num1 = Character.digit(ch[++i], 16);
                        if (-1 == num1) {
                            throw new IllegalArgumentException("" + ch[i]);
                        }
                        int num2 = Character.digit(ch[++i], 16);
                        if (-1 == num2) {
                            throw new IllegalArgumentException("" + ch[i]);
                        }
                        name[len++] = (char) ((num1 << 4) | num2);
                    } else {
                        // allow '%' at end of value or second to last character
                        for (; i < end; i++) {
                            name[len++] = ch[i];
                        }
                    }
                    break;
                default:
                    // regular character, just save it
                    name[len++] = ch[i];
                    break;
            }
        }
        return new String(name, 0, len);
    }

    /**
     * Parse a string of query parameters into a Map representing the values
     * stored using the name as the key.
     * 
     * @param data
     * @return Map
     * @throws IllegalArgumentException
     *             if the string is formatted incorrectly
     */
    private Map<String, String[]> parseQueryString(String data) {
        Map<String, String[]> map = new Hashtable<String, String[]>();
        if (null == data) {
            return map;
        }
        String valArray[] = null;
        char[] chars = data.toCharArray();
        int key_start = 0;
        for (int i = 0; i < chars.length; i++) {
            // look for the key name delimiter
            if ('=' == chars[i]) {
                if (i == key_start) {
                    // missing the key name
                    throw new IllegalArgumentException("Missing key name: " + i);
                }
                String key = parseName(chars, key_start, i);
                int value_start = ++i;
                for (; i < chars.length && '&' != chars[i]; i++) {
                    // just keep looping looking for the end or &
                }
                if (i > value_start) {
                    // did find at least one char for the value
                    String value = parseName(chars, value_start, i);
                    if (map.containsKey(key)) {
                        String oldVals[] = map.get(key);
                        valArray = new String[oldVals.length + 1];
                        System.arraycopy(oldVals, 0, valArray, 0, oldVals.length);
                        valArray[oldVals.length] = value;
                    } else {
                        valArray = new String[] { value };
                    }
                    map.put(key, valArray);
                }
                key_start = i + 1;
            }
        }
        return map;
    }

    /**
     * In certain cases, the URL will contain query string information and the
     * POST body will as well. This method merges the two maps together.
     * 
     * @param urlParams
     */
    private void mergeQueryParams(Map<String, String[]> urlParams) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "mergeQueryParams: " + urlParams);
        }
        for (Entry<String, String[]> entry : urlParams.entrySet()) {
            String key = entry.getKey();
            // prepend to postdata values if necessary
            String[] post = this.queryParameters.get(key);
            String[] url = entry.getValue();
            if (null != post) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "map already contains key " + key);
                }
                String[] newVals = new String[post.length + url.length];
                System.arraycopy(url, 0, newVals, 0, url.length);
                System.arraycopy(post, 0, newVals, url.length, post.length);
                this.queryParameters.put(key, newVals);
            } else {
                this.queryParameters.put(key, url);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "put key " + key + " into map.");
            }
        }
    }

    /*
     * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
     */
    @Override
    public String getParameter(String name) {
        String[] list = getParameterMap().get(name);
        return (null == list || 0 == list.length) ? null : list[0];
    }

    /*
     * @see javax.servlet.ServletRequest#getParameterMap()
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        if (null == this.queryParameters) {
            parseParameters();
        }
        return this.queryParameters;
    }

    /*
     * @see javax.servlet.ServletRequest#getParameterNames()
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

    /*
     * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
     */
    @Override
    public String[] getParameterValues(String name) {
        return getParameterMap().get(name);
    }

    /*
     * @see javax.servlet.ServletRequest#getProtocol()
     */
    @Override
    public String getProtocol() {
        String protocol = this.request.getVersion();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getProtocol: " + protocol);
        }
        return protocol;
    }

    /*
     * @see javax.servlet.ServletRequest#getReader()
     */
    @Override
    public BufferedReader getReader() throws IOException {
        if (this.streamActive) {
            throw new IllegalStateException("InputStream already obtained");
        }
        String enc = getCharacterEncoding();
        if (null == enc) {
            enc = connection.getEncodingUtils().getDefaultEncoding();
        }
        if (null == this.inStream) {
            this.inStream = new RequestBody(this.request.getBody());
        }
        this.inReader = new BufferedReader(
                        new InputStreamReader(this.inStream, enc));
        return this.inReader;
    }

    /**
     * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
     * @deprecated - use ServletContext.getRealPath(String)
     */
    @Override
    @Deprecated
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    /*
     * @see javax.servlet.ServletRequest#getRemoteAddr()
     */
    @Override
    public String getRemoteAddr() {
        String addr = this.connection.getRemoteHostAddress();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getRemoteAddr: " + addr);
        }
        return addr;
    }

    /*
     * @see javax.servlet.ServletRequest#getRemoteHost()
     */
    @Override
    public String getRemoteHost() {
        String addr = this.connection.getRemoteHostName(true);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getRemoteHost: " + addr);
        }
        return addr;
    }

    /*
     * @see javax.servlet.ServletRequest#getRemotePort()
     */
    @Override
    public int getRemotePort() {
        int port = this.connection.getRemotePort();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getRemotePort: " + port);
        }
        return port;
    }

    /**
     * Set the reference to the filter pipeline that is handling this
     * particular request.
     * 
     * @param pipeline
     */
    public void setFilterPipeline(FilterPipeline pipeline) {
        this.filters = pipeline;
    }

    /*
     * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    /*
     * @see javax.servlet.ServletRequest#getScheme()
     */
    @Override
    public String getScheme() {
        return this.request.getScheme();
    }

    /*
     * @see javax.servlet.ServletRequest#getServerName()
     */
    @Override
    public String getServerName() {
        String name = this.request.getVirtualHost();
        if (null == name) {
            name = "localhost";
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getServerName: " + name);
        }
        return name;
    }

    /*
     * @see javax.servlet.ServletRequest#getServerPort()
     */
    @Override
    public int getServerPort() {
        int port = this.request.getVirtualPort();
        if (-1 == port && null != this.request.getHeader("Host")) {
            // if Host is present, default to scheme
            if ("HTTP".equalsIgnoreCase(this.request.getScheme())) {
                port = 80;
            } else {
                port = 443;
            }
        }
        // if still not found, use the socket information
        if (-1 == port) {
            port = this.connection.getLocalPort();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getServerPort: " + port);
        }
        return port;
    }

    /*
     * @see javax.servlet.ServletRequest#isSecure()
     */
    @Override
    public boolean isSecure() {
        boolean secure = (null != this.connection.getSSLContext());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isSecure: " + secure);
        }
        return secure;
    }

    /*
     * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
     */
    @Override
    public void removeAttribute(String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Removing attribute: " + name);
        }
        this.attributes.remove(name);
    }

    /*
     * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Setting attribute: " + name);
        }
        this.attributes.put(name, value);
    }

    /*
     * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
     */
    @Override
    public void setCharacterEncoding(String name) throws UnsupportedEncodingException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setCharacterEncoding: " + name);
        }
        if (null != this.queryParameters || null != this.inReader) {
            // too late, ignore the setting
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring encoding, too late");
            }
            return;
        }
        if (connection.getEncodingUtils().isCharsetSupported(name)) {
            this.encoding = name;
        } else {
            throw new UnsupportedEncodingException(name);
        }
    }

    /**
     * Access the SSL cipher suite used in this connection.
     * 
     * @return String - null if this is not a secure connection
     */
    private String getCipherSuite() {
        String suite = null;
        SSLContext ssl = this.connection.getSSLContext();
        if (null != ssl) {
            suite = ssl.getSession().getCipherSuite();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCipherSuite [" + suite + "]");
        }
        return suite;
    }

    /**
     * Access any client SSL certificates for this connection.
     * 
     * @return X509Certificate[] - null if non-ssl or none exist
     */
    private X509Certificate[] getPeerCertificates() {
        X509Certificate[] rc = null;
        SSLContext ssl = this.connection.getSSLContext();
        if (null != ssl && (ssl.getNeedClientAuth() || ssl.getWantClientAuth())) {
            try {
                Object[] objs = ssl.getSession().getPeerCertificates();
                if (null != objs) {
                    rc = (X509Certificate[]) objs;
                }
            } catch (Throwable t) {
                FFDCFilter.processException(t,
                                            getClass().getName(), "peercerts",
                                            new Object[] { this, ssl });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Error getting peer certs; " + t);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (null == rc) {
                Tr.debug(tc, "getPeerCertificates: none");
            } else {
                Tr.debug(tc, "getPeerCertificates: " + rc.length);
            }
        }
        return rc;
    }

    /*
     * @see javax.servlet.ServletRequest#getServletContext()
     */
    @Override
    public ServletContext getServletContext() {
        return this.srvContext;
    }

    /**
     * Set the current servlet context on the message.
     * 
     * @param context
     */
    public void setServletContext(ServletContext context) {
        this.srvContext = context;
    }

    @Override
    public boolean authenticate(HttpServletResponse arg0) throws IOException,
                    ServletException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Part getPart(String arg0) throws IOException, ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void login(String arg0, String arg1) throws ServletException {
        // TODO Auto-generated method stub
    }

    @Override
    public void logout() throws ServletException {
        // TODO Auto-generated method stub
    }

    @Override
    public AsyncContext getAsyncContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AsyncContext startAsync() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getContentLengthLong()
     */
    @Override
    public long getContentLengthLong() {
        // TODO Servlet3.1 updates
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#changeSessionId()
     */
    @Override
    public String changeSessionId() {
        // TODO Servlet3.1 updates
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#upgrade(java.lang.Class)
     */
    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        // TODO Servlet3.1 updates
        return null;
    }
}
