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
package com.ibm.ws.httpsvc.internal;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.service.DefaultHttpContext;
import org.apache.felix.http.base.internal.service.ResourceServlet;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.httpsvc.servlet.internal.ServletContextManager;

/**
 *
 */
public class RegisteredHttpServiceImpl implements ExtHttpService {
    private static final TraceComponent tc = Tr.register(RegisteredHttpServiceImpl.class);

    /** DS-managed reference to the HTTP Container */
    private HttpServiceContainer container;

    /**
     * OSGi DS: Set the reference to the container (required reference).
     */
    protected void setHttpContainer(HttpServiceContainer ref) {
        this.container = ref;
    }

    /**
     * OSGi DS: Remove the reference to the container
     */
    protected void unsetHttpContainer(HttpServiceContainer ref) {}

    private Bundle bundle;
    private Set<Servlet> localServlets;
    private Set<Filter> localFilters;
    private ServletContextManager contextManager;

    private final Object servletLock = new Object()
    {};
    private final Object filterLock = new Object()
    {};

    /**
     * Activate this component.
     * 
     * @param ctxt
     */
    protected void activate(ComponentContext ctxt) {
        this.bundle = ctxt.getUsingBundle();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating for bundle " + this.bundle);
        }

        this.localServlets = new HashSet<Servlet>();
        this.localFilters = new HashSet<Filter>();

        this.contextManager = new ServletContextManager(this.bundle, ctxt.getProperties());
    }

    /**
     * Deactivate this component.
     * 
     * @param ctxt
     */
    protected void deactivate(ComponentContext ctxt, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating, reason=" + reason);
        }
        unregisterAll();
    }

    private ExtServletContext getServletContext(HttpContext context) {
        if (context == null) {
            context = createDefaultHttpContext();
        }

        return this.contextManager.getServletContext(context);
    }

    /** {@inheritDoc} */
    @Override
    public void registerFilter(Filter filter, String pattern, Dictionary initParams, int ranking, HttpContext context) throws ServletException {
        if (filter == null) {
            throw new IllegalArgumentException("Filter must not be null");
        }
        FilterHandler handler = new FilterHandler(getServletContext(context), filter, pattern, ranking);
        handler.setInitParams(initParams);
        synchronized (filterLock) {
            container.getHandlerRegistry().addFilter(handler);
            this.localFilters.add(filter);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void registerServlet(String alias, Servlet servlet, Dictionary initparams, HttpContext context) throws ServletException, NamespaceException {
        if (servlet == null) {
            throw new IllegalArgumentException("Servlet must not be null");
        }
        if (!isAliasValid(alias)) {
            throw new IllegalArgumentException("Malformed servlet alias [" + alias + "]");
        }

        ServletHandler handler = new ServletHandler(getServletContext(context), servlet, alias);
        handler.setInitParams(initparams);
        synchronized (servletLock) {
            container.getHandlerRegistry().addServlet(handler);
            this.localServlets.add(servlet);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void registerResources(String alias, String name, HttpContext context) throws NamespaceException {
        if (!isNameValid(name)) {
            throw new IllegalArgumentException("Malformed resource name [" + name + "]");
        }

        try {
            Servlet servlet = new ResourceServlet(name);
            registerServlet(alias, servlet, null, context);
        } catch (ServletException e) {
            SystemLogger.error("Failed to register resources", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void unregister(String alias) {
        synchronized (servletLock) {
            Servlet servlet = container.getHandlerRegistry().removeServletByAlias(alias);
            if (servlet != null)
                this.localServlets.remove(servlet);
        }
    }

    /** {@inheritDoc} */
    @Override
    public HttpContext createDefaultHttpContext() {
        return new DefaultHttpContext(this.bundle);
    }

    /** {@inheritDoc} */
    @Override
    public void unregisterFilter(Filter filter) {
        unregisterFilter(filter, true);
    }

    /** {@inheritDoc} */
    @Override
    public void unregisterServlet(Servlet servlet) {
        unregisterServlet(servlet, true);
    }

    private void unregisterFilter(Filter filter, final boolean destroy) {
        if (filter != null) {
            synchronized (filterLock) {
                container.getHandlerRegistry().removeFilter(filter, destroy);
                this.localFilters.remove(filter);
            }
        }
    }

    private void unregisterServlet(Servlet servlet, final boolean destroy) {
        if (servlet != null) {
            synchronized (servletLock) {
                container.getHandlerRegistry().removeServlet(servlet, destroy);
                this.localServlets.remove(servlet);
            }
        }
    }

    @FFDCIgnore(IllegalArgumentException.class)
    public void unregisterAll() {
        HashSet<Servlet> servlets = new HashSet<Servlet>(this.localServlets);
        for (Servlet servlet : servlets) {
            try {
                unregisterServlet(servlet, false);
            } catch (IllegalArgumentException iex) {
            }
        }

        HashSet<Filter> filters = new HashSet<Filter>(this.localFilters);
        for (Filter fiter : filters) {
            unregisterFilter(fiter, false);
        }
    }

    private boolean isNameValid(String name) {
        if (name == null) {
            return false;
        }

        if (!name.equals("/") && name.endsWith("/")) {
            return false;
        }

        return true;
    }

    private boolean isAliasValid(String alias) {
        if (alias == null) {
            return false;
        }

        if (!alias.equals("/") && (!alias.startsWith("/") || alias.endsWith("/"))) {
            return false;
        }

        return true;
    }
}
