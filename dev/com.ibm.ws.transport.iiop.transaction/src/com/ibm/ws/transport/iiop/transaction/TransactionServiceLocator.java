/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop.transaction;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionHandlerContext;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionProtocolProvider;

/**
 * Service locator that provides access to transaction services for IIOP interceptors.
 *
 * This class uses a singleton pattern with lazy initialization to provide access to
 * transaction services (TransactionManager, RemoteTransactionController, and protocol
 * providers) that are injected into TransactionSubsystemFactory via OSGi.
 *
 * The locator is initialized during factory activation and accessed by interceptors
 * at request time. This separates service access from policy configuration, allowing
 * policies to be truly serializable while services remain accessible.
 *
 * Thread Safety:
 * - Uses double-checked locking for lazy initialization
 * - Volatile instance field ensures visibility across threads
 * - Immutable after initialization (context and providers map reference)
 */
public class TransactionServiceLocator {
    private static final TraceComponent tc = Tr.register(TransactionServiceLocator.class, "IIOP", null);

    private static volatile TransactionServiceLocator instance;

    private final TransactionHandlerContext context;
    private final Map<Integer, TransactionProtocolProvider> providers;

    /**
     * Private constructor - use getInstance() or setInstance().
     *
     * @param context   the transaction handler context providing access to TransactionManager
     *                  and RemoteTransactionController
     * @param providers the map of protocol providers (shared reference to factory's map)
     */
    private TransactionServiceLocator(TransactionHandlerContext context,
                                      Map<Integer, TransactionProtocolProvider> providers) {
        this.context = context;
        this.providers = providers;
    }

    /**
     * Gets the service locator instance, initializing it from the factory if needed.
     *
     * This method uses double-checked locking to ensure thread-safe lazy initialization.
     * If the instance is not yet set, it attempts to create one from the active factory.
     *
     * @return the service locator, or null if factory is not yet available
     */
    public static TransactionServiceLocator getInstance() {
        TransactionServiceLocator result = instance;
        if (result == null) {
            synchronized (TransactionServiceLocator.class) {
                result = instance;
                if (result == null) {
                    // Try to initialize from factory
                    TransactionSubsystemFactory factory = TransactionSubsystemFactory.getActiveFactory();
                    if (factory != null) {
                        result = factory.createServiceLocator();
                        if (result != null) {
                            instance = result;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Lazy initialized service locator from factory");
                            }
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Factory not yet available for lazy initialization");
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Explicitly sets the service locator instance.
     *
     * This is called by TransactionSubsystemFactory during activation to eagerly
     * initialize the locator when services are available.
     *
     * @param locator the service locator to set
     */
    public static void setInstance(TransactionServiceLocator locator) {
        instance = locator;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Service locator instance set: {0}", locator != null);
        }
    }

    /**
     * Clears the service locator instance.
     *
     * This is called by TransactionSubsystemFactory during deactivation to clean up.
     */
    public static void clearInstance() {
        instance = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Service locator instance cleared");
        }
    }

    /**
     * Gets the transaction handler context.
     *
     * The context provides access to TransactionManager and RemoteTransactionController
     * services that are needed by import handlers.
     *
     * @return the transaction handler context, never null
     */
    public TransactionHandlerContext getContext() {
        return context;
    }

    /**
     * Gets all registered protocol providers (unmodifiable live view).
     * Used by ServerTransactionInterceptor for the import loop.
     *
     * @return unmodifiable view of the providers map (IOR tag ID to provider)
     */
    public Map<Integer, TransactionProtocolProvider> getProviders() {
        return Collections.unmodifiableMap(providers);
    }

    /**
     * Returns all registered providers sorted by priority (lowest value first).
     * Used by IORTransactionInterceptor and ClientTransactionInterceptor.
     * Sorting on every call is acceptable — N is always very small (typically 1).
     */
    public List<TransactionProtocolProvider> getSortedProviders() {
        List<TransactionProtocolProvider> list = new java.util.ArrayList<>(providers.values());
        list.sort(java.util.Comparator.comparingInt(TransactionProtocolProvider::getPriority));
        return list;
    }

    /**
     * Package-private factory method for creating a locator.
     *
     * This is called by TransactionSubsystemFactory to create locator instances.
     *
     * @param context   the transaction handler context
     * @param providers the map of protocol providers
     * @return a new service locator instance
     */
    static TransactionServiceLocator create(TransactionHandlerContext context,
                                            Map<Integer, TransactionProtocolProvider> providers) {
        return new TransactionServiceLocator(context, providers);
    }
}

// Made with Bob
