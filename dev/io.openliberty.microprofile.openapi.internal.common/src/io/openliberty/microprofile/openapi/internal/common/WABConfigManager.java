/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.openapi.internal.common;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.wab.configure.WABConfiguration;

/**
 * Manages services to provide the real value for a Web Application Bundle that has a configurable context path
 * <p>
 * Example:
 * <p>
 * Configure a WAB with {@code Web-ContextPath: @myWABContextPath}
 * <p>
 * Set the context path value:
 *
 * <pre>
 * WABConfigManager manager = new WABConfigManager(myContext, "myWABContextPath", "myWAB");
 * manager.setPath("/foo");
 * </pre>
 *
 * <p>
 * Update the context path value:
 *
 * <pre>
 * manager.setPath("/bar");
 * </pre>
 *
 * <p>
 * At shutdown:
 *
 * <pre>
 * manager.close();
 * </pre>
 *
 * For more detail on the actual services registered by this class and how they're used, see {@link WABConfiguration}
 */
public class WABConfigManager {

    private static final TraceComponent tc = Tr.register(WABConfigManager.class);
    private static final WABConfiguration SERVICE_INSTANCE = new WABConfiguration() {
    };

    private final BundleContext context;
    private final String contextName;
    private final String name;

    private String contextPath;
    private ServiceRegistration<WABConfiguration> wabConfigReg;
    private boolean closed;

    /**
     * WAB Configuration Manager for OpenAPI Endpoints
     *
     * @param context     Component Context for the Web bundle
     * @param contextName Name of the property within the bnd that the Web-ContextPath is bound to e.g. "openAPIUIPATH"
     * @param name        Name associated with the bundle for tracing
     */
    public WABConfigManager(BundleContext context, String contextName, String name) {
        this.context = context;
        this.contextName = contextName;
        this.contextPath = null;
        this.name = name;
        this.closed = false;
    }

    /**
     * Set the real value for the context path variable managed by this object
     *
     * @param path the path value
     */
    public void setPath(String path) {
        synchronized (this) {
            // If we've been closed, do nothing
            // The services have been unregistered and we don't want to re-register them
            if (closed) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, name + " web app bundle setPath called after close");
                }
                return;
            }

            // If path is unchanged, do nothing
            if (path == this.contextPath) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, name + " web app bundle path unchanged: " + path);
                }
                return;
            }

            if (path == null) {
                // path is null, unregister service
                // wabConfigReg should always be non-null here
                if (wabConfigReg != null) {
                    wabConfigReg.unregister();
                    wabConfigReg = null;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, name + " web app bundle unregistered");
                    }
                }
            } else {
                // Path is set, create properties
                final Dictionary<String, String> props = new Hashtable<String, String>();
                props.put(WABConfiguration.CONTEXT_NAME, contextName);
                props.put(WABConfiguration.CONTEXT_PATH, path);

                if (wabConfigReg == null) {
                    // No existing service, register one
                    wabConfigReg = context.registerService(WABConfiguration.class, SERVICE_INSTANCE, props);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, name + " web app bundle registered, WAB config=" + wabConfigReg + " using props=" + props);
                    }
                } else {
                    // Existing service, just update the properties
                    wabConfigReg.setProperties(props);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, name + " web app bundle modified, WAB config=" + wabConfigReg + " using props=" + props);
                    }
                }
            }

            this.contextPath = path;
        }
    }

    public void close() {
        synchronized (this) {
            if (wabConfigReg == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, name + " web app bundle shutting down. Service already unregistered");
                }
            } else {
                wabConfigReg.unregister();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, name + " web app bundle shutting down. Service unregistered");
                }
            }
            closed = true;
        }
    }

}
