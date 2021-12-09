/*******************************************************************************
 * Copyright (c) 2018,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.contextpropagation;

import static com.ibm.ws.microprofile.contextpropagation.ThreadContextBuilderImpl.DEFAULT_CLEARED;
import static com.ibm.ws.microprofile.contextpropagation.ThreadContextBuilderImpl.DEFAULT_PROPAGATED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.concurrent.mp.spi.ManagedExecutorFactory;
import com.ibm.ws.microprofile.context.EmptyHandleListContextProvider;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threading.PolicyExecutor;

/**
 * Builder that programmatically configures and creates ManagedExecutor instances.
 */
public class ManagedExecutorBuilderImpl implements ManagedExecutor.Builder {
    private static final TraceComponent tc = Tr.register(ManagedExecutorBuilderImpl.class);

    // Represents that no value is defined for an int config attribute (maxAsync or maxQueued)
    private static final int UNDEFINED = -2;

    private final ContextManagerImpl contextManager;
    private final ArrayList<ThreadContextProvider> contextProviders;

    private Set<String> cleared;
    private int maxAsync = UNDEFINED;
    private int maxQueued = UNDEFINED;
    private String name;
    private Set<String> propagated;

    ManagedExecutorBuilderImpl(ContextManagerImpl contextManager, ArrayList<ThreadContextProvider> contextProviders) {
        this.contextManager = contextManager;
        this.contextProviders = contextProviders;
    }

    @Override
    public ManagedExecutor build() {
        Set<String> cleared = this.cleared == null ? contextManager.getDefault("mp.context.ManagedExecutor.cleared", DEFAULT_CLEARED) : this.cleared;
        int maxAsync = this.maxAsync == UNDEFINED ? contextManager.getDefault("mp.context.ManagedExecutor.maxAsync", -1) : this.maxAsync;
        int maxQueued = this.maxQueued == UNDEFINED ? contextManager.getDefault("mp.context.ManagedExecutor.maxQueued", -1) : this.maxQueued;
        Set<String> propagated = this.propagated == null ? contextManager.getDefault("mp.context.ManagedExecutor.propagated", DEFAULT_PROPAGATED) : this.propagated;

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

        ContextManagerProviderImpl cmProvider = contextManager.cmProvider;
        ThreadContextConfigImpl configPerProvider = new ThreadContextConfigImpl(cmProvider);

        for (ThreadContextProvider provider : contextProviders) {
            String contextType = provider.getThreadContextType();
            unknown.remove(contextType);

            ContextOp op = EmptyHandleListContextProvider.EMPTY_HANDLE_LIST.contentEquals(contextType) ? ContextOp.CLEARED //
                            : propagated.contains(contextType) ? ContextOp.PROPAGATED //
                                            : cleared.contains(contextType) ? ContextOp.CLEARED //
                                                            : remaining;
            configPerProvider.put(provider, op);
        }

        // unknown thread context types
        if (unknown.size() > 0)
            ThreadContextBuilderImpl.failOnUnknownContextTypes(unknown, contextProviders);

        // Generate name for CDI injected instance,
        //  ManagedExecutor@INSTANCEID_AppName_org.test.MyBean.execA(maxAsync=5,maxQueued=max,propagated=[CDI,Security])
        // or for instance created via the builder,
        //  ManagedExecutor@INSTANCEID_AppName(maxAsync=5,maxQueued=max,propagated=[CDI,Security])

        int hash = ContextManagerImpl.instanceCount.incrementAndGet();
        StringBuilder nameBuilder = new StringBuilder("ManagedExecutor@").append(Integer.toHexString(hash));
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        String appName = cData == null ? null : cData.getJ2EEName().getApplication();
        if (appName != null)
            nameBuilder.append('_').append(appName);
        if (name != null)
            nameBuilder.append('_').append(name);

        nameBuilder.append("(maxAsync=")
                        .append(maxAsync == -1 ? "max" : maxAsync)
                        .append(",maxQueued=")
                        .append(maxQueued == -1 ? "max" : maxQueued);

        if (propagated.contains(ThreadContext.ALL_REMAINING))
            nameBuilder.append(",cleared=").append(cleared);
        else
            nameBuilder.append(",propagated=").append(propagated);

        nameBuilder.append(')');

        String executorName = nameBuilder.toString();
        String threadContextName = nameBuilder.replace(2, 15, "ThreadContext").substring(2);

        PolicyExecutor policyExecutor = cmProvider.policyExecutorProvider.create(executorName, appName) //
                        .maxConcurrency(maxAsync) //
                        .maxQueueSize(maxQueued);

        return ManagedExecutorFactory.createManagedExecutor(executorName, threadContextName, hash, cmProvider.eeVersion,
                                                            policyExecutor, configPerProvider,
                                                            cmProvider.transactionContextProvider.transactionContextProviderRef);
    }

    @Override
    public ManagedExecutor.Builder cleared(String... types) {
        if (cleared == null)
            cleared = new HashSet<String>();
        else
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

    /**
     * Sets the name of the CDI injection point for which the container is producing an instance.
     *
     * @param name fully qualified injection point name.
     * @return the same builder instance.
     */
    public ManagedExecutor.Builder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public ManagedExecutor.Builder propagated(String... types) {
        if (propagated == null)
            propagated = new HashSet<String>();
        else
            propagated.clear();
        Collections.addAll(propagated, types);
        return this;
    }
}