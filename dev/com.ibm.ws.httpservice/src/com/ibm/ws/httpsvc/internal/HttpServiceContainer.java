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
package com.ibm.ws.httpsvc.internal;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.http.base.internal.dispatch.Dispatcher;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.httpsvc.servlet.internal.RequestMessage;
import com.ibm.ws.httpsvc.servlet.internal.ResponseMessage;
import com.ibm.ws.httpsvc.session.internal.SessionManager;
import com.ibm.wsspi.http.HttpContainer;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.VirtualHost;

/**
 * Primary container component for the HttpService implementation. This
 * handles much of the lifecycle and cross-bundle dependencies for the
 * bundle.
 */
public class HttpServiceContainer implements HttpContainer {
    private static final TraceComponent tc = Tr.register(HttpServiceContainer.class);

    private static final class SingletonSessionManager {
        static final SessionManager instance = new SessionManager();
    }

    private final HandlerRegistry registry;
    private final Dispatcher dispatcher;
    private LogService logService;
    private VirtualHost virtualHost;

    private final Set<String> activeContextRoots = new HashSet<String>();

    /**
     * Constructor.
     */
    public HttpServiceContainer() {
        this.registry = new HandlerRegistry(this);
        this.dispatcher = new Dispatcher(this.registry);
    }

    /**
     * Activate this DS component.
     * 
     * @param context
     * @throws Exception
     */
    protected void activate(ComponentContext context) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating");
        }

        processConfig(context.getProperties());

        SystemLogger.setLogService(logService);
    }

    /**
     * Deactivate this DS component.
     * 
     * @param context
     * @throws Exception
     */
    protected void deactivate(ComponentContext context, int reason) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating, reason=" + reason);
        }

        this.registry.removeAll();
    }

    protected void setLogService(LogService logService) {
        this.logService = logService;
    }

    protected void unsetLogService(LogService logService) {
        if (this.logService == logService)
            logService = null;
    }

    /** Required static reference */
    protected synchronized void setVirtualHost(VirtualHost virtualHost) {
        this.virtualHost = virtualHost;
        if (!activeContextRoots.isEmpty()) {
            for (String contextRoot : activeContextRoots) {
                virtualHost.addContextRoot(contextRoot, this);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "add virtual host " + virtualHost);
        }
    }

    /** Required static reference */
    protected void unsetVirtualHost(VirtualHost virtualHost) {}

    public synchronized void addContextRoot(String contextRoot) {
        activeContextRoots.add(contextRoot);
        if (virtualHost != null) {
            virtualHost.addContextRoot(contextRoot, this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "HTTP SERVICE: Context root added " + contextRoot);
        }
    }

    public synchronized void removeContextRoot(String contextRoot) {
        activeContextRoots.remove(contextRoot);
        if (virtualHost != null) {
            virtualHost.removeContextRoot(contextRoot, this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Context root removed " + contextRoot);
        }
    }

    /**
     * DS method for runtime updates to configuration without stopping and
     * restarting the component.
     * 
     * @param properties
     */
    protected void modified(Map<?, ?> properties) {
        if (properties instanceof Dictionary<?, ?>) {
            processConfig((Dictionary<?, ?>) properties);
        } else {
            Dictionary<?, ?> newconfig = new Hashtable<Object, Object>(properties);
            processConfig(newconfig);
        }
    }

    /**
     * Process configuration information.
     * 
     * @param properties
     */
    protected synchronized void processConfig(Dictionary<?, ?> properties) {
        if (properties == null)
            return;
    }

    /** {@inheritDoc} */
    @Override
    public Runnable createRunnableHandler(final HttpInboundConnection inboundConnection) {
        final Dispatcher reqDispatcher = dispatcher;

        String uri = inboundConnection.getRequest().getURI();

        for (ServletHandler handler : registry.getServlets()) {
            if (handler.matches(uri)) {
                final ServletHandler myHandler = handler;

                return new Runnable() {
                    @Override
                    public void run() {
                        final RequestMessage request = new RequestMessage(inboundConnection, SingletonSessionManager.instance);
                        final ResponseMessage response = new ResponseMessage(inboundConnection, request);

                        Exception error = null;
                        try {
                            request.setServletContext(myHandler.getServlet().getServletConfig().getServletContext());
                            request.setServletPath(myHandler.getAlias());
                            reqDispatcher.dispatch(request, response);
                        } catch (Exception e) {
                            error = e;

                            if (!response.isCommitted()) {
                                try {
                                    response.sendError(500);
                                } catch (Throwable t) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "Unable to send 500 error response", t);
                                    }
                                }
                            }
                        } finally {
                            response.finish();
                            inboundConnection.finish(error);
                        }
                    }
                };
            }
        }

        return null;
    }

    protected HandlerRegistry getHandlerRegistry() {
        return registry;
    }

}
