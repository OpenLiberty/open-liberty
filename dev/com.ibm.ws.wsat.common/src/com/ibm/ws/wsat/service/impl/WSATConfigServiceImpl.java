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

import javax.xml.soap.SOAPException;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.jaxws.wsat.components.WSATConfigService;
import com.ibm.ws.wsat.service.Handler;
import com.ibm.ws.wsat.service.WSATUtil;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

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
    private static final String WSATContextRoot = "/ibm/wsatservice";

    private static final String HTTPCONFIGSERVICE_REFERENCE_NAME = "httpOptions";
    private static final String WSATHANDLERSERVICE_REFERENCE_NAME = "handler";
    private static final String VARIABLEREGISTRY_REFERENCE_NAME = "variableRegistry";

    private static final AtomicServiceReference<VirtualHost> httpOptions = new AtomicServiceReference<VirtualHost>(HTTPCONFIGSERVICE_REFERENCE_NAME);
    private static final AtomicServiceReference<Handler> handlerService = new AtomicServiceReference<Handler>(WSATHANDLERSERVICE_REFERENCE_NAME);
    private static final AtomicServiceReference<VariableRegistry> variableRegistryRef = new AtomicServiceReference<VariableRegistry>(VARIABLEREGISTRY_REFERENCE_NAME);

    private boolean enabled;
    private String sslId;
    private String proxy;
    private long asyncResponseTimeout;
    private boolean clientAuth;
    private String configuredVirtualHostId = "default_host";
    private ServiceReference<VirtualHost> configuredVirtualHostRef = null;

    private static WSATConfigService INSTANCE;

    public WSATConfigServiceImpl() {
        INSTANCE = this;
    }

    /**
     * @return
     */
    public static WSATConfigService getInstance() {
        return INSTANCE;
    }

    @Reference(name = WSATHANDLERSERVICE_REFERENCE_NAME, service = Handler.class)
    protected void setHandlerService(ServiceReference<Handler> ref) {
        handlerService.setReference(ref);
    }

    @Reference(name = HTTPCONFIGSERVICE_REFERENCE_NAME, service = VirtualHost.class,
               target = "(&(enabled=true)(id=default_host))",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setHttpOptions(ServiceReference<VirtualHost> ref) {
        httpOptions.setReference(ref);
    }

    protected void unsetHttpOptions(ServiceReference<VirtualHost> ref) {
        httpOptions.unsetReference(ref);
    }

    @Reference(name = VARIABLEREGISTRY_REFERENCE_NAME,
               service = VariableRegistry.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setVariableRegistry(ServiceReference<VariableRegistry> ref) {
        variableRegistryRef.setReference(ref);
    }

    protected void unsetVariableRegistry(ServiceReference<VariableRegistry> ref) {
        variableRegistryRef.unsetReference(ref);
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

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) throws SOAPException {
        if (!FrameworkState.isStopping()) {
            modified(cc, properties);
        } else {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Not activating com.ibm.ws.wsat.service.wsatconfigservice because the framework is stopping");
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        // Remove the variable from VariableRegistry on deactivation
        unregisterVirtualHostVariable();
        
        httpOptions.deactivate(cc);
        variableRegistryRef.deactivate(cc);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> properties) throws SOAPException {
        httpOptions.activate(cc);
        handlerService.activate(cc);
        variableRegistryRef.activate(cc);
        
        // Read the configured virtual host reference from server.xml
        String virtualHostRef = (String) properties.get("virtualHostRef");
        if (virtualHostRef == null || virtualHostRef.isEmpty()) {
            virtualHostRef = "default_host";  // Use default if not configured
        }
        
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Configured virtual host: {0}", virtualHostRef);
        }
        
        // Register the virtual host variable for WABInstaller to resolve
        registerVirtualHostVariable(virtualHostRef);
        
        // If the configured virtual host is different from what we're currently using,
        // look it up and update our reference
        if (!virtualHostRef.equals(configuredVirtualHostId)) {
            configuredVirtualHostId = virtualHostRef;
            
            if (!"default_host".equals(virtualHostRef)) {
                // Look up the configured virtual host
                ServiceReference<VirtualHost> newRef = lookupVirtualHost(cc, virtualHostRef);
                if (newRef != null) {
                    // Update to use the configured virtual host
                    if (configuredVirtualHostRef != null) {
                        httpOptions.unsetReference(configuredVirtualHostRef);
                    }
                    httpOptions.setReference(newRef);
                    configuredVirtualHostRef = newRef;
                    
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "Updated to use virtual host: {0}", virtualHostRef);
                    }
                } else {
                    Tr.warning(TC, "Configured virtual host {0} not found, using default_host", virtualHostRef);
                    configuredVirtualHostId = "default_host";
                }
            }
        }

        enabled = (Boolean) properties.get(SSLEnabled);
        sslId = (String) properties.get(SSLRef);
        proxy = (String) properties.get(proxyRef);

        if (ASYNC_RESPONSE_TIMEOUT != null) {
            if (TC.isDebugEnabled()) {
                // Tests will fail if you change the format of this debug message
                Tr.debug(TC, "asyncResponseTimeout setting overridden to " + ASYNC_RESPONSE_TIMEOUT + " by " + ASYNC_RESPONSE_TIMEOUT_PROPERTY + " system property");
            }
            asyncResponseTimeout = ASYNC_RESPONSE_TIMEOUT;
        } else {
            asyncResponseTimeout = (Long) properties.get(asyncResponseTimeoutRef);
        }

        clientAuth = (Boolean) properties.get(clientAuthRef);

        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "SSLEnabled = [{0}], SSLRefId = [{1}], proxy = [{2}], clientAuth = [{3}], asyncResponseTimeout = [{4}]", enabled, sslId, proxy, clientAuth,
                     asyncResponseTimeout);
        }

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

        setEndpoints(handlerService.getService(), host);
    }

    /**
     * Look up a VirtualHost service by its ID.
     *
     * @param cc ComponentContext for accessing OSGi services
     * @param virtualHostId The ID of the virtual host to look up
     * @return ServiceReference to the VirtualHost, or null if not found
     */
    private ServiceReference<VirtualHost> lookupVirtualHost(ComponentContext cc, String virtualHostId) {
        try {
            String filter = "(&(enabled=true)(id=" + virtualHostId + "))";
            Collection<ServiceReference<VirtualHost>> refs =
                cc.getBundleContext().getServiceReferences(VirtualHost.class, filter);
            
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
        else
            return httpOptions.getService().getUrlString(WSATContextRoot, enabled);
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

    /**
     * Register the WS-AT virtual host variable in the VariableRegistry.
     * This allows the WABInstaller to resolve ${wsat.webservice.virtualHostRef}
     * when deploying the WS-AT web service bundle.
     *
     * @param virtualHostRef The virtual host ID to register (may be null or empty)
     */
    private void registerVirtualHostVariable(String virtualHostRef) {
        VariableRegistry varReg = variableRegistryRef.getService();
        if (varReg != null) {
            // Ensure we always register a valid value - default to "default_host" if not configured
            // This ensures the web container binding and URL construction stay in sync
            String vhostToRegister = (virtualHostRef != null && !virtualHostRef.trim().isEmpty())
                ? virtualHostRef
                : "default_host";
            
            // Use WS-AT-specific variable name to avoid conflicts
            varReg.addVariable("wsat.webservice.virtualHostRef", vhostToRegister);
            
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
        VariableRegistry varReg = variableRegistryRef.getService();
        if (varReg != null) {
            varReg.removeVariable("wsat.webservice.virtualHostRef");
            
            if (TraceComponent.isAnyTracingEnabled() && TC.isDebugEnabled()) {
                Tr.debug(TC, "Removed variable: wsat.webservice.virtualHostRef");
            }
        }
    }
}
