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
package com.ibm.ws.openapi31;

import static com.ibm.ws.openapi31.OpenAPIUtils.getOptionalValue;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.wab.configure.WABConfiguration;

/**
 * This component manages the life cycle and configuration of the public and private endpoints.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class OpenAPIEndpointManager {

    private static final TraceComponent tc = Tr.register(OpenAPIEndpointManager.class);

    /**
     * Configuration property for customization of the URL for the public endpoint. The default value is "/api".
     */
    private static final String PUBLIC_URL = OASConfig.EXTENSIONS_PREFIX + "liberty.public.url";
    private static final String PUBLIC_URL_DEFAULT_VALUE = "/api";

    /**
     * Configuration property to enable/disable the private endpoint. The default value is false.
     */
    private static final String ENABLE_PRIVATE_URL = OASConfig.EXTENSIONS_PREFIX + "liberty.enable.private.url";
    private static final boolean ENABLE_PRIVATE_URL_DEFAULT_VALUE = false;

    /** The variable name specified in the bnd.bnd file of the public WAB project. */
    private static final String PUBLIC_OPEN_API_DOCS_VAR_NAME = "publicOpenAPIDocsURL";
    private static final String PUBLIC_OPEN_API_DOCS_CONTEXT_PATH_SUFFIX = "/docs";

    private static final String PUBLIC_OPEN_API_EXPLORER_VAR_NAME = "publicOpenAPIExplorerURL";
    private static final String PUBLIC_OPEN_API_EXPLORER_CONTEXT_PATH_SUFFIX = "/explorer";

    /** The variable name specified in the bnd.bnd file of the private WAB project. */
    private static final String PRIVATE_OPEN_API_DOCS_VAR_NAME = "privateOpenAPIDocsURL";
    private static final String PRIVATE_OPEN_API_DOCS_URL = "/ibm/api/docs";

    private static final String PRIVATE_OPEN_API_EXPLORER_VAR_NAME = "privateOpenAPIExplorerURL";
    private static final String PRIVATE_OPEN_API_EXPLORER_URL = "/ibm/api/explorer";

    private static final String RESERVED_URL = "/ibm/api";

    // Cached WAB service configurations for Public URLs - /api/docs and /api/explorer
    private final List<PublicOpenAPIWABConfigManager> publicWabConfigMgrs;

    // Cached WAB service configurations for Private URLs - /ibm/api/docs and /ibm/api/explorer
    private final List<PrivateOpenAPIWABConfigManager> privateWabConfigMgrs;

    public OpenAPIEndpointManager() {
        // Initialize WAB configuration managers for public URLs
        publicWabConfigMgrs = new ArrayList<PublicOpenAPIWABConfigManager>();
        publicWabConfigMgrs.add(new PublicOpenAPIWABConfigManager(PUBLIC_OPEN_API_DOCS_VAR_NAME, PUBLIC_OPEN_API_DOCS_CONTEXT_PATH_SUFFIX));
        publicWabConfigMgrs.add(new PublicOpenAPIWABConfigManager(PUBLIC_OPEN_API_EXPLORER_VAR_NAME, PUBLIC_OPEN_API_EXPLORER_CONTEXT_PATH_SUFFIX));
        // Initialize WAB configuration managers for private URLs
        privateWabConfigMgrs = new ArrayList<PrivateOpenAPIWABConfigManager>();
        privateWabConfigMgrs.add(new PrivateOpenAPIWABConfigManager(PRIVATE_OPEN_API_DOCS_VAR_NAME, PRIVATE_OPEN_API_DOCS_URL));
        privateWabConfigMgrs.add(new PrivateOpenAPIWABConfigManager(PRIVATE_OPEN_API_EXPLORER_VAR_NAME, PRIVATE_OPEN_API_EXPLORER_URL));
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        final Config config = ConfigProvider.getConfig(OpenAPIEndpointManager.class.getClassLoader());
        processPublicURL(context, config);
        processEnablePrivate(context, config);
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        deactivate(publicWabConfigMgrs);
        deactivate(privateWabConfigMgrs);
    }

    private void deactivate(List<? extends WABConfigManager> configMgrs) {
        configMgrs.forEach(v -> {
            v.deactivate();
        });
    }

    private void processPublicURL(final ComponentContext context, final Config config) {
        String publicURL = getOptionalValue(config, PUBLIC_URL, String.class, PUBLIC_URL_DEFAULT_VALUE);
        // Add a forward slash if the URL does not already start with one.
        if (!publicURL.startsWith("/")) {
            publicURL = '/' + publicURL;
        }
        // Remove trailing slash if the URL contains one.
        if (publicURL.endsWith("/") && !publicURL.equals("/")) {
            publicURL = publicURL.substring(0, publicURL.length() - 1);
        }
        // Check if the public URL specified is "/ibm/api".
        if (RESERVED_URL.equals(publicURL)) {
            // Report a warning and keep the default value.
            Tr.warning(tc, "PUBLIC_URL_INVALID", RESERVED_URL);
            publicURL = PUBLIC_URL_DEFAULT_VALUE;
        }
        final String _publicURL = publicURL;
        publicWabConfigMgrs.forEach(v -> {
            v.updateWABConfiguration(context, _publicURL);
        });
    }

    private void processEnablePrivate(final ComponentContext context, final Config config) {
        final boolean enablePrivate = getOptionalValue(config, ENABLE_PRIVATE_URL, Boolean.class, ENABLE_PRIVATE_URL_DEFAULT_VALUE);
        privateWabConfigMgrs.forEach(v -> {
            v.processEnablePrivate(context, enablePrivate);
        });
    }

    abstract static class WABConfigManager {

        final String contextName;
        ServiceRegistration<WABConfiguration> wabConfigReg;

        public WABConfigManager(String contextName) {
            this.contextName = contextName;
        }

        public final void pushConfiguration(ComponentContext context, String path) {
            final BundleContext bundleContext = context.getBundleContext();
            final Dictionary<String, String> props = new Hashtable<String, String>();
            props.put(WABConfiguration.CONTEXT_NAME, contextName);
            props.put(WABConfiguration.CONTEXT_PATH, path);
            if (wabConfigReg == null) {
                wabConfigReg = bundleContext.registerService(WABConfiguration.class, new WABConfiguration() {}, props);
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    registerEvent(props);
                }
            } else {
                wabConfigReg.setProperties(props);
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    modifiedEvent(props);
                }
            }
        }

        public final void deactivate() {
            if (wabConfigReg != null) {
                wabConfigReg.unregister();
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    deactivateEvent();
                }
                wabConfigReg = null;
            }
        }

        public abstract void registerEvent(Dictionary<String, String> props);

        public abstract void modifiedEvent(Dictionary<String, String> props);

        public abstract void deactivateEvent();
    }

    final static class PublicOpenAPIWABConfigManager extends WABConfigManager {

        private final String contextPathSuffix;

        public PublicOpenAPIWABConfigManager(String contextName, String contextPathSuffix) {
            super(contextName);
            this.contextPathSuffix = contextPathSuffix;
        }

        public void updateWABConfiguration(ComponentContext context, String publicURL) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "OpenAPI " + PUBLIC_URL + " property updated: " + publicURL + " WAB config=" + wabConfigReg);
            }
            // Push new WABConfiguration to the WAB installer.
            pushConfiguration(context, publicURL + contextPathSuffix);
        }

        @Override
        public void registerEvent(Dictionary<String, String> props) {
            Tr.event(tc, "OpenAPI public web app bundle registered, WAB config=" + wabConfigReg + " using props=" + props);
        }

        @Override
        public void modifiedEvent(Dictionary<String, String> props) {
            Tr.event(tc, "OpenAPI public web app bundle modified, WAB config=" + wabConfigReg + " using props=" + props);
        }

        @Override
        public void deactivateEvent() {
            Tr.event(tc, "Unregistered public web app bundle, WAB config=" + wabConfigReg +
                         " for contextName=" + contextName + " and contextPathSuffix=" + contextPathSuffix);
        }
    }

    final static class PrivateOpenAPIWABConfigManager extends WABConfigManager {

        private final String contextPath;

        public PrivateOpenAPIWABConfigManager(String contextName, String contextPath) {
            super(contextName);
            this.contextPath = contextPath;
        }

        public void processEnablePrivate(ComponentContext context, boolean enabled) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "OpenAPI " + ENABLE_PRIVATE_URL + " property updated: " + enabled + " WAB config=" + wabConfigReg);
            }
            // if enablePrivateURL="false" and WAB has never started then there is no work to do here.
            if (wabConfigReg == null && !enabled) {
                return;
            }
            if (enabled) {
                // Push configuration to the WAB installer to start private WAB
                pushConfiguration(context, contextPath);
            } else {
                deactivate();
            }
        }

        @Override
        public void registerEvent(Dictionary<String, String> props) {
            Tr.event(tc, "OpenAPI private web app bundle registered, WAB config=" + wabConfigReg + " using props=" + props);
        }

        @Override
        public void modifiedEvent(Dictionary<String, String> props) {
            Tr.event(tc, "OpenAPI private web app bundle modified, WAB config=" + wabConfigReg + " using props=" + props);
        }

        @Override
        public void deactivateEvent() {
            Tr.event(tc, "Unregistered private web app bundle, WAB config=" + wabConfigReg + " for contextName=" + contextName + " and contextPath=" + contextPath);
        }
    }
}
