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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.wab.configure.WABConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Manage the service registration of Web Application Bundles that support configurable context paths
 * 
 * For more information about WAB Configuration, including how to enable configurable context paths for bundles, refer to:
 * dev/com.ibm.websphere.appserver.spi.wab.configure/build/libs/javadoc/com/ibm/wsspi/wab/configure/WABConfiguration.html
 * 
 */
public class WABConfigManager {

    private static final TraceComponent tc = Tr.register(WABConfigManager.class);

    private final BundleContext context;
    private final String contextName;
    private final String contextPath;
    private final String name;

    private ServiceRegistration<WABConfiguration> wabConfigReg;

    /**
     * WAB Configuration Manager for OpenAPI Endpoints
     *
     * @param context     Component Context for the Web bundle
     * @param contextName Name of the property within the bnd that the Web-ContextPath is bound to e.g. "openAPIUIPATH"
     * @param contextPath Context path for the bundle to be served from
     * @param name        Name associated with the bundle for tracing
     */
    public WABConfigManager(BundleContext context, String contextName, String contextPath, String name) {
        this.context = context;
        this.contextName = contextName;
        this.contextPath = contextPath;
        this.name = name;
    }

    /**
     * Register Web Bundle
     */
    public void register() {
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(WABConfiguration.CONTEXT_NAME, contextName);
        props.put(WABConfiguration.CONTEXT_PATH, contextPath);
        if (wabConfigReg == null) {
            wabConfigReg = context.registerService(WABConfiguration.class, new WABConfiguration() {
            }, props);
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
     * Unregister Web bundle
     */
    public void unregister() {
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
        Tr.event(tc, "Unregistered web app bundle " + name + ", WAB config=" + wabConfigReg + " for contextName=" + contextName + " and contextPath=" + contextPath);
    }

}
