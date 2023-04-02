/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ejs.ras.Traceable;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.VirtualHostConfiguration;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.http.HttpContainer;
import com.ibm.wsspi.http.VirtualHost;

/**
 *
 */
public class DynamicVirtualHostConfiguration extends VirtualHostConfiguration implements Traceable {
    private static final TraceComponent tc = Tr.register(DynamicVirtualHostConfiguration.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    final String name;
    final HashSet<String> activeContexts = new HashSet<String>();

    // keep track of the number of apps bound to this config that are in the process of starting
    private AtomicInteger appStartingCount;

    /**
     * Construct the VirtualHostConfiguration object: use this when the
     * associated VirtualHost is unknown or unavailable at construction time.
     */
    public DynamicVirtualHostConfiguration(String name) {
        super(name);
        this.name = name;
        this.appStartingCount = new AtomicInteger(0);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "DynamicVirtualHostConfiguration", "name ->"+ this.name);
        }
    }

    /**
     * Construct the VirtualHostConfiguration object with the underlying VirtualHost
     * 
     * @param config VirtualHost object representing the new/updated VirtualHost
     *            configuration
     */
    public DynamicVirtualHostConfiguration(VirtualHost config) {
        super(config);        
        this.name = config.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "DynamicVirtualHostConfiguration", "config name ->"+ this.name);
        }
    }

    /**
     * @param contextRoot
     */
    synchronized void addContextRoot(String contextRoot, HttpContainer container) {
        if (contextRoot != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "addContextRoot", "contextRoot-> ["+ contextRoot+"]");
            }
            activeContexts.add(contextRoot);
        }
        VirtualHost local = this.config;
        if (local != null) {
            local.addContextRoot(contextRoot, container);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Context root added " + contextRoot);
            }
        }
        else{
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "addContextRoot", "local VH config is null");
            }
        }
    }

    /**
     * @param contextRoot
     */
    synchronized void removeContextRoot(String contextRoot, HttpContainer container) {
        if (contextRoot != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "removeContextRoot", "contextRoot-> ["+ contextRoot+"]");
            }
            activeContexts.remove(contextRoot);
        }
        VirtualHost local = this.config;
        if (local != null) {
            local.removeContextRoot(contextRoot, container);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Context root removed " + contextRoot);
            }
        }
        else{
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "removeContextRoot", "local VH config is null");
            }
        }
    }

    public synchronized Iterator<String> getActiveContext() {
        HashSet<String> copy = new HashSet<String>(activeContexts);
        return copy.iterator();
    }

    @Override
    public String toString() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public String toTraceString() {
        return getClass().getSimpleName()
               + "[name=" + name
               + "," + config
               + "]";
    }

    /**
     * Increment this config's app starting count by one
     */
    protected void incrementAppStartingCount() {
        int count = appStartingCount.incrementAndGet();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "incrementAppStartingCount new count:", count);
        }
    }

    /**
     * Decrement this config's app starting count by one
     */
    protected void decrementAppStartingCount() {
        int count = appStartingCount.decrementAndGet();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "decrementAppStartingCount new count:", count);
        }
    }

    /**
     * Get the number of apps (which depend on this vhost config) that are currently initializing.
     * This can be used to prevent a scenario where an app that's being uninstalled destroys this
     * config while some other dependent app is starting.
     *
     * @return the number of apps depending on this config that are starting
     */
    protected int getAppStartingCount() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAppStartingCount count:", appStartingCount.get());
        }
        return appStartingCount.get();
    }
}
