/*******************************************************************************
 * Copyright (c) 2014,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.ibm.ws.jaxrs20.client.configuration.LibertyJaxRsClientConfigInterceptor;
import com.ibm.ws.jaxrs20.client.configuration.LibertyJaxRsClientProxyInterceptor;
import com.ibm.ws.jaxrs20.client.security.LibertyJaxRsClientSSLOutInterceptor;
import com.ibm.ws.jaxrs20.client.security.ltpa.LibertyJaxRsClientLtpaInterceptor;
import com.ibm.ws.jaxrs20.client.security.oauth.LibertyJaxRsClientOAuthInterceptor;
import com.ibm.ws.jaxrs20.client.security.saml.PropagationHandler;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 *
 */
public final class JAXRSClientImpl extends ClientImpl {
    private static final TraceComponent tc = Tr.register(JAXRSClientImpl.class);

    private static final ReferenceQueue<JAXRSClientImpl> referenceQueue = new ReferenceQueue<>();

    private static class ClientWeakReference extends WeakReference<JAXRSClientImpl> {

        final String moduleName;
        volatile boolean wasClosed = false;

        ClientWeakReference(JAXRSClientImpl r, ReferenceQueue<JAXRSClientImpl> queue, String moduleName) {
            super(r, queue);
            this.moduleName = moduleName;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof ClientWeakReference) {
                return get() == ((ClientWeakReference) obj).get();
            }

            return false;
        }
    }

    protected final AtomicBoolean closed = new AtomicBoolean(false);
    protected Set<WebClient> baseClients = Collections.newSetFromMap(new WeakHashMap<WebClient, Boolean>());
    protected boolean hasSSLConfigInfo = false;
    private TLSConfiguration secConfig = null;
    //Defect 202957 move busCache from ClientMetaData to JAXRSClientImpl
    //Before this change, all the WebTarget has same url in a web module share a bus
    //After this change, all the WebTarget has same url in a JAXRSClientImpl share a bus
    private static final Map<String, LibertyApplicationBus> busCache = new ConcurrentHashMap<>();
    private static final Map<String, List<ClientWeakReference>> clientsPerModule = new HashMap<>();

    /**
     * @param config
     * @param secConfig
     */
    JAXRSClientImpl(Configuration config, TLSConfiguration secConfig) {
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
            final BundleContext bc = AccessController.doPrivileged(new PrivilegedExceptionAction<BundleContext>() {

                @Override
                public BundleContext run() throws Exception {
                    Bundle b = FrameworkUtil.getBundle(JAXRSClientImpl.class);
                    return b == null ? null : b.getBundleContext();
                }
            });

            if (bc != null) {
                final List<Object> providers = new ArrayList<>();
                // we don't send feature list for client APIs
                final Set<String> features = Collections.emptySet();

                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

                    @Override
                    public Void run() throws Exception {
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
                        return null;
                    }
                });

                // now that we have a list of providers, register them
                for (Object provider : providers) {
                    if (provider != null) {
                        register(provider);
                    }
                }
            }
        } catch (Throwable ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "<init> failed to find and install declared providers ", ex);
            }
        }

        String moduleName = getModuleName();
        synchronized (clientsPerModule) {
            List<ClientWeakReference> clients = clientsPerModule.get(moduleName);
            if (clients == null) {
                clients = new ArrayList<>();
                clientsPerModule.put(moduleName, clients);
            }
            clients.add(new ClientWeakReference(this, referenceQueue, moduleName));
        }
    }

    private static void poll() {
        ClientWeakReference clientRef;
        while ((clientRef = (ClientWeakReference) referenceQueue.poll()) != null) {
            // if closed was called, do not need to remove it since it was already removed.
            if (!clientRef.wasClosed) {
                synchronized (clientsPerModule) {
                    List<ClientWeakReference> clients = clientsPerModule.get(clientRef.moduleName);

                    if (clients != null) { // the only way this isn't null is if the same client was closed twice
                        clients.remove(clientRef);
                        if (clients.isEmpty()) {
                            for (String id : busCache.keySet()) {
                                if (id.startsWith(clientRef.moduleName) || id.startsWith("unknown:")) {
                                    busCache.remove(id).shutdown(false);
                                }
                            }
                            clientsPerModule.remove(clientRef.moduleName);
                        }
                    }
                }
            }
        }
    }

    static Client newClient(Configuration config, TLSConfiguration secConfig) {
        Client jaxrsClient = new JAXRSClientImpl(config, secConfig);
        poll();
        return jaxrsClient;
    }

    /**
     * override this method, then put our webclient into cxf client API
     */
    @Override
    public WebTarget target(UriBuilder builder) {
        checkClosed();
        WebTargetImpl wt = (WebTargetImpl) super.target(builder);

        //construct our own webclient
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        URI uri = builder.build();
        bean.setAddress(uri.toString());
        WebClient targetClient = bean.createWebClient();

        //get ClientCongfiguration
        ClientConfiguration ccfg = WebClient.getConfig(targetClient);

        //add Liberty Jax-RS Client Config Interceptor to configure things like KeepAlive, timeouts, etc.
        ccfg.getOutInterceptors().add(new LibertyJaxRsClientConfigInterceptor(Phase.PRE_LOGICAL));

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
        String moduleName = getModuleName();
        String id = moduleName + uri.getHost() + "-" + uri.getPort();
        synchronized (busCache) {
            bus = busCache.get(id);
            if (bus == null) {
                bus = LibertyJAXRSClientBusFactory.getInstance().getClientScopeBus(id);
                busCache.put(id, bus);
            }
        }

        ccfg.setBus(bus);

        //add the root WebTarget to managed set so we can close it's associated WebClient
        synchronized (baseClients) {
            baseClients.add(targetClient);
        }

        return new WebTargetImpl(wt.getUriBuilder(), wt.getConfiguration(), targetClient);
    }

    private boolean doClose() {
        boolean notClosed = closed.compareAndSet(false, true);
        if (notClosed) {
            super.close();
            synchronized (baseClients) {
                for (WebClient wc : baseClients) {
                    wc.close();
                }
            }

            baseClients = null;
        }
        return notClosed;
    }

    /**
     * make the cxf ClientImpl.close works, and we should close all clients too
     */
    @Override
    public void close() {
        if (doClose()) {
            String moduleName = getModuleName();
            synchronized (clientsPerModule) {
                List<ClientWeakReference> clients = clientsPerModule.get(moduleName);

                if (clients != null) { // the only way this isn't null is if the same client was closed twice
                    for (int i = 0, size = clients.size(); i < size; ++i) {
                        ClientWeakReference weakRef = clients.get(i);
                        if (weakRef.get() == this) {
                            weakRef.wasClosed = true;
                            clients.remove(i);
                            break;
                        }
                    }
                    if (clients.isEmpty()) {
                        for (String id : busCache.keySet()) {
                            if (id.startsWith(moduleName) || id.startsWith("unknown:")) {
                                busCache.remove(id).shutdown(false);
                            }
                        }
                        clientsPerModule.remove(moduleName);
                    }
                }
            }
        }
        poll();
    }

    public static void closeClients(ModuleMetaData mmd) {
        String moduleName = getModuleName(mmd);
        List<ClientWeakReference> clients = null;
        synchronized (clientsPerModule) {
            clients = clientsPerModule.remove(moduleName);
        }
        if (clients != null) {
            for (ClientWeakReference client : clients) {
                JAXRSClientImpl jaxrsClient = client.get();
                if (jaxrsClient != null) {
                    jaxrsClient.doClose();
                }
            }
            synchronized (clientsPerModule) {
                for (String id : busCache.keySet()) {
                    if (id.startsWith(moduleName) || id.startsWith("unknown:")) {
                        busCache.remove(id).shutdown(false);
                    }
                }
            }
        }
    }

    /**
     * @return busCache
     */
    public Map<String, LibertyApplicationBus> getBusCache() {
        return busCache;
    }

    @Override
    public Client property(String name, @Sensitive Object value) {
        checkClosed();
        // need to convert proxy password to ProtectedString
        if (JAXRSClientConstants.PROXY_PASSWORD.equals(name) && value != null &&
            !(value instanceof ProtectedString)) {
            return super.property(name, new ProtectedString(value.toString().toCharArray()));
        }
        return super.property(name, value);
    }

    private String getModuleName() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd != null) {
            ModuleMetaData mmd = cmd.getModuleMetaData();
            if (mmd != null) {
                return getModuleName(mmd);
            }
        }
        return "unknown:";
    }

    private static String getModuleName(ModuleMetaData mmd) {
        return mmd.getName() + ":";
    }

    private void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException("client is closed");
        }
    }
}
