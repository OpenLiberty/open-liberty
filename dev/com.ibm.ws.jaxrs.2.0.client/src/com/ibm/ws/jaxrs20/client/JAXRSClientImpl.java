/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.spec.ClientImpl;
import org.apache.cxf.jaxrs.client.spec.TLSConfiguration;
import org.apache.cxf.phase.Phase;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBus;
import com.ibm.ws.jaxrs20.client.bus.LibertyJAXRSClientBusFactory;
import com.ibm.ws.jaxrs20.client.configuration.LibertyJaxRsClientProxyInterceptor;
import com.ibm.ws.jaxrs20.client.configuration.LibertyJaxRsClientTimeOutInterceptor;
import com.ibm.ws.jaxrs20.client.security.LibertyJaxRsClientSSLOutInterceptor;
import com.ibm.ws.jaxrs20.client.security.ltpa.LibertyJaxRsClientLtpaInterceptor;
import com.ibm.ws.jaxrs20.client.security.oauth.LibertyJaxRsClientOAuthInterceptor;
import com.ibm.ws.jaxrs20.client.security.saml.PropagationHandler;
import com.ibm.ws.jaxrs20.client.util.JaxRSClientUtil;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

/**
 *
 */
public class JAXRSClientImpl extends ClientImpl {
    private static final TraceComponent tc = Tr.register(JAXRSClientImpl.class);

    protected boolean closed;
    protected Set<WebClient> baseClients = new HashSet<WebClient>();
    protected boolean hasSSLConfigInfo = false;
    private TLSConfiguration secConfig = null;
    //Defect 202957 move busCache from ClientMetaData to JAXRSClientImpl
    //Before this change, all the WebTarget has same url in a web module share a bus
    //After this change, all the WebTarget has same url in a JAXRSClientImpl share a bus
    private final Map<String, LibertyApplicationBus> busCache = new ConcurrentHashMap<String, LibertyApplicationBus>();

    /**
     * @param config
     * @param secConfig
     */
    public JAXRSClientImpl(Configuration config, TLSConfiguration secConfig) {
        super(config, secConfig);
        this.secConfig = secConfig;
        /**
         * check if there is any user's programmed SSLContext info
         */
        TLSClientParameters ttClientParams = secConfig.getTlsClientParams();
        if (secConfig.getSslContext() != null
            ||
            ((ttClientParams.getTrustManagers() != null && ttClientParams.getTrustManagers().length > 0) && (ttClientParams.getKeyManagers() != null && ttClientParams.getKeyManagers().length > 0))) {
            hasSSLConfigInfo = true;
        }

        try {
            Bundle b = FrameworkUtil.getBundle(JAXRSClientImpl.class);
            BundleContext bc = b == null ? null : b.getBundleContext();
            if (bc != null) {
                final List<Object> providers = new ArrayList<>();
                // we don't send feature list for client APIs
                final Set<String> features = Collections.emptySet();

                Collection<ServiceReference<JaxRsProviderRegister>> refs = bc.getServiceReferences(JaxRsProviderRegister.class, null);

                for (ServiceReference<JaxRsProviderRegister> ref : refs) {
                    JaxRsProviderRegister providerRegister = bc.getService(ref);
                    try {
                        providerRegister.installProvider(true, providers, features);
                    } catch (Throwable t) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            String providerRegisterBundleLoc = ref.getBundle() == null ? "unknown" : ref.getBundle().getSymbolicName() + " " + ref.getBundle().getVersion();
                            Tr.debug(tc, "<init> failed to install providers from " + providerRegister.getClass().getName() +
                                         " loaded from " + providerRegisterBundleLoc,
                                     t);
                        }
                    }
                }
                // now that we have a list of providers, register them
                for (Object provider : providers) {
                    if (provider != null) {
                        register(provider);
                    }
                }
            }
        } catch (Exception ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "<init> failed to find and install declared providers ", ex);
            }
        }
    }

    /**
     * override this method, then put our webclient into cxf client API
     */
    @Override
    public WebTarget target(UriBuilder builder) {
        WebTargetImpl wt = (WebTargetImpl) super.target(builder);

        //construct our own webclient
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        URI uri = builder.build();
        bean.setAddress(uri.toString());
        WebClient targetClient = bean.createWebClient();

        //get ClientCongfiguration
        ClientConfiguration ccfg = WebClient.getConfig(targetClient);

        //add Liberty Jax-RS Client Timeout Interceptor to configure the timeout
        ccfg.getOutInterceptors().add(new LibertyJaxRsClientTimeOutInterceptor(Phase.PRE_LOGICAL));

        //add Liberty Jax-RS Client Proxy Interceptor to configure the proxy
        ccfg.getOutInterceptors().add(new LibertyJaxRsClientProxyInterceptor(Phase.PRE_LOGICAL));

        //add Liberty Ltpa handler Interceptor to check if is using ltpa token for sso
        ccfg.getOutInterceptors().add(new LibertyJaxRsClientLtpaInterceptor());

        //add Liberty Jax-RS OAuth Interceptor to check whether it has to propagate OAuth/access token
        ccfg.getOutInterceptors().add(new LibertyJaxRsClientOAuthInterceptor());

        //add  Interceptor to check whether it has to propagate SAML token for sso
        ccfg.getOutInterceptors().add(new PropagationHandler());

        /**
         * if no any user programmed SSL context info
         * put the LibertyJaxRsClientSSLOutInterceptor into client OUT interceptor chain
         * see if Liberty SSL can help
         */
        if (hasSSLConfigInfo == false) {
            LibertyJaxRsClientSSLOutInterceptor sslOutInterceptor = new LibertyJaxRsClientSSLOutInterceptor(Phase.PRE_LOGICAL);
            sslOutInterceptor.setTLSConfiguration(secConfig);
            ccfg.getOutInterceptors().add(sslOutInterceptor);
        }
        //set bus
        LibertyApplicationBus bus;
        //202957 same url use same bus, add a lock to busCache to ensure only one bus will be created in concurrent mode.
        //ConcurrentHashMap can't ensure that.
        String id = JaxRSClientUtil.convertURItoBusId(uri.toString());
        synchronized (busCache) {
            bus = busCache.get(id);
            if (bus == null) {
                bus = LibertyJAXRSClientBusFactory.getInstance().getClientScopeBus(id);
                busCache.put(id, bus);
            }
        }

        ccfg.setBus(bus);

        //add the webclient to managed set
        this.baseClients.add(targetClient);

        return new WebTargetImpl(wt.getUriBuilder(), wt.getConfiguration(), targetClient);
    }

    /**
     * make the cxf ClientImpl.close works, and we should close all clients too
     */
    @Override
    public void close() {
        super.close();
        for (WebClient wc : baseClients) {
//defec 202957 don't need bus counter any more, since the bus is not shared between jaxrs client any more
            //if one webclient is closed, check if its bus is not used by any other webclient, and reduce counter
//            String id = JaxRSClientUtil.convertURItoBusId(wc.getBaseURI().toString());
//            synchronized (busCache) {
//                LibertyApplicationBus bus = busCache.get(id);
//                if (bus != null) {
//                    AtomicInteger ai = bus.getBusCounter();
//                    if (ai != null) {
//                        //if no webclient uses the bus at this moment, then remove & release it
//                        if (ai.decrementAndGet() == 0) {
//                            busCache.remove(id);
//                            //release bus
//                            bus.shutdown(false);
//                        }
//                    }
//                }
//
//            }

            //close webclient
            wc.close();

        }
        for (LibertyApplicationBus bus : busCache.values()) {
            bus.shutdown(false);
        }
        busCache.clear();
        baseClients = null;
    }

    /**
     * @return busCache
     */
    public Map<String, LibertyApplicationBus> getBusCache() {
        return busCache;
    }

    @Override
    public Client property(String name, @Sensitive Object value) {
        // need to convert proxy password to ProtectedString
        if (JAXRSClientConstants.PROXY_PASSWORD.equals(name) && value != null &&
            !(value instanceof ProtectedString)) {
            return super.property(name, new ProtectedString(value.toString().toCharArray()));
        }
        return super.property(name, value);
    }
}
