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

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextBuilder;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

/**
 * Builder that programmatically configures and creates ThreadContext instances.
 */
class ThreadContextBuilderImpl implements ThreadContextBuilder {
    private final ConcurrencyProviderImpl concurrencyProvider;

    private final HashSet<String> cleared = new HashSet<String>();
    private final HashSet<String> propagated = new HashSet<String>();
    private final HashSet<String> unchanged = new HashSet<String>();

    ThreadContextBuilderImpl(ConcurrencyProviderImpl concurrencyProvider) {
        this.concurrencyProvider = concurrencyProvider;

        // built-in defaults from spec:
        // cleared.add(ThreadContext.TRANSACTION); // TODO add this once we pull in newer spec jar
        propagated.add(ThreadContext.ALL); // TODO ALL is renamed to ALL_REMAINING once we update spec binaries
    }

    @Override
    public ThreadContext build() {
        // For detection of unknown and overlapping types,
        HashSet<String> unknown = new HashSet<String>(cleared);
        unknown.addAll(propagated);
        unknown.addAll(unchanged);

        if (unknown.size() < cleared.size() + propagated.size() + unchanged.size())
            throw new IllegalArgumentException(/* TODO findOverlapping(configured) */);

        // Determine what to with remaining context types that are not explicitly configured
        ContextOp remaining;
        if (unknown.remove(ThreadContext.ALL)) {
            remaining = propagated.contains(ThreadContext.ALL) ? ContextOp.PROPAGATED //
                            : cleared.contains(ThreadContext.ALL) ? ContextOp.CLEARED //
                                            : unchanged.contains(ThreadContext.ALL) ? ContextOp.UNCHANGED //
                                                            : null;
            if (remaining == null) // only possible if builder is concurrently modified during build
                throw new ConcurrentModificationException();
        } else
            remaining = ContextOp.CLEARED;

        LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider = new LinkedHashMap<ThreadContextProvider, ContextOp>();

        Consumer<ThreadContextProvider> addProvider = provider -> {
            String contextType = provider.getThreadContextType();
            unknown.remove(contextType);

            ContextOp op = propagated.contains(contextType) ? ContextOp.PROPAGATED //
                            : cleared.contains(contextType) ? ContextOp.CLEARED //
                                            : unchanged.contains(contextType) ? ContextOp.UNCHANGED //
                                                            : remaining;
            if (op != ContextOp.UNCHANGED)
                configPerProvider.put(provider, op);
        };

        // thread context providers from the container
        addProvider.accept(concurrencyProvider.applicationContextProvider);
        // TODO other container providers

        // TODO obtain providers from ServiceLoader, preferably from cache based on class loader (can also do duplicate provider check there)

        // TODO process in an order that keeps prereqs satisfied

        // unknown thread context types
        if (unknown.size() > 0)
            throw new IllegalArgumentException(unknown.toString());

        return new ThreadContextImpl(configPerProvider);
    }

    // TODO @Override
    public ThreadContextBuilder cleared(String... types) {
        cleared.clear();
        for (String type : types)
            cleared.add(type);
        return this;
    }

    @Override
    public ThreadContextBuilder propagated(String... types) {
        propagated.clear();
        for (String type : types)
            propagated.add(type);
        return this;
    }

    @Override
    public ThreadContextBuilder unchanged(String... types) {
        unchanged.clear();
        for (String type : types)
            unchanged.add(type);
        return this;
    }
}