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

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

/**
 * Builder that programmatically configures and creates ThreadContext instances.
 */
class ThreadContextBuilderImpl implements ThreadContext.Builder {
    private final ConcurrencyProviderImpl concurrencyProvider;
    private final ArrayList<ThreadContextProvider> contextProviders;

    private final HashSet<String> cleared = new HashSet<String>();
    private final HashSet<String> propagated = new HashSet<String>();
    private final HashSet<String> unchanged = new HashSet<String>();

    ThreadContextBuilderImpl(ConcurrencyProviderImpl concurrencyProvider, ArrayList<ThreadContextProvider> contextProviders) {
        this.concurrencyProvider = concurrencyProvider;
        this.contextProviders = contextProviders;

        // built-in defaults from spec:
        cleared.add(ThreadContext.TRANSACTION);
        propagated.add(ThreadContext.ALL_REMAINING);
    }

    @Override
    public ThreadContext build() {
        // For detection of unknown and overlapping types,
        HashSet<String> unknown = new HashSet<String>(cleared);
        unknown.addAll(propagated);
        unknown.addAll(unchanged); // TODO should not cause an error if an unchanged type is not known

        if (unknown.size() < cleared.size() + propagated.size() + unchanged.size())
            throw new IllegalArgumentException(/* TODO findOverlapping(configured) */);

        // Determine what to with remaining context types that are not explicitly configured
        ContextOp remaining;
        if (unknown.remove(ThreadContext.ALL_REMAINING)) {
            remaining = propagated.contains(ThreadContext.ALL_REMAINING) ? ContextOp.PROPAGATED //
                            : cleared.contains(ThreadContext.ALL_REMAINING) ? ContextOp.CLEARED //
                                            : unchanged.contains(ThreadContext.ALL_REMAINING) ? ContextOp.UNCHANGED //
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
                                            : unchanged.contains(contextType) ? ContextOp.UNCHANGED //
                                                            : remaining;
            if (op != ContextOp.UNCHANGED)
                configPerProvider.put(provider, op);
        }

        // unknown thread context types
        if (unknown.size() > 0)
            throw new IllegalArgumentException(unknown.toString());

        return new ThreadContextImpl(concurrencyProvider, configPerProvider);
    }

    @Override
    public ThreadContext.Builder cleared(String... types) {
        cleared.clear();
        Collections.addAll(cleared, types);
        return this;
    }

    @Override
    public ThreadContext.Builder propagated(String... types) {
        propagated.clear();
        Collections.addAll(propagated, types);
        return this;
    }

    @Override
    public ThreadContext.Builder unchanged(String... types) {
        unchanged.clear();
        Collections.addAll(unchanged, types);
        return this;
    }
}