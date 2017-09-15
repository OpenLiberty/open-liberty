/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;

/**
 * JaxWsHttpServletRequestAdapter is used to recover the Web Component Context MetaData while invoking the methods from request instance,
 * as in EJB based Web Services, the EJB invocation context is built very earlier in one intercepter, which may cause issue for those methods requiring
 * Web Component Context MetaData
 */
public class JaxRsHttpServletRequestAdapter extends HttpServletRequestWrapper implements HttpServletRequest {

    private final IWebAppNameSpaceCollaborator collaborator;

    private final ComponentMetaData componentMetaData;

    private final HttpServletRequest request;

    /**
     * @param request
     */
    public JaxRsHttpServletRequestAdapter(HttpServletRequest request, IWebAppNameSpaceCollaborator collaborator, ComponentMetaData componentMetaData) {
        super(request);
        this.collaborator = collaborator;
        this.componentMetaData = componentMetaData;
        this.request = request;

    }

    @Override
    public String getRemoteUser() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getRemoteUser();
        } finally {
            collaborator.postInvoke();
        }
    }

    @Override
    public Principal getUserPrincipal() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getUserPrincipal();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getAsyncContext()
     */
    @Override
    public AsyncContext getAsyncContext() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getAsyncContext();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
     */
    @Override
    public Object getAttribute(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getAttribute(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getAttributeNames()
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getAttributeNames();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getCharacterEncoding()
     */
    @Override
    public String getCharacterEncoding() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getCharacterEncoding();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getContentLength()
     */
    @Override
    public int getContentLength() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getContentLength();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getContentType()
     */
    @Override
    public String getContentType() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getContentType();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getDispatcherType()
     */
    @Override
    public DispatcherType getDispatcherType() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getDispatcherType();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getInputStream()
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getInputStream();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getLocalAddr()
     */
    @Override
    public String getLocalAddr() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getLocalAddr();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getLocalName()
     */
    @Override
    public String getLocalName() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getLocalName();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getLocalPort()
     */
    @Override
    public int getLocalPort() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getLocalPort();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getLocale()
     */
    @Override
    public Locale getLocale() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getLocale();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getLocales()
     */
    @Override
    public Enumeration<Locale> getLocales() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getLocales();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
     */
    @Override
    public String getParameter(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getParameter(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getParameterMap()
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getParameterMap();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getParameterNames()
     */
    @Override
    public Enumeration<String> getParameterNames() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getParameterNames();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
     */
    @Override
    public String[] getParameterValues(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getParameterValues(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getProtocol()
     */
    @Override
    public String getProtocol() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getProtocol();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getReader()
     */
    @Override
    public BufferedReader getReader() throws IOException {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getReader();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
     */
    @SuppressWarnings("deprecation")
    @Override
    public String getRealPath(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getRealPath(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getRemoteAddr()
     */
    @Override
    public String getRemoteAddr() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getRemoteAddr();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getRemoteHost()
     */
    @Override
    public String getRemoteHost() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getRemoteHost();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getRemotePort()
     */
    @Override
    public int getRemotePort() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getRemotePort();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getRequestDispatcher(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getScheme()
     */
    @Override
    public String getScheme() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getScheme();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getServerName()
     */
    @Override
    public String getServerName() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getServerName();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getServerPort()
     */
    @Override
    public int getServerPort() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getServerPort();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getServletContext()
     */
    @Override
    public ServletContext getServletContext() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getServletContext();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#isAsyncStarted()
     */
    @Override
    public boolean isAsyncStarted() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.isAsyncStarted();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#isAsyncSupported()
     */
    @Override
    public boolean isAsyncSupported() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.isAsyncSupported();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#isSecure()
     */
    @Override
    public boolean isSecure() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.isSecure();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
     */
    @Override
    public void removeAttribute(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            request.removeAttribute(arg0);
        } finally {
            collaborator.postInvoke();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void setAttribute(String arg0, Object arg1) {
        try {
            collaborator.preInvoke(componentMetaData);
            request.setAttribute(arg0, arg1);
        } finally {
            collaborator.postInvoke();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
     */
    @Override
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
        try {
            collaborator.preInvoke(componentMetaData);
            request.setCharacterEncoding(arg0);
        } finally {
            collaborator.postInvoke();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#startAsync()
     */
    @Override
    public AsyncContext startAsync() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.startAsync();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#startAsync(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    @Override
    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.startAsync(arg0, arg1);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#authenticate(javax.servlet.http.HttpServletResponse)
     */
    @Override
    public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.authenticate(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getAuthType()
     */
    @Override
    public String getAuthType() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getAuthType();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getContextPath()
     */
    @Override
    public String getContextPath() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getContextPath();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getCookies()
     */
    @Override
    public Cookie[] getCookies() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getCookies();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
     */
    @Override
    public long getDateHeader(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getDateHeader(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getHeader(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getHeaderNames();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
     */
    @Override
    public Enumeration<String> getHeaders(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getHeaders(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
     */
    @Override
    public int getIntHeader(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getIntHeader(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getMethod()
     */
    @Override
    public String getMethod() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getMethod();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getPart(java.lang.String)
     */
    @Override
    public Part getPart(String arg0) throws IOException, ServletException {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getPart(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getParts()
     */
    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getParts();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getPathInfo()
     */
    @Override
    public String getPathInfo() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getPathInfo();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
     */
    @Override
    public String getPathTranslated() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getPathTranslated();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getQueryString()
     */
    @Override
    public String getQueryString() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getQueryString();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getRequestURI()
     */
    @Override
    public String getRequestURI() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getRequestURI();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getRequestURL()
     */
    @Override
    public StringBuffer getRequestURL() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getRequestURL();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
     */
    @Override
    public String getRequestedSessionId() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getRequestedSessionId();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getServletPath()
     */
    @Override
    public String getServletPath() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getServletPath();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getSession()
     */
    @Override
    public HttpSession getSession() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getSession();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
     */
    @Override
    public HttpSession getSession(boolean arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.getSession(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.isRequestedSessionIdFromCookie();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.isRequestedSessionIdFromURL();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.isRequestedSessionIdFromUrl();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.isRequestedSessionIdValid();
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
     */
    @Override
    public boolean isUserInRole(String arg0) {
        try {
            collaborator.preInvoke(componentMetaData);
            return request.isUserInRole(arg0);
        } finally {
            collaborator.postInvoke();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#login(java.lang.String, java.lang.String)
     */
    @Override
    public void login(String arg0, String arg1) throws ServletException {
        try {
            collaborator.preInvoke(componentMetaData);
            request.login(arg0, arg1);
        } finally {
            collaborator.postInvoke();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#logout()
     */
    @Override
    public void logout() throws ServletException {
        try {
            collaborator.preInvoke(componentMetaData);
            request.logout();
        } finally {
            collaborator.postInvoke();
        }
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
//    @Override
//    public String changeSessionId() {
//        // TODO Servlet3.1 updates
//        return null;
//    }

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
