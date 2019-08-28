/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.wab.configure.WABConfiguration;

@Component(service = { MetricsConfig.class }, configurationPid = "com.ibm.ws.microprofile.metrics", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, property = { "service.vendor=IBM" })
public class MetricsConfig {

    private static final TraceComponent tc = Tr.register(MetricsConfig.class);

    public static final String CONFIG_AUTHENTICATION = "authentication";

    private static final String MP_METRICS_ENDPOINT_URL = "/metrics";

    /** The variable name specified in the bnd.bnd file of the public WAB project. */
    private static final String PUBLIC_MP_METRICS_VAR_NAME = "publicMetricsURL";

    /** The variable name specified in the bnd.bnd file of the private WAB project. */
    private static final String PRIVATE_MP_METRICS_VAR_NAME = "privateMetricsURL";

    private Boolean authentication;

    // Cached WAB service configurations for Public MicroProfile Metrics
    private final WABConfigManager publicWabConfigMgr;

    // Cached WAB service configurations for Private MicroProfile Metrics
    private final WABConfigManager privateWabConfigMgr;

    public MetricsConfig() {
        // Initialize WAB configuration managers for public URLs
        publicWabConfigMgr = new WABConfigManager(PUBLIC_MP_METRICS_VAR_NAME, MP_METRICS_ENDPOINT_URL);
        // Initialize WAB configuration managers for private URLs
        privateWabConfigMgr = new WABConfigManager(PRIVATE_MP_METRICS_VAR_NAME, MP_METRICS_ENDPOINT_URL);
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activate MetricsConfig", properties);
        }
        processConfig(context, properties);
    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Modified MetricsConfig", properties);
        }
        processConfig(context, properties);
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Deactivate MetricsConfig with reason=" + reason);
        }
        publicWabConfigMgr.deactivate();
        privateWabConfigMgr.deactivate();
    }

    private synchronized void processConfig(ComponentContext context, Map<String, Object> properties) {
        final Boolean authenticationOld = authentication;

        authentication = (Boolean) properties.get(CONFIG_AUTHENTICATION);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Processing configuration - " + toString());
        }

        if (valueChanged(authenticationOld, authentication)) {
            if (authentication) {
                publicWabConfigMgr.processEnableAuthentication(context, false);
                privateWabConfigMgr.processEnableAuthentication(context, true);
            } else {
                privateWabConfigMgr.processEnableAuthentication(context, false);
                publicWabConfigMgr.processEnableAuthentication(context, true);
            }
        }
    }

    private boolean valueChanged(Object oldValue, Object newValue) {
        return oldValue == null ? newValue != null : !oldValue.equals(newValue);
    }

    @Override
    public String toString() {
        return "MetricsConfig [authentication=" + authentication + "]";
    }

    final static class WABConfigManager {
        private final String contextPath;
        private final String contextName;
        ServiceRegistration<WABConfiguration> wabConfigReg;

        public WABConfigManager(String contextName, String contextPath) {
            this.contextName = contextName;
            this.contextPath = contextPath;
        }

        public final void deactivate() {
            if (wabConfigReg != null) {
                wabConfigReg.unregister();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    deactivateEvent();
                }
                wabConfigReg = null;
            }
        }

        public void processEnableAuthentication(ComponentContext context, boolean enabled) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "mpMetrics authentication attribute updated: " + enabled + " WAB config=" + wabConfigReg);
            }
            // if enabled="false" and WAB has never started then there is no work to do here.
            if (wabConfigReg == null && !enabled) {
                return;
            }
            if (enabled) {
                // Push WAB configuration to the WAB installer to start the WAB
                pushConfiguration(context, contextPath);
            } else {
                deactivate();
            }
        }

        private final void pushConfiguration(ComponentContext context, String path) {
            final BundleContext bundleContext = context.getBundleContext();
            final Dictionary<String, String> props = new Hashtable<String, String>();
            props.put(WABConfiguration.CONTEXT_NAME, contextName);
            props.put(WABConfiguration.CONTEXT_PATH, path);
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

        public void registerEvent(Dictionary<String, String> props) {
            Tr.event(tc, "Registered web app bundle", toString(), "props=" + props + " }");
        }

        public void modifiedEvent(Dictionary<String, String> props) {
            Tr.event(tc, "Modified web app bundle", toString(), "props=" + props + " }");
        }

        public void deactivateEvent() {
            Tr.event(tc, "Unregistered web app bundle", toString());
        }

        @Override
        public String toString() {
            return "WABConfigManager [contextPath=" + contextPath + ", contextName=" + contextName + ", wabConfigReg=" + wabConfigReg + "]";
        }
    }
}
