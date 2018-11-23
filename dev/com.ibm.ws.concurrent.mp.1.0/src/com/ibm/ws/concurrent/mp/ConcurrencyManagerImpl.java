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
package com.ibm.ws.concurrent.mp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.ServiceLoader;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.concurrent.mp.context.WLMContextProvider;

/**
 * Concurrency manager, which includes the collection of ThreadContextProviders
 * for a particular class loader.
 */
public class ConcurrencyManagerImpl implements ConcurrencyManager {
    private static final TraceComponent tc = Tr.register(ConcurrencyManagerImpl.class);

    final ConcurrencyProviderImpl concurrencyProvider;

    /**
     * List of available thread context providers.
     * Container-implemented context providers are ordered according to their prerequisites.
     * This is the order in which thread context should be captured and applied to threads.
     * It is the reverse of the order in which thread context is restored on threads.
     */
    final ArrayList<ThreadContextProvider> contextProviders = new ArrayList<ThreadContextProvider>();

    /**
     * Merge built-in thread context providers from the container with those found
     * on the class loader, detecting any duplicate provider types.
     *
     * @param concurrencyProvider the registered concurrency provider
     * @param classloader the class loader from which to discover thread context providers
     */
    ConcurrencyManagerImpl(ConcurrencyProviderImpl concurrencyProvider, ClassLoader classloader) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        this.concurrencyProvider = concurrencyProvider;

        // Thread context types for which providers are available
        HashSet<String> available = new HashSet<String>();

        // Built-in thread context providers (always available)
        contextProviders.add(concurrencyProvider.applicationContextProvider);
        available.add(ThreadContext.APPLICATION);
        contextProviders.add(concurrencyProvider.securityContextProvider);
        available.add(ThreadContext.SECURITY);
        contextProviders.add(concurrencyProvider.transactionContextProvider);
        available.add(ThreadContext.TRANSACTION);
        contextProviders.add(concurrencyProvider.wlmContextProvider);
        available.add(WLMContextProvider.WORKLOAD);

        // Thread context providers for the supplied class loader
        for (ThreadContextProvider provider : ServiceLoader.load(ThreadContextProvider.class, classloader)) {
            String type = provider.getThreadContextType();

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "context type " + type + " provided by " + provider);

            if (available.add(type))
                contextProviders.add(provider);
            else
                // TODO message: "Duplicate type of thread context, " + type + ", is provided by " + provider + " and " + getProvider(type));
                throw new IllegalStateException();
        }
    }

    @Override
    public ManagedExecutor.Builder newManagedExecutorBuilder() {
        return new ManagedExecutorBuilderImpl(concurrencyProvider, contextProviders);
    }

    @Override
    public ThreadContext.Builder newThreadContextBuilder() {
        return new ThreadContextBuilderImpl(concurrencyProvider, contextProviders);
    }
}