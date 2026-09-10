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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.TransactionManager;

import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
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

    private final Register providerRegistry;
    private ServiceProvider transactionInitializerClass;

    private final TransactionManager transactionManager;
    private final RemoteTransactionController remoteTransactionController;

    /**
     * Map of protocol providers, keyed by IOR tag ID for uniqueness checking.
     * ConcurrentHashMap provides thread-safe access without locking.
     * This map is read live by getProviders() so that providers arriving after
     * activation (e.g. WSATTransactionProtocolProvider, whose Handler dependency
     * may not be satisfied until after this component activates) are not silently
     * dropped.
     */
    private final Map<Integer, TransactionProtocolProvider> providers = new ConcurrentHashMap<>();

    @Activate
    public TransactionSubsystemFactory(
            @Reference Register providerRegistry,
            @Reference TransactionManager transactionManager,
            @Reference RemoteTransactionController remoteTransactionController) {
        this.providerRegistry = providerRegistry;
        this.transactionManager = transactionManager;
        this.remoteTransactionController = remoteTransactionController;

        activeFactory = this;

        TransactionServiceLocator locator = createServiceLocator();
        if (locator != null) {
            TransactionServiceLocator.setInstance(locator);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Eagerly initialized service locator");
            }
        }

        transactionInitializerClass = new ServiceProvider(new MyLocalFactory(), TransactionInitializer.class);
        providerRegistry.registerProvider(transactionInitializerClass);
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
     * Returns all currently registered protocol providers as an immutable snapshot.
     * Reads from the live ConcurrentHashMap on every call so that providers that
     * arrive after activation (e.g. WSATTransactionProtocolProvider) are included.
     */
    public List<TransactionProtocolProvider> getProviders() {
        return Collections.unmodifiableList(new ArrayList<>(providers.values()));
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
        TransactionHandlerContext context = new TransactionHandlerContextImpl(
            remoteTransactionController,
            transactionManager
        );

        return TransactionServiceLocator.create(context, providers);
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
