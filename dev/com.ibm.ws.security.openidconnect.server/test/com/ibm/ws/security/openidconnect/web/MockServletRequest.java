/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

/*
 * Mock servlet request which is used for some of servlet tests.
 */
public class MockServletRequest implements HttpServletRequest {

    HashMap<String, String> _headers = new HashMap<String, String>();
    HashMap<String, ArrayList<String>> _params = new HashMap<String, ArrayList<String>>();
    Cookie[] _cookies = null;
    Principal _principal = null;

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public Cookie[] getCookies() {
        return _cookies;
    }

    public void setCookies(Cookie[] cookies) {
        _cookies = cookies;
    }

    @Override
    public long getDateHeader(String arg0) {
        return 0;
    }

    @Override
    public String getHeader(String key) {
        return _headers.get(key);
    }

    public void setHeader(String key, String value) {
        _headers.put(key, value);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(_headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String arg0) {
        return Collections.enumeration(_headers.values());
    }

    @Override
    public int getIntHeader(String arg0) {
        return 0;
    }

    String method = "POST";

    @Override
    public String getMethod() {
        return "POST";
    }

    public void setMethod(String methodIn) {
        method = methodIn;
    }

    String pathInfo = null;

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(String info) {
        pathInfo = info;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    protected String servletPath = null;

    @Override
    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(String path) {
        servletPath = path;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean arg0) {
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        return _principal;
    }

    public void setUserPrincipal(Principal principal) {
        _principal = principal;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isUserInRole(String arg0) {
        return false;
    }

    @Override
    public Object getAttribute(String arg0) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
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
    public String getLocalAddr() {
        return null;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public String getParameter(String key) {
        List<String> paramValues = _params.get(key);
        String result = null;
        if (paramValues != null && paramValues.size() > 0) {
            result = paramValues.get(0);
        }
        return result;
    }

    public void setParameter(String key, String value) {
        ArrayList<String> newValues = new ArrayList<String>();
        newValues.add(value);
        _params.put(key, newValues);
    }

    public void setParameter(String key, String[] values) {
        ArrayList<String> newValues = new ArrayList<String>();
        newValues.addAll(Arrays.asList(values));
        _params.put(key, newValues);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(_params.keySet());

    }

    @Override
    public String[] getParameterValues(String key) {
        String[] result = null;
        List<String> vals = _params.get(key);
        if (vals != null && vals.size() > 0) {
            result = vals.toArray(new String[vals.size()]);
        }
        return result;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRealPath(String arg0) {
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
    public int getRemotePort() {
        return 0;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
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
    public boolean isSecure() {
        return false;
    }

    @Override
    public void removeAttribute(String arg0) {
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
    }

    @Override
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map getParameterMap() {
        Map<String, String[]> result = new HashMap<String, String[]>();
        for (Iterator<String> i = _params.keySet().iterator(); i.hasNext();) {
            String key = i.next();
            List<String> vals = _params.get(key);
            if (vals != null && vals.size() > 0) {
                String[] valsArray = vals.toArray(new String[vals.size()]);
                result.put(key, valsArray);
            }
        }
        return result;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext startAsync() {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
        return null;
    }

    @Override
    public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
        return false;
    }

    @Override
    public Part getPart(String arg0) throws IOException, ServletException {
        return null;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public void login(String arg0, String arg1) throws ServletException {
    }

    @Override
    public void logout() throws ServletException {
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

}
