/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.webcontainerext.ws;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;

import com.ibm.websphere.servlet31.response.DummyResponse31;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspHelper;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspServletRequest;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspServletResponse;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer31.servlet.DummyRequest31;

public class PrepareJspHelper23 extends PrepareJspHelper implements Runnable {

    static final protected Logger logger;
    private static final String CLASS_NAME = "com.ibm.ws.jsp.webcontainerext.PrepareJspHelper23";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    public PrepareJspHelper23(AbstractJSPExtensionProcessor s, IServletContext webapp, JspOptions options) {
        super(s, webapp, options);
    }

    @Override
    protected PrepareJspServletRequest newPrepareJspServletRequest() {
        DummyRequest31 dummyRequest = new DummyRequest31();
        return new PrepareJspServletRequest23Impl(dummyRequest);
    }

    @Override
    protected PrepareJspServletResponse newPrepareJspServletResponse() {
        DummyResponse31 dummyResponse = new DummyResponse31();
        return new PrepareJspServletResponse23Impl(dummyResponse);
    }

}

class PrepareJspServletResponse23Impl extends HttpServletResponseWrapper implements PrepareJspServletResponse {

    public PrepareJspServletResponse23Impl(HttpServletResponse response) {
        super(response);
    }

    private final PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());

    @Override
    public void addCookie(Cookie cookie) {}

    @Override
    public void addDateHeader(String name, long date) {}

    @Override
    public void addHeader(String name, String value) {}

    @Override
    public void addIntHeader(String name, int value) {}

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeUrl(String url) {
        return url;
    }

    @Override
    public String encodeURL(String url) {
        return encodeUrl(url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return encodeRedirectUrl(url);
    }

    @Override
    public void sendError(int code) {}

    @Override
    public void sendError(int code, String message) {}

    @Override
    public void sendRedirect(String location) {}

    @Override
    public void setDateHeader(String name, long date) {}

    @Override
    public void setHeader(String name, String value) {}

    @Override
    public void setIntHeader(String name, int value) {}

    @Override
    public void setStatus(int sc) {}

    @Override
    public void setStatus(int sc, String sm) {}

    @Override
    public void flushBuffer() {}

    @Override
    public int getBufferSize() {
        return 1024;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return null;
    }

    @Override
    public PrintWriter getWriter() {
        return this.writer;
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {}

    @Override
    public void resetBuffer() {}

    @Override
    public void setBufferSize(int size) {}

    @Override
    public void setCharacterEncoding(String encoding) {}

    @Override
    public void setContentLength(int length) {}

    @Override
    public void setContentType(String type) {}

    @Override
    public void setLocale(Locale loc) {}

    @Override
    public HttpServletResponse getHttpServletResponse() {
        return this;
    }
}

class PrepareJspServletRequest23Impl extends HttpServletRequestWrapper implements PrepareJspServletRequest {

    public PrepareJspServletRequest23Impl(HttpServletRequest request) {
        super(request);
        // TODO Auto-generated constructor stub
    }

    private Cookie[] cookies;
    private String method;
    private String requestURI;
    private String servletPath;
    private String pathInfo;
    private String queryString;
    private String attribute;

    @Override
    public Cookie[] getCookies() {
        return null;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    public int getIntHeader(String name, int def) {
        return -1;
    }

    public long getLongHeader(String name, long def) {
        return -1;
    }

    public long getDateHeader(String name, long def) {
        return -1;
    }

    @Override
    public Enumeration getHeaderNames() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public long getDateHeader(String arg0) {
        return 0;
    }

    @Override
    public Enumeration getHeaders(String arg0) {
        return null;
    }

    @Override
    public int getIntHeader(String arg0) {
        return 0;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public boolean isUserInRole(String arg0) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public Object getAttribute(String arg0) {
        return attribute;
    }

    @Override
    public Enumeration getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {}

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0L;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getParameter(String arg0) {
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String arg0) {
        return null;
    }

    @Override
    public Map getParameterMap() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {}

    @Override
    public void removeAttribute(String arg0) {}

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
    }

    @Override
    public String getRealPath(String arg0) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
        return null;
    }

    public void setAttribute(String string) {
        attribute = string;
    }

    public void setCookies(Cookie[] cookies) {
        this.cookies = cookies;
    }

    public void setMethod(String string) {
        method = string;
    }

    public void setPathInfo(String string) {
        pathInfo = string;
    }

    @Override
    public void setQueryString(String string) {
        queryString = string;
    }

    @Override
    public void setRequestURI(String string) {
        requestURI = string;
    }

    @Override
    public void setServletPath(String string) {
        servletPath = string;
    }

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return this;
    }

}