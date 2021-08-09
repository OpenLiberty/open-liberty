/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.lra.internal;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
@Component(name = "io.openliberty.microprofile.lra.1.0.internal.config", service = LraConfig.class, configurationPid = "io.openliberty.microprofile.lra.1.0.internal.config", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, property = { "service.vendor=IBM" })
public class LraConfig {
    private static final TraceComponent tc = Tr.register(LraConfig.class);

    private final Map<String, Object> properties = new HashMap<>();

    /**
     * DS method to activate this component
     *
     * @param compcontext the context of this component
     * @param properties the new configuration properties
     */
    @Activate
    protected void activate(ComponentContext compcontext, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating LraConfig");
        }
        this.updateConfiguration(properties);
    }

    /**
     * DS method to deactivate this component
     *
     * @param compcontext the context of this component
     */
    @Deactivate
    protected void deactivate(ComponentContext compcontext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Deactivating " + this);
        }
    }

    /**
     * DS method to modify the configuration of this component
     *
     * @param compcontext the context of this component
     * @param properties the updated configuration properties
     */
    @Modified
    protected void modified(ComponentContext compcontext, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Modifying " + this);
        }
        this.updateConfiguration(properties);
    }

    /**
     * Updates the current configuration properties
     *
     * @param properties the updated configuration properties
     */
    protected void updateConfiguration(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Updating properties: " + properties);
        }
        if (properties != null) {
            this.properties.clear();
            this.properties.putAll(properties);
        }
    }

    public int getPort() {
        // This is written according to the suggestions for getting properties in the comments in
        // CDI12ContainerConfig.java
        //boolean enableImplicitBeanArchivesValue = (Boolean) this.properties.get("enableImplicitBeanArchives");
        int port = (Integer) this.properties.get("port");
        return port;
    }

    public String getHost() {
        String host = (String) this.properties.get("host");
        return host;
    }

    public String getPath() {
        String path = (String) this.properties.get("path");
        return path;
    }
}
