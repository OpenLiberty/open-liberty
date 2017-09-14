/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* Temporary file pending public availability of api jar */
package javax.servlet.http;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.ServletRequestWrapper;

public class HttpServletRequestWrapper extends ServletRequestWrapper implements HttpServletRequest {

    HttpServletRequest _req = this.getHttpServletRequest();

    public HttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    private HttpServletRequest getHttpServletRequest() {
        return (HttpServletRequest) super.getRequest();
    }

    @Override
    public boolean authenticate(HttpServletResponse resp) throws IOException, ServletException {
        // Call wrapped Request
        return this.getHttpServletRequest().authenticate(resp);
    }

    @Override
    public String getAuthType() {
        // Call wrapped request
        return this.getHttpServletRequest().getAuthType();
    }

    @Override
    public String getContextPath() {
        // Call wrapped request
        return this.getHttpServletRequest().getContextPath();
    }

    @Override
    public Cookie[] getCookies() {
        // Call wrapped request
        return this.getHttpServletRequest().getCookies();
    }

    @Override
    public Cookie[] getCookies(String name) {
        // Call wrapped request
        return this.getHttpServletRequest().getCookies(name);
    }

    @Override
    public long getDateHeader(String name) {
        // Call wrapped request
        return this.getHttpServletRequest().getDateHeader(name);
    }

    @Override
    public String getHeader(String name) {
        // Call wrapped request
        return this.getHttpServletRequest().getHeader(name);
    }

    @Override
    public Enumeration getHeaderNames() {
        // Call wrapped request
        return this.getHttpServletRequest().getHeaderNames();
    }

    @Override
    public Enumeration getHeaders(String name) {
        // Call wrapped request
        return this.getHttpServletRequest().getHeaders(name);
    }

    @Override
    public int getIntHeader(String name) {
        // Call wrapped request
        return this.getHttpServletRequest().getIntHeader(name);
    }

    @Override
    public ServletMapping getMapping() {
        // Call wrapped request
        return this.getHttpServletRequest().getMapping();
    }

    @Override
    public String getMethod() {
        // Call wrapped request
        return this.getHttpServletRequest().getMethod();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        // Call wrapped request
        return this.getHttpServletRequest().getPart(name);
    }

    @Override
    public Collection getParts() throws IOException, ServletException {
        // Call wrapped request
        return this.getHttpServletRequest().getParts();
    }

    @Override
    public String getPathInfo() {
        // Call wrapped request
        return this.getHttpServletRequest().getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        // Call wrapped request
        return this.getHttpServletRequest().getPathTranslated();
    }

    @Override
    public PushBuilder newPushBuilder() {
        // Call wrapped request
        return this.getHttpServletRequest().newPushBuilder();
    }

    @Override
    public String getQueryString() {
        // Call wrapped request
        return this.getHttpServletRequest().getQueryString();
    }

    @Override
    public String getRemoteUser() {
        // Call wrapped request
        return this.getHttpServletRequest().getRemoteUser();
    }

    @Override
    public String getRequestedSessionId() {
        // Call wrapped request
        return this.getHttpServletRequest().getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        // Call wrapped request
        return this.getHttpServletRequest().getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        // Call wrapped request
        return this.getHttpServletRequest().getRequestURL();
    }

    @Override
    public String getServletPath() {
        // Call wrapped request
        return this.getHttpServletRequest().getServletPath();
    }

    @Override
    public HttpSession getSession() {
        // Call wrapped request
        return this.getHttpServletRequest().getSession();
    }

    @Override
    public HttpSession getSession(boolean create) {
        // Call wrapped request
        return this.getHttpServletRequest().getSession(create);
    }

    @Override
    public Principal getUserPrincipal() {
        // Call wrapped request
        return this.getHttpServletRequest().getUserPrincipal();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        // Call wrapped request
        return this.getHttpServletRequest().isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        // Call wrapped request
        return this.getHttpServletRequest().isRequestedSessionIdFromUrl();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        // Call wrapped request
        return this.getHttpServletRequest().isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        // Call wrapped request
        return this.getHttpServletRequest().isRequestedSessionIdValid();
    }

    @Override
    public boolean isUserInRole(String user) {
        // Call wrapped request
        return this.getHttpServletRequest().isUserInRole(user);
    }

    @Override
    public void login(String user, String password) throws ServletException {
        // Call wrapped Request
        this.getHttpServletRequest().login(user, password);

    }

    @Override
    public void logout() throws ServletException {
        // Call wrapped Request
        this.getHttpServletRequest().logout();
    }

    @Override
    public String changeSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        // TODO Auto-generated method stub
        return null;
    }

}
