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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.concurrent.mp.spi.ThreadContextFactory;
import com.ibm.ws.microprofile.context.EmptyHandleListContextProvider;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Builder that programmatically configures and creates ThreadContext instances.
 */
public class ThreadContextBuilderImpl implements ThreadContext.Builder {
    private static final TraceComponent tc = Tr.register(ThreadContextBuilderImpl.class);

    // TODO decide whether or not to have vendor-specific defaults
    static final Set<String> DEFAULT_CLEARED = Collections.singleton(ThreadContext.TRANSACTION);
    static final Set<String> DEFAULT_PROPAGATED = Collections.singleton(ThreadContext.ALL_REMAINING);
    static final Set<String> DEFAULT_UNCHANGED = Collections.emptySet();

    private final ContextManagerImpl contextManager;
    private final ArrayList<ThreadContextProvider> contextProviders;

    private Set<String> cleared;
    private String name;
    private Set<String> propagated;
    private Set<String> unchanged;

    ThreadContextBuilderImpl(ContextManagerImpl contextManager, ArrayList<ThreadContextProvider> contextProviders) {
        this.contextManager = contextManager;
        this.contextProviders = contextProviders;
    }

    @Override
    public ThreadContext build() {
        Set<String> cleared = this.cleared == null ? contextManager.getDefault("mp.context.ThreadContext.cleared", DEFAULT_CLEARED) : this.cleared;
        Set<String> propagated = this.propagated == null ? contextManager.getDefault("mp.context.ThreadContext.propagated", DEFAULT_PROPAGATED) : this.propagated;
        Set<String> unchanged = this.unchanged == null ? contextManager.getDefault("mp.context.ThreadContext.unchanged", DEFAULT_UNCHANGED) : this.unchanged;

        // For detection of unknown and overlapping types,
        HashSet<String> unknown = new HashSet<String>(cleared);
        unknown.addAll(propagated);

        if (unknown.size() < cleared.size() + propagated.size() || !Collections.disjoint(unknown, unchanged))
            failOnOverlapOfClearedPropagatedUnchanged(cleared, propagated, unchanged);

        // Determine what to with remaining context types that are not explicitly configured
        ContextOp remaining;
        if (unknown.remove(ThreadContext.ALL_REMAINING)) {
            remaining = propagated.contains(ThreadContext.ALL_REMAINING) ? ContextOp.PROPAGATED //
                            : cleared.contains(ThreadContext.ALL_REMAINING) ? ContextOp.CLEARED //
                                            : null;
            if (remaining == null) // only possible if builder is concurrently modified during build
                throw new ConcurrentModificationException();
        } else
            remaining = unchanged.contains(ThreadContext.ALL_REMAINING) ? ContextOp.UNCHANGED : ContextOp.CLEARED;

        ThreadContextConfigImpl configPerProvider = new ThreadContextConfigImpl(contextManager.cmProvider);

        for (ThreadContextProvider provider : contextProviders) {
            String contextType = provider.getThreadContextType();
            unknown.remove(contextType);

            ContextOp op = EmptyHandleListContextProvider.EMPTY_HANDLE_LIST.contentEquals(contextType) ? ContextOp.CLEARED //
                            : propagated.contains(contextType) ? ContextOp.PROPAGATED //
                                            : cleared.contains(contextType) ? ContextOp.CLEARED //
                                                            : unchanged.contains(contextType) ? ContextOp.UNCHANGED //
                                                                            : remaining;
            if (op != ContextOp.UNCHANGED)
                configPerProvider.put(provider, op);
        }

        // unknown thread context types
        if (unknown.size() > 0)
            failOnUnknownContextTypes(unknown, contextProviders);

        // Generate name for CDI injected instance,
        //  ThreadContext@INSTANCEID_AppName_org.test.MyBean.threadContextA(propagated=[CDI, Security],cleared=[Remaining],unchanged=[Transaction])
        // or for instance created via the builder,
        //  ThreadContext@INSTANCEID_AppName(propagated=[Security],cleared=[Application, CDI, Transaction],unchanged=[Remaining])

        int hash = ContextManagerImpl.instanceCount.incrementAndGet();
        StringBuilder nameBuilder = new StringBuilder("ThreadContext@").append(Integer.toHexString(hash));
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        String appName = cData == null ? null : cData.getJ2EEName().getApplication();
        if (appName != null)
            nameBuilder.append('_').append(appName);
        if (name != null)
            nameBuilder.append('_').append(name);

        nameBuilder.append("(propagated=").append(propagated);
        nameBuilder.append(",cleared=").append(cleared);
        nameBuilder.append(",unchanged=").append(unchanged);
        nameBuilder.append(')');

        String threadContextName = nameBuilder.toString();

        return ThreadContextFactory.createThreadContext(threadContextName, hash, contextManager.cmProvider.eeVersion, configPerProvider);
    }

    @Override
    public ThreadContext.Builder cleared(String... types) {
        if (cleared == null)
            cleared = new HashSet<String>();
        else
            cleared.clear();
        Collections.addAll(cleared, types);
        return this;
    }

    /**
     * Fail with error identifying the overlap(s) in context types between any two of:
     * cleared, propagated, unchanged.
     *
     * @throws IllegalStateException identifying the overlap.
     */
    private void failOnOverlapOfClearedPropagatedUnchanged(Set<String> cleared, Set<String> propagated, Set<String> unchanged) {
        HashSet<String> overlap = new HashSet<String>(cleared);
        overlap.retainAll(propagated);
        HashSet<String> s = new HashSet<String>(cleared);
        s.retainAll(unchanged);
        overlap.addAll(s);
        s = new HashSet<String>(propagated);
        s.retainAll(unchanged);
        overlap.addAll(s);
        if (overlap.isEmpty()) // only possible if builder is concurrently modified during build
            throw new ConcurrentModificationException();
        throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1152.context.lists.overlap", overlap));
    }

    /**
     * Fail with error identifying unknown context type(s) that were specified.
     *
     * @param unknown          set of unknown context types(s) that were specified.
     * @param contextProviders
     */
    static void failOnUnknownContextTypes(HashSet<String> unknown, ArrayList<ThreadContextProvider> contextProviders) {
        Set<String> known = new TreeSet<>(); // alphabetize for readability of message
        known.addAll(Arrays.asList(ThreadContext.ALL_REMAINING, ThreadContext.APPLICATION, ThreadContext.CDI, ThreadContext.SECURITY, ThreadContext.TRANSACTION));
        for (ThreadContextProvider provider : contextProviders) {
            String contextType = provider.getThreadContextType();
            known.add(contextType);
        }

        throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1155.unknown.context", new TreeSet<String>(unknown), known));
    }

    /**
     * Sets the name of the CDI injection point for which the container is producing an instance.
     *
     * @param name fully qualified injection point name.
     * @return the same builder instance.
     */
    public ThreadContext.Builder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public ThreadContext.Builder propagated(String... types) {
        if (propagated == null)
            propagated = new HashSet<String>();
        else
            propagated.clear();
        Collections.addAll(propagated, types);
        return this;
    }

    @Override
    public ThreadContext.Builder unchanged(String... types) {
        if (unchanged == null)
            unchanged = new HashSet<String>();
        else
            unchanged.clear();
        Collections.addAll(unchanged, types);
        return this;
    }
}