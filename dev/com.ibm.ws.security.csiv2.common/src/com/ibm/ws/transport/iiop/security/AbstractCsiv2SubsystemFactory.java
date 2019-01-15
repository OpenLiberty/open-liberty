/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.security;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.csiv2.config.ssl.SSLConfig;
import com.ibm.ws.security.csiv2.util.SecurityServices;
import com.ibm.ws.ssl.optional.SSLSupportOptional;
import com.ibm.ws.transport.iiop.security.config.ssl.yoko.SocketFactory;
import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.ws.transport.iiop.spi.ReadyListener;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;
import com.ibm.wsspi.ssl.SSLSupport;
import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public abstract class AbstractCsiv2SubsystemFactory extends SubsystemFactory {
    private static final TraceComponent tc = Tr.register(AbstractCsiv2SubsystemFactory.class);
    protected static final long TIMEOUT_SECONDS = 10;

    private enum MyLocalFactory implements LocalFactory {
        INSTANCE;
        @Override
        public Class<?> forName(String clsName) throws ClassNotFoundException {
            return Class.forName(clsName);
        }

        @Override
        public Object newInstance(Class cls) throws InstantiationException, IllegalAccessException {
            return cls.newInstance();
        }
    }

    private Register providerRegistry;
    private ServiceProvider securityInitializerClass;
    private ServiceProvider connectionHelperClass;
    private SSLSupport sslSupport;
    private ScheduledExecutorService executor;
    protected String defaultAlias;
    private Collection<String> sslRefs = Collections.emptyList();
    private final List<ReadyRegistration> regs = new ArrayList<ReadyRegistration>();

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Reference
    protected void setSSLSupport(SSLSupportOptional sslSupport, Map<String, Object> props) {
        this.sslSupport = sslSupport;
        defaultAlias = (String) props.get("sslRef");
        String[] repertoireIds = (String[]) props.get(SSLSupportOptional.REPERTOIRE_IDS);
        if (repertoireIds != null) {
            sslRefs = Arrays.asList(repertoireIds);
        }
        //no updateRegistered necessary here since this is before activate, there can be no ReadyRegistrations yet.
    }

    protected void updatedSSLSupport(SSLSupportOptional sslSupport, Map<String, Object> props) {
        String[] repertoireIds = (String[]) props.get(SSLSupportOptional.REPERTOIRE_IDS);
        synchronized (this) {
            if (repertoireIds != null) {
                sslRefs = Arrays.asList(repertoireIds);
            } else {
                sslRefs = Collections.emptyList();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Known ssl configurations: {0}", sslRefs);
        }
        updateRegistered();
    }

    @Reference
    protected void setScheduledExecutorService(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        securityInitializerClass = new ServiceProvider(MyLocalFactory.INSTANCE, SecurityInitializer.class);
        connectionHelperClass = new ServiceProvider(MyLocalFactory.INSTANCE, SocketFactory.class);
        providerRegistry.registerProvider(securityInitializerClass);
        providerRegistry.registerProvider(connectionHelperClass);
        SecurityServices.setupSSLConfig(new SSLConfig(sslSupport.getJSSEHelper()));
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregisterProvider(securityInitializerClass);
        providerRegistry.unregisterProvider(connectionHelperClass);
        SecurityServices.clean();
    }

    /** {@inheritDoc} */
    @Override
    public String getInitializerClassName(boolean endpoint) {
        return SecurityInitializer.class.getName();
    }

    /** {@inheritDoc} */
    @Override
    public void register(ReadyListener listener, Map<String, Object> properties, List<IIOPEndpoint> endpoints) {
        ReadyRegistration rr = new ReadyRegistration(extractSslRefs(properties, endpoints), listener);
        regs.add(rr);
        rr.check();
    }

    /**
     * @param properties
     * @param endpoints
     * @return
     */
    protected abstract Set<String> extractSslRefs(Map<String, Object> properties, List<IIOPEndpoint> endpoints);

    /** {@inheritDoc} */
    @Override
    public void unregister(ReadyListener listener) {
        for (ReadyRegistration rr : regs) {
            if (rr.listener == listener) {
                regs.remove(rr);
                rr.cancelTimeout();
                break;
            }
        }
    }

    protected void updateRegistered() {
        for (ReadyRegistration rr : regs) {
            rr.check();
        }
    }

    protected void timeoutMessage(Set<String> requiredSslRefs, ReadyListener listener) {
        Set<String> missing = new HashSet<String>(requiredSslRefs);
        synchronized (AbstractCsiv2SubsystemFactory.this) {
            missing.removeAll(sslRefs);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Known ssl configurations: {0}", sslRefs);
        }
        Tr.error(tc, "SSL_SERVICE_NOT_STARTED", missing, listener.listenerId(), TIMEOUT_SECONDS);
    }

    protected boolean check(Collection<String> requiredSslRefs) {
        boolean containsAll = sslRefs.containsAll(requiredSslRefs);
        return containsAll;
    }

    private class ReadyRegistration {

        private final Set<String> requiredSslRefs;
        private final ReadyListener listener;
        private ScheduledFuture<?> future;

        /**
         * @param ssf
         * @param requiredSslRefs
         * @param listener
         */
        public ReadyRegistration(Set<String> requiredSslRefs, ReadyListener listener) {
            super();
            this.requiredSslRefs = requiredSslRefs;
            this.listener = listener;
            scheduleTimeout();
        }

        protected void scheduleTimeout() {
            this.future = executor.schedule(new Runnable() {

                @Override
                public void run() {
                    timeoutMessage(requiredSslRefs, listener);
                }
            }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        void check() {
            boolean containsAll = AbstractCsiv2SubsystemFactory.this.check(requiredSslRefs);
            listener.readyChanged(AbstractCsiv2SubsystemFactory.this, containsAll);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Check: Known ssl configurations: {0}, required: {1}, containsAll: {2} timeout exists: {3}", sslRefs, requiredSslRefs, containsAll, future != null);
            }
            synchronized (this) {
                if (containsAll) {
                    cancelTimeout();
                } else if (future == null) {
                    scheduleTimeout();
                }
            }
        }

        public void cancelTimeout() {
            if (future != null) {
                future.cancel(false);
                future = null;
            }
        }

    }

}
