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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ServiceLoader;
import java.util.Set;

import org.eclipse.microprofile.concurrent.ManagedExecutorBuilder;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextBuilder;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.concurrent.mp.context.WLMContextProvider;

/**
 * Concurrency manager, which includes the collection of ThreadContextProviders
 * for a particular class loader, ordered by their prerequisites.
 */
public class ConcurrencyManagerImpl implements ConcurrencyManager {
    private static final TraceComponent tc = Tr.register(ConcurrencyManagerImpl.class);

    /**
     * List of available thread context providers, ordered according to their prerequisites.
     * This is the order in which thread context should be captured and applied to threads.
     * It is the reverse of the order in which thread context is restored on threads.
     */
    private final ArrayList<ThreadContextProvider> contextProviders = new ArrayList<ThreadContextProvider>();

    ConcurrencyManagerImpl(ConcurrencyProviderImpl concurrencyProvider, ClassLoader classloader) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // Thread context types for which providers with satisfied prerequisites are found.
        HashSet<String> available = new HashSet<String>();

        // Thread context providers whose prerequisites are unmet
        LinkedList<ThreadContextProvider> unsatisfied = new LinkedList<ThreadContextProvider>();

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
            Set<String> prereqs = provider.getPrerequisites();

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "context type " + type + " with prereqs " + prereqs + " provided by " + provider);

            if (available.containsAll(prereqs)) {
                if (available.add(type))
                    contextProviders.add(provider);
                else
                    // TODO message: "Duplicate type of thread context, " + type + ", is provided by " + provider + " and " + getProvider(type));
                    throw new IllegalStateException();
            } else {
                unsatisfied.add(provider);
            }
        }

        // Additional passes through the unsatisfied providers list until all are satisfied or no changes are made

        for (boolean changed = true; changed && !unsatisfied.isEmpty();) {
            changed = false;
            for (Iterator<ThreadContextProvider> providers = unsatisfied.iterator(); providers.hasNext();) {
                ThreadContextProvider provider = providers.next();

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "previously unsatisfied provider " + provider);

                if (available.containsAll(provider.getPrerequisites())) {
                    providers.remove();
                    if (available.add(provider.getThreadContextType()))
                        contextProviders.add(provider);
                    else
                        throw new IllegalStateException(); // TODO same message from earlier
                    changed = true;
                }
            }
        }

        // TODO should unsatisfied providers be an error, or defer to later & only if the builder wants to enable them?
        if (!unsatisfied.isEmpty())
            throw new IllegalStateException(unsatisfied.toString()); // TODO message with better detail
    }

    @Override
    public ManagedExecutorBuilder newManagedExecutorBuilder() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ThreadContextBuilder newThreadContextBuilder() {
        return new ThreadContextBuilderImpl(contextProviders);
    }
}