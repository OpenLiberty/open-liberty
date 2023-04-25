/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.openapi.ui;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.wab.configure.WABConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;
import java.util.Hashtable;

public class OpenAPIWABConfigManager {

    private static final TraceComponent tc = Tr.register(OpenAPIUIEndpointManager.class);

    final ComponentContext context;
    final String contextName;
    final String contextPath;
    final String name;

    ServiceRegistration<WABConfiguration> wabConfigReg;

    /**
     * WAB Configuration Manager for OpenAPI Endpoints
     *
     * @param context Component Context for the Web bundle
     * @param contextName Name of the property within the bnd that the Web-ContextPath is bound to
     * @param contextPath Path for the bundle
     * @param name Name associated with the OpenAPI bundle
     */
    public OpenAPIWABConfigManager(ComponentContext context, String contextName, String contextPath, String name) {
        this.context = context;
        this.contextName = contextName;
        this.contextPath = contextPath;
        this.name=name;
    }

    /**
     * Active Web Bundle
     */
    public void activate() {
        final BundleContext bundleContext = context.getBundleContext();
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(WABConfiguration.CONTEXT_NAME, contextName);
        props.put(WABConfiguration.CONTEXT_PATH, contextPath);
        if (wabConfigReg == null) {
            wabConfigReg = bundleContext.registerService(WABConfiguration.class, new WABConfiguration() {}, props);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                registerEvent(props);
            }
        } else {
            wabConfigReg.setProperties(props);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                modifiedEvent(props);
            }
        }
    }

    /**
     * Deactivate Web bundle
     */
    public void deactivate() {
        if (wabConfigReg != null) {
            wabConfigReg.unregister();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                deactivateEvent();
            }
            wabConfigReg = null;
        }
    }

    public void registerEvent(Dictionary<String, String> props) {
        Tr.event(tc, name + " web app bundle registered, WAB config=" + wabConfigReg + " using props=" + props);
    }

    public void modifiedEvent(Dictionary<String, String> props) {
        Tr.event(tc, name + " web app bundle modified, WAB config=" + wabConfigReg + " using props=" + props);
    }

    public void deactivateEvent() {
        Tr.event(tc, "Unregistered web app bundle "+ name +", WAB config=" + wabConfigReg + " for contextName=" + contextName + " and contextPath=" + contextPath);
    }

}
