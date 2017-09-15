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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.httpsvc.session.internal.SessionConfig;
import com.ibm.ws.httpsvc.session.internal.SessionInfo;
import com.ibm.wsspi.http.EncodingUtils;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpResponse;

/**
 * HTTP bundle's implementation of a servlet response message.
 */
public class ResponseMessage implements HttpServletResponse {
    /** Debug variable */
    private static final TraceComponent tc = Tr.register(ResponseMessage.class);

    /** HTTP response message wrapped by this servlet interface */
    private HttpResponse response = null;
    /** Reference to the request message */
    private RequestMessage request = null;
    /** Possible output body as a Writer interface */
    private PrintWriter outWriter = null;
    /** Possible output body as a Stream interface */
    private ResponseBody outStream = null;
    /** Encoding used for the possible outgoing body */
    private String encoding = null;
    /** Locale for the possible outgoing body */
    private Locale locale = null;
    /** Possible explicitly set content-type */
    private String contentType = null;
    /** Flag on whether the body stream interface is active or not */
    private boolean streamActive = false;

    /** Wrapped connection object */
    private HttpInboundConnection connection = null;

    /**
     * Constructor.
     * 
     * @param conn
     * @param req
     */
    public ResponseMessage(HttpInboundConnection conn, RequestMessage req) {
        init(conn, req);
    }

    /**
     * Initialize this response message with the input connection.
     * 
     * @param conn
     * @param req
     */
    public void init(HttpInboundConnection conn, RequestMessage req) {
        this.request = req;
        this.response = conn.getResponse();
        this.connection = conn;
        this.outStream = new ResponseBody(this.response.getBody());
        this.locale = Locale.getDefault();
    }

    /**
     * Clear all the temporary variables of this response.
     */
    public void clear() {
        this.contentType = null;
        this.encoding = null;
        this.locale = null;
        this.outStream = null;
        this.outWriter = null;
        this.streamActive = false;
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie)
     */
    @Override
    public void addCookie(Cookie cookie) {
        this.response.addCookie(convertCookie(cookie));
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#addDateHeader(java.lang.String, long)
     */
    @Override
    public void addDateHeader(String hdr, long value) {
        this.response.addHeader(hdr, connection.getDateFormatter().getRFC1123Time(new Date(value)));
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#addHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void addHeader(String hdr, String value) {
        this.response.addHeader(hdr, value);
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#addIntHeader(java.lang.String, int)
     */
    @Override
    public void addIntHeader(String hdr, int value) {
        this.response.addHeader(hdr, Integer.toString(value));
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#containsHeader(java.lang.String)
     */
    @Override
    public boolean containsHeader(String hdr) {
        return (null != this.response.getHeader(hdr));
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectURL(java.lang.String)
     */
    @Override
    public String encodeRedirectURL(String url) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "encodeRedirectURL: " + url);
        }
        return encodeURL(url);
    }

    /**
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String)
     * @deprecated - use encodeRedirectURL(String)
     */
    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#encodeURL(java.lang.String)
     */
    @Override
    public String encodeURL(String url) {
        SessionInfo info = this.request.getSessionInfo();
        if (null == info || !info.getSessionConfig().isURLRewriting()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "encodeURL: no session update needed");
            }
            return url;
        }
        if (null == info.getSession()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "encodeURL: no session found");
            }
            return url;
        }
        return SessionInfo.encodeURL(url, info);
    }

    /**
     * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
     * @deprecated - use encodeURL(String)
     */
    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#sendError(int)
     */
    @Override
    public void sendError(int code) throws IOException {
        sendError(code, null);
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#sendError(int, java.lang.String)
     */
    @Override
    public void sendError(int code, String message) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }
        resetBuffer();
        this.response.setStatus(code);
        commit();
        if (null != message) {
            if (null != this.outWriter) {
                this.outWriter.print(message);
            } else {
                this.outStream.print(message);
            }
            flushBuffer();
        }
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
     */
    @Override
    public void sendRedirect(String location) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }
        if (null == location) {
            throw new IllegalArgumentException("Location is null");
        }
        resetBuffer();
        this.response.setHeader("Location", convertURItoURL(location.trim()));
        this.response.setStatus(307);
        flushBuffer();
    }

    /**
     * Convert a possible URI to a full URL.
     * 
     * @param uri
     * @return String
     */
    private String convertURItoURL(String uri) {
        int indexScheme = uri.indexOf("://");
        if (-1 != indexScheme) {
            int indexQuery = uri.indexOf('?');
            if (-1 == indexQuery || indexScheme < indexQuery) {
                // already a full url
                return uri;
            }
        }
        StringBuilder sb = new StringBuilder();
        String scheme = this.request.getScheme();
        sb.append(scheme).append("://");
        sb.append(this.request.getServerName());
        int port = this.request.getServerPort();
        if ("http".equalsIgnoreCase(scheme) && 80 != port) {
            sb.append(':').append(port);
        } else if ("https".equalsIgnoreCase(scheme) && 443 != port) {
            sb.append(':').append(port);
        }
        String data = this.request.getContextPath();
        if (!"".equals(data)) {
            sb.append(data);
        }
        if (0 == uri.length()) {
            sb.append('/');
        } else if (uri.charAt(0) == '/') {
            // relative to servlet container root...
            sb.append(uri);
        } else {
            // relative to current URI,
            data = this.request.getServletPath();
            if (!"".equals(data)) {
                sb.append(data);
            }
            data = this.request.getPathInfo();
            if (null != data) {
                sb.append(data);
            }
            sb.append('/');
            sb.append(uri);
            // TODO: webcontainer converted "/./" and "/../" info too
        }

        return sb.toString();
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#setDateHeader(java.lang.String, long)
     */
    @Override
    public void setDateHeader(String hdr, long value) {
        if (-1L == value) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setDateHeader(" + hdr + ", -1), removing header");
            }
            this.response.removeHeader(hdr);
        } else {
            this.response.setHeader(hdr, connection.getDateFormatter().getRFC1123Time(new Date(value)));
        }
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#setHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void setHeader(String hdr, String value) {
        if (null == value) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setHeader(" + hdr + ", null), removing header");
            }
            this.response.removeHeader(hdr);
        } else {
            this.response.setHeader(hdr, value);
        }
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#setIntHeader(java.lang.String, int)
     */
    @Override
    public void setIntHeader(String hdr, int value) {
        if (-1 == value) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setIntHeader(" + hdr + ", -1), removing header");
            }
            this.response.removeHeader(hdr);
        } else {
            this.response.setHeader(hdr, Integer.toString(value));
        }
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#setStatus(int)
     */
    @Override
    public void setStatus(int code) {
        this.response.setStatus(code);
    }

    /*
     * @see javax.servlet.http.HttpServletResponse#setStatus(int, java.lang.String)
     */
    @Override
    public void setStatus(int code, String reason) {
        this.response.setStatus(code);
        this.response.setReason(reason);
    }

    /*
     * @see javax.servlet.ServletResponse#flushBuffer()
     */
    @Override
    public void flushBuffer() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Flushing buffers: " + this);
        }
        if (null != this.outWriter) {
            this.outWriter.flush();
        } else {
            this.outStream.flush();
        }
    }

    /*
     * @see javax.servlet.ServletResponse#getBufferSize()
     */
    @Override
    public int getBufferSize() {
        return this.outStream.getBufferSize();
    }

    /*
     * @see javax.servlet.ServletResponse#getCharacterEncoding()
     */
    @Override
    public String getCharacterEncoding() {
        if (null == this.encoding) {
            EncodingUtils encodingUtils = connection.getEncodingUtils();
            this.encoding = encodingUtils.getEncodingFromLocale(this.locale);
            if (null == this.encoding) {
                this.encoding = encodingUtils.getDefaultEncoding();
            }
        }
        return this.encoding;
    }

    /*
     * @see javax.servlet.ServletResponse#getContentType()
     */
    @Override
    public String getContentType() {
        String type = this.response.getHeader("Content-Type");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getContentType: " + type);
        }
        return type;
    }

    /*
     * @see javax.servlet.ServletResponse#getLocale()
     */
    @Override
    public Locale getLocale() {
        return this.locale;
    }

    /*
     * @see javax.servlet.ServletResponse#getOutputStream()
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getOutputStream: " + this);
        }
        if (null != this.outWriter) {
            throw new IllegalStateException("Output writer already obtained");
        }
        this.streamActive = true;
        return this.outStream;
    }

    /*
     * @see javax.servlet.ServletResponse#getWriter()
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getWriter: " + this);
        }
        if (this.streamActive) {
            throw new IllegalStateException("Output stream already obtained");
        }
        if (null == this.outWriter) {
            this.outWriter = new PrintWriter(
                            new OutputStreamWriter(
                                            this.outStream, getCharacterEncoding()), false);
        }
        return this.outWriter;
    }

    /*
     * @see javax.servlet.ServletResponse#isCommitted()
     */
    @Override
    public boolean isCommitted() {
        return this.response.isCommitted();
    }

    /**
     * When headers are being marshalled out, this message moves to committed
     * state which will disallow most further changes.
     */
    public void commit() {
        if (isCommitted()) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Committing: " + this);
        }
        if (null == this.response.getHeader("Content-Language")) {
            // content-language not yet set, add now
            this.response.setHeader(
                                    "Content-Language",
                                    getLocale().toString().replace('_', '-'));
        }
        if (null != this.response.getHeader("Content-Encoding")) {
            this.response.removeHeader("Content-Length");
        }
        // if a session exists, store the set-cookie in the response now
        SessionInfo info = this.request.getSessionInfo();
        if (null != info) {
            SessionConfig config = info.getSessionConfig();
            if (config.usingCookies() && null != info.getSession()) {
                // create a session cookie now
                boolean add = true;
                HttpCookie existing = this.response.getCookie(config.getIDName());
                if (null != existing) {
                    if (!info.getSession().getId().equals(existing.getValue())) {
                        // some other session cookie existed but doesn't match
                        // the current session, remove it
                        this.response.removeCookie(existing);
                    } else {
                        // this session already exists
                        add = false;
                    }
                }
                if (add) {
                    this.response.addCookie(
                                    convertCookie(SessionInfo.encodeCookie(info)));
                }
            }
        } // end-if-request-session-exists
    }

    /**
     * Finish any processing necessary before the connection begins it's
     * own final work and completes this request/response exchange.
     */
    public void finish() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Finishing " + this);
        }
        // if there is a print writer, flush any content unless it is already
        // closed. We don't need to do this with streams.
        if (null != this.outWriter) {
            this.outWriter.checkError();
        }
    }

    /*
     * @see javax.servlet.ServletResponse#reset()
     */
    @Override
    public void reset() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }
        this.response.setStatus(200);
        this.response.removeAllHeaders();
        this.locale = Locale.getDefault();
        this.encoding = null;
        resetBuffer();
    }

    /*
     * @see javax.servlet.ServletResponse#resetBuffer()
     */
    @Override
    public void resetBuffer() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }
        this.outStream.clear();
    }

    /*
     * @see javax.servlet.ServletResponse#setBufferSize(int)
     */
    @Override
    public void setBufferSize(int size) {
        if (isCommitted()) {
            throw new IllegalStateException("Unable to change buffer size");
        }
        this.outStream.setBufferSize(size);
    }

    /*
     * @see javax.servlet.ServletResponse#setCharacterEncoding(java.lang.String)
     */
    @Override
    public void setCharacterEncoding(String charset) {
        if (null != this.outWriter || isCommitted()) {
            return;
        }
        if (null != charset) {
            this.encoding = connection.getEncodingUtils().stripQuotes(charset);
        }
        if (null != this.contentType) {
            int index = this.contentType.indexOf("charset=");
            StringBuilder sb = new StringBuilder();
            if (-1 != index) {
                sb.append(this.contentType.substring(0, index - 1).trim());
            } else {
                sb.append(this.contentType);
            }
            if (this.encoding != null) {
                sb.append(";charset=").append(this.encoding);
            }

            this.contentType = sb.toString();
            this.response.setHeader("Content-Type", this.contentType);
        }
    }

    /*
     * @see javax.servlet.ServletResponse#setContentLength(int)
     */
    @Override
    public void setContentLength(int length) {
        this.response.setContentLength(length);
    }

    /*
     * @see javax.servlet.ServletResponse#setContentType(java.lang.String)
     */
    @Override
    public void setContentType(String value) {
        if (isCommitted()) {
            return;
        }
        if (null == value) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setContentType: null, removing header");
            }
            this.response.removeHeader("Content-Type");
            return;
        }
        boolean addEncoding = false;
        String type = value;

        // see if the input value contains a charset
        int charsetIndex = value.indexOf("charset=");
        if (-1 != charsetIndex) {
            if (null == this.outWriter) {
                // can update both mime-set and charset
                addEncoding = true;
                this.encoding = connection.getEncodingUtils().stripQuotes(value.substring(charsetIndex + 8));
            } else {
                // can only update the mimeset so strip the charset off
                type = value.substring(0, charsetIndex).trim();
                charsetIndex = -1;
            }
        }
        if (null != this.outWriter) {
            // can still update the mime-type but not charset
            addEncoding = true;
        } else {
            // TODO don't quite follow why webcontainer checks for text*
            if (type.startsWith("text")) {
                addEncoding = true;
            }
        }
        if (addEncoding) {
            if (-1 != charsetIndex) {
                type = type.substring(0, charsetIndex - 1);
            }
            if (this.encoding != null) {
                type = type + ";charset=" + this.encoding;
            }
        }
        this.contentType = type;
        this.response.setHeader("Content-Type", type);
    }

    /*
     * @see javax.servlet.ServletResponse#setLocale(java.util.Locale)
     */
    @Override
    public void setLocale(Locale inLocale) {
        if (isCommitted() || null == inLocale) {
            return;
        }
        this.locale = inLocale;
        if (null != this.outWriter || null != this.encoding) {
            return;
        }
        EncodingUtils encodingUtils = connection.getEncodingUtils();
        this.encoding = encodingUtils.getEncodingFromLocale(inLocale);
        if (null == this.encoding) {
            this.encoding = encodingUtils.getDefaultEncoding();
        }
        if (null != this.contentType) {
            int index = this.contentType.indexOf("charset=");
            StringBuilder sb = new StringBuilder();
            if (-1 != index) {
                sb.append(this.contentType.substring(0, index - 1).trim());
            }
            if (this.encoding != null) {
                sb.append(";charset=").append(this.encoding);
            }

            this.response.setHeader("Content-Type", sb.toString());
        }
    }

    /**
     * Convert from a J2EE spec cookie to the HTTP transport (non-servlet) cookie
     * object.
     * 
     * @param cookie
     * @return HttpCookie
     */
    private HttpCookie convertCookie(Cookie cookie) {
        HttpCookie rc = new HttpCookie(cookie.getName(), cookie.getValue());
        rc.setVersion(cookie.getVersion());
        rc.setComment(cookie.getComment());
        rc.setDomain(cookie.getDomain());
        rc.setPath(cookie.getPath());
        rc.setMaxAge(cookie.getMaxAge());
        rc.setSecure(cookie.getSecure());
        return rc;
    }

    /*
     * @see javax.servlet.HttpServletResponse#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String name) {
        // Note: servlet 3.0
        return this.response.getHeader(name);
    }

    /*
     * @see javax.servlet.HttpServletResponse#getHeaderNames()
     */
    @Override
    public Collection<String> getHeaderNames() {
        // Note: servlet 3.0
        return this.response.getHeaderNames();
    }

    /*
     * @see javax.servlet.HttpServletResponse#getHeaders(java.lang.String)
     */
    @Override
    public Collection<String> getHeaders(String name) {
        // Note: servlet 3.0
        return this.response.getHeaders(name);
    }

    /*
     * @see javax.servlet.HttpServletResponse#getStatus()
     */
    @Override
    public int getStatus() {
        // Note: servlet 3.0
        return this.response.getStatus();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponse#setContentLengthLong(long)
     */
    @Override
    public void setContentLengthLong(long len) {
        // TODO Servlet3.1 updates        
    }

}
