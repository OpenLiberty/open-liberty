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
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.threading.PolicyExecutor;

/**
 * Builder that programmatically configures and creates ManagedExecutor instances.
 */
class ManagedExecutorBuilderImpl implements ManagedExecutor.Builder {
    private static final TraceComponent tc = Tr.register(ManagedExecutorBuilderImpl.class);

    static final AtomicLong instanceCount = new AtomicLong();

    private final ConcurrencyProviderImpl concurrencyProvider;
    private final ArrayList<ThreadContextProvider> contextProviders;

    private final HashSet<String> cleared = new HashSet<String>();
    private int maxAsync = -1; // unlimited
    private int maxQueued = -1; // unlimited
    private final HashSet<String> propagated = new HashSet<String>();

    ManagedExecutorBuilderImpl(ConcurrencyProviderImpl concurrencyProvider, ArrayList<ThreadContextProvider> contextProviders) {
        this.concurrencyProvider = concurrencyProvider;
        this.contextProviders = contextProviders;

        // built-in defaults from spec:
        cleared.add(ThreadContext.TRANSACTION);
        propagated.add(ThreadContext.ALL_REMAINING);
    }

    @Override
    public ManagedExecutor build() {
        // For detection of unknown and overlapping types,
        HashSet<String> unknown = new HashSet<String>(cleared);
        unknown.addAll(propagated);

        if (unknown.size() < cleared.size() + propagated.size())
            failOnOverlapOfClearedPropagated();

        // Determine what to with remaining context types that are not explicitly configured
        ContextOp remaining;
        if (unknown.remove(ThreadContext.ALL_REMAINING)) {
            remaining = propagated.contains(ThreadContext.ALL_REMAINING) ? ContextOp.PROPAGATED //
                            : cleared.contains(ThreadContext.ALL_REMAINING) ? ContextOp.CLEARED //
                                            : null;
            if (remaining == null) // only possible if builder is concurrently modified during build
                throw new ConcurrentModificationException();
        } else
            remaining = ContextOp.CLEARED;

        LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider = new LinkedHashMap<ThreadContextProvider, ContextOp>();

        for (ThreadContextProvider provider : contextProviders) {
            String contextType = provider.getThreadContextType();
            unknown.remove(contextType);

            ContextOp op = propagated.contains(contextType) ? ContextOp.PROPAGATED //
                            : cleared.contains(contextType) ? ContextOp.CLEARED //
                                            : remaining;
            configPerProvider.put(provider, op);
        }

        // unknown thread context types
        if (unknown.size() > 0)
            ThreadContextBuilderImpl.failOnUnknownContextTypes(unknown, contextProviders);

        StringBuilder nameBuilder = new StringBuilder("ManagedExecutor_") //
                        .append(maxAsync)
                        .append('_') //
                        .append(maxQueued)
                        .append('_');

        for (String propagatedType : propagated)
            if (propagatedType.matches("\\w*")) // one or more of a-z, A-Z, _, 0-9
                nameBuilder.append(propagatedType).append('_');

        nameBuilder.append(instanceCount.incrementAndGet());

        String name = nameBuilder.toString();

        PolicyExecutor policyExecutor = concurrencyProvider.policyExecutorProvider.create(name) //
                        .maxConcurrency(maxAsync) //
                        .maxQueueSize(maxQueued);

        ThreadContextImpl mpThreadContext = new ThreadContextImpl(concurrencyProvider, configPerProvider);

        return new ManagedExecutorImpl(name, policyExecutor, mpThreadContext, concurrencyProvider.transactionContextProvider.transactionContextProviderRef);
    }

    @Override
    public ManagedExecutor.Builder cleared(String... types) {
        cleared.clear();
        Collections.addAll(cleared, types);
        return this;
    }

    /**
     * Fail with error indentifying the overlap(s) in context types between:
     * cleared, propagated.
     *
     * @throws IllegalStateException identifying the overlap.
     */
    private void failOnOverlapOfClearedPropagated() {
        HashSet<String> overlap = new HashSet<String>(cleared);
        overlap.retainAll(propagated);
        if (overlap.isEmpty()) // only possible if builder is concurrently modified during build
            throw new ConcurrentModificationException();
        throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1151.context.lists.overlap", overlap));
    }

    @Override
    public ManagedExecutor.Builder maxAsync(int max) {
        if (max == 0 || max < -1)
            throw new IllegalArgumentException(Integer.toString(max));
        maxAsync = max;
        return this;
    }

    @Override
    public ManagedExecutor.Builder maxQueued(int max) {
        if (max == 0 || max < -1)
            throw new IllegalArgumentException(Integer.toString(max));
        maxQueued = max;
        return this;
    }

    @Override
    public ManagedExecutor.Builder propagated(String... types) {
        propagated.clear();
        Collections.addAll(propagated, types);
        return this;
    }
}