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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

public interface HttpServletRequest extends ServletRequest {

    public static final String BASIC_AUTH = "BASIC";
    public static final String FORM_AUTH = "FORM";
    public static final String CLIENT_CERT_AURG = "CLIENT CERT";
    public static final String DIGEST_AUTH = "DIGEST";

    public abstract boolean authenticate(HttpServletResponse resp) throws IOException, ServletException;

    public abstract String changeSessionId();

    public abstract String getAuthType();

    public abstract String getContextPath();

    public abstract Cookie[] getCookies();

    // Method added in Servlet 4.0
    public abstract Cookie[] getCookies(String name);

    public abstract long getDateHeader(String name);

    public abstract String getHeader(String name);

    public abstract java.util.Enumeration getHeaderNames();

    public abstract java.util.Enumeration getHeaders(String name);

    public abstract int getIntHeader(String name);

    // Method added in Servlet 4.0
    public abstract ServletMapping getMapping();

    public abstract String getMethod();

    public abstract Part getPart(String name) throws IOException, ServletException;

    public abstract java.util.Collection getParts() throws IOException, ServletException;

    public abstract String getPathInfo();

    public abstract String getPathTranslated();

    public abstract PushBuilder newPushBuilder();

    public abstract String getQueryString();

    public abstract String getRemoteUser();

    public abstract String getRequestedSessionId();

    public abstract String getRequestURI();

    public abstract StringBuffer getRequestURL();

    public abstract String getServletPath();

    public abstract HttpSession getSession();

    public abstract HttpSession getSession(boolean create);

    public abstract java.security.Principal getUserPrincipal();

    public abstract boolean isRequestedSessionIdFromCookie();

    public abstract boolean isRequestedSessionIdFromUrl();

    public abstract boolean isRequestedSessionIdFromURL();

    public abstract boolean isRequestedSessionIdValid();

    public abstract boolean isUserInRole(String arg0);

    public abstract void login(String arg0, String arg1) throws javax.servlet.ServletException;

    public abstract void logout() throws javax.servlet.ServletException;

    public abstract <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws java.io.IOException, javax.servlet.ServletException;;

    public default Map<String, String> getTrailerFields() throws IllegalStateException {
        return new HashMap<String, String>();
    }

    public default boolean isTrailerFieldsReady() {
        return false;
    }
}
