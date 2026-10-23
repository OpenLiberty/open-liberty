/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
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
package com.ibm.ws.wsat.service.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Map;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.jaxws.wsat.components.WSATConfigService;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.wsat.service.Handler;
import com.ibm.ws.wsat.service.WSATUtil;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * The WS-AT configuration service.
 *
 * <p>This component is immutable: all of its state is established by the constructor and there is no
 * {@code @Modified} method. Any change to the {@code wsAtomicTransaction} configuration, or to one of
 * the injected services, therefore causes DS to deactivate this component and create a new instance.
 */
@Component(name = "com.ibm.ws.wsat.service.wsatconfigservice",
           immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "service.vendor=IBM" })
public class WSATConfigServiceImpl implements WSATConfigService {

    private static final TraceComponent TC = Tr.register(WSATConfigServiceImpl.class);

    public static final String ASYNC_RESPONSE_TIMEOUT_PROPERTY = "com.ibm.ws.wsat.asyncResponseTimeout";

    public static final Long ASYNC_RESPONSE_TIMEOUT = AccessController.doPrivileged(new PrivilegedAction<Long>() {
        @Override
        public Long run() {
            return Long.getLong(ASYNC_RESPONSE_TIMEOUT_PROPERTY);
        }
    });

    private static final String SSLEnabled = "sslEnabled";
    private static final String clientAuthRef = "clientAuth";
    private static final String SSLRef = "sslRef";
    private static final String proxyRef = "externalURLPrefix";
    private static final String asyncResponseTimeoutRef = "asyncResponseTimeout";
    private static final String PROPAGATION = "propagation";
    private static final String PROPAGATION_CHECK_WSDL = "check_wsdl";
    private static final String DEFAULT_HOST = "default_host";
    private static final String WSATContextRoot = "/ibm/wsatservice";

    private static final String HTTPCONFIGSERVICE_REFERENCE_NAME = "httpOptions";
    private static final String WSATHANDLERSERVICE_REFERENCE_NAME = "handler";
    private static final String VARIABLEREGISTRY_REFERENCE_NAME = "variableRegistry";

    private static volatile WSATConfigService INSTANCE;

    private final BundleContext bundleContext;
    private final VirtualHost defaultVirtualHost;
    private final VariableRegistry variableRegistry;

    private final boolean enabled;
    private final String sslId;
    private final String proxy;
    private final long asyncResponseTimeout;
    private final boolean clientAuth;
    private final boolean checkWsdlMode;

    // The actual VirtualHost service instance for the configured non-default virtual host.
    // Obtained via BundleContext.getService() (not injected) because the virtual host to use is
    // only known from configuration. Null when using default_host.
    private final ServiceReference<VirtualHost> configuredVirtualHostRef;
    private final VirtualHost configuredVirtualHost;

    @Activate
    public WSATConfigServiceImpl(ComponentContext cc,
                                 Map<String, Object> properties,
                                 @Reference(name = WSATHANDLERSERVICE_REFERENCE_NAME) Handler handler,
                                 @Reference(name = HTTPCONFIGSERVICE_REFERENCE_NAME,
                                            target = "(&(enabled=true)(id=default_host))",
                                            cardinality = ReferenceCardinality.OPTIONAL,
                                            policyOption = ReferencePolicyOption.GREEDY) VirtualHost httpOptions,
                                 @Reference(name = VARIABLEREGISTRY_REFERENCE_NAME,
                                            cardinality = ReferenceCardinality.OPTIONAL,
                                            policyOption = ReferencePolicyOption.GREEDY) VariableRegistry variableRegistry) {
        this.bundleContext = cc.getBundleContext();
        this.defaultVirtualHost = httpOptions;
        this.variableRegistry = variableRegistry;

        // Read the configured virtual host reference from server.xml
        String virtualHostRef = (String) properties.get("virtualHostRef");
        if (virtualHostRef == null || virtualHostRef.isEmpty()) {
            virtualHostRef = DEFAULT_HOST;  // Use default if not configured
        }

        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Configured virtual host: {0}", virtualHostRef);
        }

        // Resolve a non-default virtual host directly via BundleContext.getService() so getWSATUrl()
        // can use it; the injected reference is always bound to default_host.
        ServiceReference<VirtualHost> vhRef = null;
        VirtualHost vh = null;
        if (!DEFAULT_HOST.equals(virtualHostRef)) {
            ServiceReference<VirtualHost> newRef = lookupVirtualHost(bundleContext, virtualHostRef);
            if (newRef != null) {
                vh = bundleContext.getService(newRef);
                if (vh != null) {
                    vhRef = newRef;
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "Using virtual host: {0}", virtualHostRef);
                    }
                } else {
                    Tr.warning(TC, "Configured virtual host {0} could not be resolved, using default_host", virtualHostRef);
                }
            } else {
                Tr.warning(TC, "Configured virtual host {0} not found, using default_host", virtualHostRef);
            }
        }
        this.configuredVirtualHostRef = vhRef;
        this.configuredVirtualHost = vh;

        this.enabled = (Boolean) properties.get(SSLEnabled);
        this.sslId = (String) properties.get(SSLRef);
        this.proxy = (String) properties.get(proxyRef);

        if (ASYNC_RESPONSE_TIMEOUT != null) {
            if (TC.isDebugEnabled()) {
                // Tests will fail if you change the format of this debug message
                Tr.debug(TC, "asyncResponseTimeout setting overridden to " + ASYNC_RESPONSE_TIMEOUT + " by " + ASYNC_RESPONSE_TIMEOUT_PROPERTY + " system property");
            }
            this.asyncResponseTimeout = ASYNC_RESPONSE_TIMEOUT;
        } else {
            this.asyncResponseTimeout = (Long) properties.get(asyncResponseTimeoutRef);
        }

        this.clientAuth = (Boolean) properties.get(clientAuthRef);

        // The propagation attribute is beta: it is hidden from the schema (ibm:beta) and,
        // because the config layer does not enforce that, ignored here unless running a beta edition.
        // Its default is supplied by the public wsAtomicTransaction-1.2 feature, so a value
        // that is present without that feature has been set explicitly and can have no effect.
        String propagationValue = ProductInfo.getBetaEdition() ? (String) properties.get(PROPAGATION) : null;
        this.checkWsdlMode = PROPAGATION_CHECK_WSDL.equals(propagationValue);

        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "SSLEnabled = [{0}], SSLRefId = [{1}], proxy = [{2}], clientAuth = [{3}], asyncResponseTimeout = [{4}], propagation = [{5}]",
                     enabled, sslId, proxy, clientAuth, asyncResponseTimeout, propagationValue);
        }

        INSTANCE = this;

        if (FrameworkState.isStopping()) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Not activating com.ibm.ws.wsat.service.wsatconfigservice because the framework is stopping");
            }
            return;
        }

        // Register the virtual host variable for WABInstaller to resolve
        registerVirtualHostVariable(virtualHostRef);

        String host = getWSATUrl();

        if (enabled) {
            Tr.info(TC, "WSAT_SECURITY_CWLIB0206", sslId);

            if (host.startsWith("http://")) {
                // Let's see how long this takes to rectify
                do {
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "SSL is enabled but the WSAT URL we got is {0}", host);
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                } while ((host = getWSATUrl()).startsWith("http://"));

                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "SSL is enabled and the WSAT URL we now have is {0}", host);
                }
            }
        }

        if (proxy != null && !proxy.equals("")) {
            Tr.info(TC, "WSAT_PROXY_CWLIB0207", proxy);

            if (enabled) {
                if (!proxy.startsWith("https://")) {
                    Tr.error(TC, "WSAT_PROXY_CWLIB0211", proxy);
                }
            } else {
                if (!proxy.startsWith("http://") && !proxy.startsWith("https://")) {
                    Tr.error(TC, "WSAT_PROXY_CWLIB0210", proxy);
                }
            }
        }

        setEndpoints(handler, host);
    }

    /**
     * @return the most recently activated instance
     */
    public static WSATConfigService getInstance() {
        return INSTANCE;
    }

    @Deactivate
    protected void deactivate() {
        // Remove the variable from VariableRegistry on deactivation
        unregisterVirtualHostVariable();

        // Release the directly-obtained VirtualHost service
        if (configuredVirtualHostRef != null) {
            bundleContext.ungetService(configuredVirtualHostRef);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.WSATConfigService#isGlobalEnabled()
     */
    @Override
    @Trivial
    public boolean isSSLEnabled() {
        return enabled;
    }

    @Override
    @Trivial
    public long getAsyncResponseTimeout() {
        return asyncResponseTimeout;
    }

    /**
     * Look up a VirtualHost service by its ID.
     *
     * @param context BundleContext for accessing OSGi services
     * @param virtualHostId The ID of the virtual host to look up
     * @return ServiceReference to the VirtualHost, or null if not found
     */
    private static ServiceReference<VirtualHost> lookupVirtualHost(BundleContext context, String virtualHostId) {
        try {
            String filter = "(&(enabled=true)(id=" + virtualHostId + "))";
            Collection<ServiceReference<VirtualHost>> refs = context.getServiceReferences(VirtualHost.class, filter);

            if (refs != null && !refs.isEmpty()) {
                ServiceReference<VirtualHost> ref = refs.iterator().next();
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Found VirtualHost service for id={0}", virtualHostId);
                }
                return ref;
            } else {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "No VirtualHost service found for id={0}", virtualHostId);
                }
            }
        } catch (Exception e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Exception looking up VirtualHost for id={0}: {1}", virtualHostId, e);
            }
        }
        return null;
    }

    public void setEndpoints(Handler handler, String host) {

        String regHost = host
                         + "/"
                         + Constants.COORDINATION_REGISTRATION_ENDPOINT;
        String coorHost = host
                          + "/"
                          + Constants.COORDINATION_ENDPOINT;
        String partHost = host
                          + "/"
                          + Constants.PARTICIPANT_ENDPOINT;

        EndpointReferenceType localCoorEpr = WSATUtil.createEpr(coorHost);
        EndpointReferenceType localRegEpr = WSATUtil.createEpr(regHost);
        EndpointReferenceType localPartEpr = WSATUtil.createEpr(partHost);

        //set into HandlerService will always self coor...
        handler.setCoordinatorEndpoint(localCoorEpr);
        handler.setRegistrationEndpoint(localRegEpr);
        handler.setParticipantEndpoint(localPartEpr);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxws.wsat.components.WSATConfigService#getSSLReferenceId()
     */
    @Override
    @Trivial
    public String getSSLReferenceId() {
        return sslId;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxws.wsat.components.WSATConfigService#getWSATUrl(boolean)
     */
    @Override
    @Trivial
    public String getWSATUrl() {
        if (proxy != null && proxy.length() > 0)
            return proxy + WSATContextRoot;
        else if (configuredVirtualHost != null)
            // Use the directly-obtained VirtualHost instance for the configured virtual host.
            return configuredVirtualHost.getUrlString(WSATContextRoot, enabled);
        else
            return defaultVirtualHost.getUrlString(WSATContextRoot, enabled);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxws.wsat.components.WSATConfigService#isClientAuthEnabled()
     */
    @Override
    @Trivial
    public boolean isClientAuthEnabled() {
        return clientAuth;
    }

    @Override
    @Trivial
    public boolean isCheckWsdlMode() {
        return checkWsdlMode;
    }

    /**
     * Register the WS-AT virtual host variable in the VariableRegistry.
     * This allows the WABInstaller to resolve ${wsat.webservice.virtualHostRef}
     * when deploying the WS-AT web service bundle.
     *
     * @param virtualHostRef The virtual host ID to register (may be null or empty)
     */
    private void registerVirtualHostVariable(String virtualHostRef) {
        if (variableRegistry != null) {
            // Ensure we always register a valid value - default to "default_host" if not configured
            // This ensures the web container binding and URL construction stay in sync
            String vhostToRegister = (virtualHostRef != null && !virtualHostRef.trim().isEmpty())
                ? virtualHostRef
                : DEFAULT_HOST;

            // Use WS-AT-specific variable name to avoid conflicts
            variableRegistry.addVariable("wsat.webservice.virtualHostRef", vhostToRegister);

            if (TraceComponent.isAnyTracingEnabled() && TC.isDebugEnabled()) {
                Tr.debug(TC, "Registered variable: wsat.webservice.virtualHostRef=" + vhostToRegister);
            }
        } else {
            // VariableRegistry not available - log debug message
            // WABInstaller will use default_host if variable can't be resolved
            if (TraceComponent.isAnyTracingEnabled() && TC.isDebugEnabled()) {
                Tr.debug(TC, "VariableRegistry not available, cannot register wsat.webservice.virtualHostRef");
            }
        }
    }

    /**
     * Remove the WS-AT virtual host variable from the VariableRegistry.
     * Called during component deactivation to clean up.
     */
    private void unregisterVirtualHostVariable() {
        if (variableRegistry != null) {
            variableRegistry.removeVariable("wsat.webservice.virtualHostRef");

            if (TraceComponent.isAnyTracingEnabled() && TC.isDebugEnabled()) {
                Tr.debug(TC, "Removed variable: wsat.webservice.virtualHostRef");
            }
        }
    }
}
