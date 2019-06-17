/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.httpsvc.servlet.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.util.MimeTypes;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Implementation of a servlet context object.
 */
public class ServletContextImpl implements ExtServletContext {
    private static final TraceComponent tc = Tr.register(ServletContextImpl.class);

    private Bundle bundle;
    private ServletContext rootContext;
    private HttpContext httpContext;
    private Map<String, Object> attributes;

    public ServletContextImpl() {}

    public void init(Bundle bundle, ServletContext context, HttpContext httpContext) {
        this.bundle = bundle;
        this.rootContext = context;
        this.httpContext = httpContext;
        this.attributes = new ConcurrentHashMap<String, Object>();
    }

    @Override
    public String getContextPath() {
        return this.rootContext.getContextPath();
    }

    @Override
    public ServletContext getContext(String uri) {
        return this.rootContext.getContext(uri);
    }

    @Override
    public int getMajorVersion() {
        return this.rootContext.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return this.rootContext.getMinorVersion();
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        Enumeration<String> paths = this.bundle.getEntryPaths(normalizePath(path));
        if ((paths == null) || !paths.hasMoreElements()) {
            return null;
        }

        Set<String> set = new HashSet<String>();
        while (paths.hasMoreElements()) {
            set.add(paths.nextElement());
        }

        return set;
    }

    @Override
    public URL getResource(String path) {
        return this.httpContext.getResource(normalizePath(path));
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        URL res = getResource(path);
        if (res != null) {
            try {
                return res.openStream();
            } catch (IOException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Ignoring the exception caught while opening resource at " + path, e);
                }
                // Do nothing
            }
        }

        return null;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }

        String normalizedPath = path.trim().replaceAll("/+", "/");
        if (normalizedPath.startsWith("/") && (normalizedPath.length() > 1)) {
            normalizedPath = normalizedPath.substring(1);
        }

        return normalizedPath;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String uri) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        return this.rootContext.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return this.rootContext.getInitParameterNames();
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(this.attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            this.removeAttribute(name);
        } else if (name != null) {
            this.attributes.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    /*
     * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
     */
    @Override
    public String getRealPath(String path) {
        // the app is in the user bundle, not the filesystem, always return null
        return null;
    }

    @Override
    public String getServerInfo() {
        return this.rootContext.getServerInfo();
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        // deprecated, always return null
        return null;
    }

    @Override
    public String getServletContextName() {
        return this.rootContext.getServletContextName();
    }

    @Override
    public Enumeration<String> getServletNames() {
        List<String> empty = Collections.emptyList();
        return Collections.enumeration(empty);
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        List<Servlet> empty = Collections.emptyList();
        return Collections.enumeration(empty);
    }

    @Override
    public void log(String message) {
        SystemLogger.info(message);
    }

    @Override
    public void log(Exception cause, String message) {
        SystemLogger.error(message, cause);
    }

    @Override
    public void log(String message, Throwable cause) {
        SystemLogger.error(message, cause);
    }

    @Override
    public Dynamic addFilter(String arg0, String arg1)
                    throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public Dynamic addFilter(String arg0, Filter arg1)
                    throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public Dynamic addFilter(String arg0, Class<? extends Filter> arg1)
                    throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public void addListener(Class<? extends EventListener> arg0) {}

    @Override
    public void addListener(String arg0) {}

    @Override
    public <T extends EventListener> void addListener(T arg0) {}

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
                                                                String arg1)
                    throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
                                                                Servlet arg1)
                    throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
                                                                Class<? extends Servlet> arg1)
                    throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> arg0)
                    throws ServletException {
        return null;
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> arg0)
                    throws ServletException {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> arg0)
                    throws ServletException {
        return null;
    }

    @Override
    public void declareRoles(String... arg0) {}

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public int getEffectiveMajorVersion() throws UnsupportedOperationException {
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() throws UnsupportedOperationException {
        return 0;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String arg0) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String arg0) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public boolean setInitParameter(String arg0, String arg1) {
        return false;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {}

    @Override
    public String getMimeType(String file) {
        String type = this.httpContext.getMimeType(file);
        if (type != null) {
            return type;
        }

        return MimeTypes.get().getByFile(file);
    }

    @Override
    public boolean handleSecurity(HttpServletRequest req, HttpServletResponse res)
                    throws IOException {
        return this.httpContext.handleSecurity(req, res);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContext#getVirtualServerName()
     */
    @Override
    public String getVirtualServerName() {
        return null;
    }
}
