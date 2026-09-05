/*
 * Copyright (c) 2014, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.transaction;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.transaction.TransactionManager;

import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.tx.remote.RemoteTransactionController;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionHandlerContext;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionProtocolProvider;

@Component(configurationPolicy = IGNORE, property = { "service.ranking:Integer=2" })
public class TransactionSubsystemFactory implements SubsystemFactory {
    private static final TraceComponent tc = Tr.register(TransactionSubsystemFactory.class, "IIOP", null);
    
    /**
     * Static reference to the active factory instance.
     * This is needed because the ORB's LocalFactory mechanism may create new instances
     * of MyLocalFactory, so we need a static way to access the factory.
     */
    private static volatile TransactionSubsystemFactory activeFactory;
    
    private static class MyLocalFactory implements LocalFactory {
        public Class<?> forName(String name) throws ClassNotFoundException {
            return TransactionInitializer.class;
        }
        
        @SuppressWarnings("rawtypes")
        public Object newInstance(Class cls) throws InstantiationException, IllegalAccessException {
            return new TransactionInitializer(activeFactory);
        }
    }

    private Register providerRegistry;
    private ServiceProvider transactionInitializerClass;

    private TransactionManager transactionManager;
    private RemoteTransactionController remoteTransactionController;

    /**
     * Map of protocol providers, keyed by IOR tag ID for uniqueness checking.
     * ConcurrentHashMap provides thread-safe access without locking.
     */
    private final Map<Integer, TransactionProtocolProvider> providers = new ConcurrentHashMap<>();

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Reference
    protected void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        // Try to initialize locator now that we have a service
        tryInitializeLocator();
    }
    
    @Reference
    protected void setRemoteTransactionController(RemoteTransactionController remoteTransactionController) {
        this.remoteTransactionController = remoteTransactionController;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Set RemoteTransactionController: {0}", remoteTransactionController);
        }
        // Try to initialize locator now that we have a service
        tryInitializeLocator();
    }
    
    @Reference(service = TransactionProtocolProvider.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void addTransactionProtocolProvider(TransactionProtocolProvider provider) {
        providers.put(provider.getIORTagId(), provider);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Registered protocol provider: {0} (tag: 0x{1})",
                    provider.getProtocolName(),
                    Integer.toHexString(provider.getIORTagId()));
        }
    }

    protected void removeTransactionProtocolProvider(TransactionProtocolProvider provider) {
        providers.remove(provider.getIORTagId());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Unregistered protocol provider: {0} (tag: 0x{1})",
                    provider.getProtocolName(),
                    Integer.toHexString(provider.getIORTagId()));
        }
    }

    /**
     * Get the TransactionManager for this subsystem.
     *
     * @return the TransactionManager, or null if not available
     */
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Get the active factory instance. This is called by IORTransactionInterceptor
     * at runtime to retrieve protocol contributors.
     *
     * @return the active factory, or null if not yet activated
     */
    public static TransactionSubsystemFactory getActiveFactory() {
        return activeFactory;
    }

    /**
     * Returns all registered providers sorted by priority (lowest value first).
     * Used by IORTransactionInterceptor and ClientTransactionInterceptor.
     */
    public List<TransactionProtocolProvider> getSortedProviders() {
        List<TransactionProtocolProvider> list = new java.util.ArrayList<>(providers.values());
        list.sort(java.util.Comparator.comparingInt(TransactionProtocolProvider::getPriority));
        return list;
    }

    /**
     * Creates a service locator with current services.
     *
     * This is called during activation to eagerly initialize the locator,
     * or by TransactionServiceLocator.getInstance() for lazy initialization.
     *
     * @return a new service locator, or null if services not yet available
     */
    public TransactionServiceLocator createServiceLocator() {
        if (transactionManager == null || remoteTransactionController == null) {
            // Services not yet injected
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Cannot create service locator - services not yet available");
            }
            return null;
        }
        
        TransactionHandlerContext context = new TransactionHandlerContextImpl(
            remoteTransactionController,
            transactionManager
        );

        return TransactionServiceLocator.create(context, providers);
    }
    
    /**
     * Attempts to initialize the service locator if not already initialized
     * and all required services are available.
     *
     * This is called when services are injected to eagerly initialize the locator.
     */
    private void tryInitializeLocator() {
        if (TransactionServiceLocator.getInstance() == null) {
            TransactionServiceLocator locator = createServiceLocator();
            if (locator != null) {
                TransactionServiceLocator.setInstance(locator);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Initialized service locator after service injection");
                }
            }
        }
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        // Set the static reference so MyLocalFactory can access it
        activeFactory = this;
        
        // Try to eagerly initialize service locator
        // (will succeed if services are already injected, otherwise lazy init will handle it)
        TransactionServiceLocator locator = createServiceLocator();
        if (locator != null) {
            TransactionServiceLocator.setInstance(locator);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Eagerly initialized service locator");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Services not yet available - service locator will lazy initialize");
            }
        }
        
        transactionInitializerClass = new ServiceProvider(new MyLocalFactory(), TransactionInitializer.class);
        providerRegistry.registerProvider(transactionInitializerClass);
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregisterProvider(transactionInitializerClass);
        // Clear the service locator
        TransactionServiceLocator.clearInstance();
        // Clear the static reference
        activeFactory = null;
    }

    @Override
    public Policy getTargetPolicy(ORB orb, Map<String, Object> properties, Map<String, Object> extraConfig) throws Exception {
        // Create lightweight, truly serializable policy with just configuration
        boolean enabled = true;  // Could be extracted from properties if needed
        int timeout = 30;        // Could be extracted from properties if needed
        
        ServerTransactionPolicyConfig config = new ServerTransactionPolicyConfig(enabled, timeout);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created server transaction policy with config: {0}", config);
        }
        
        return new ServerTransactionPolicy(config);
    }

    /**
     * Create a client transaction policy for the ORB.
     * The policy is stateless and serves as an enablement gate.
     *
     * @param orb the ORB instance
     * @param properties configuration properties (unused)
     * @return stateless ClientTransactionPolicy
     * @throws Exception if policy creation fails
     */
    @Override
    public Policy getClientPolicy(ORB orb, Map<String, Object> properties) throws Exception {
        // Create stateless policy as enablement gate
        return new ClientTransactionPolicy();
    }

    @Override
    public String getInitializerClassName(boolean endpoint) {
        return TransactionInitializer.class.getName();
    }
}
