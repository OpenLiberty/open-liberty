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

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.context.ContainerContextProvider;
import com.ibm.ws.concurrent.mp.context.DeferredClearedContext;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Programmatically built ThreadContext instance - either via ThreadContextBuilder
 * or injected by CDI and possibly annotated by <code>@ThreadContextConfig</code>
 */
class ThreadContextDescriptorImpl implements ThreadContextDescriptor {
    private static final TraceComponent tc = Tr.register(ThreadContextDescriptorImpl.class);

    private final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    /**
     * The concurrency provider.
     */
    private final ConcurrencyProviderImpl concurrencyProvider;

    /**
     * List of thread context snapshots (either captured from the requesting thread or cleared/empty)
     */
    private ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots = new ArrayList<com.ibm.wsspi.threadcontext.ThreadContext>();

    /**
     * Metadata identifier for the application component. Can be null if not associated with an application component.
     */
    private final String metadataIdentifier;

    ThreadContextDescriptorImpl(ConcurrencyProviderImpl concurrencyProvider, LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider) {
        this.concurrencyProvider = concurrencyProvider;

        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        metadataIdentifier = cData == null ? null : concurrencyProvider.metadataIdentifierService.getMetaDataIdentifier(cData);

        // create snapshots of captured or cleared context here, per the configured instructions for each type
        for (Map.Entry<ThreadContextProvider, ContextOp> entry : configPerProvider.entrySet()) {
            ThreadContextProvider provider = entry.getKey();
            if (provider instanceof ContainerContextProvider) {
                ((ContainerContextProvider) provider).addContextSnapshot(entry.getValue(), contextSnapshots);
            } else {
                ThreadContextSnapshot contextSnapshot = entry.getValue() == ContextOp.CLEARED //
                                ? provider.clearedContext(EMPTY_MAP) // CLEARED
                                : provider.currentContext(EMPTY_MAP); // PROPAGATED
                // Convert to the com.ibm.wsspi.threadcontext.ThreadContext type which the container understands
                contextSnapshots.add(new ContextSnapshotProxy(contextSnapshot));
            }
        }
    }

    @Override
    @Trivial
    public ThreadContextDescriptor clone() {
        try {
            ThreadContextDescriptorImpl clone = (ThreadContextDescriptorImpl) super.clone();
            clone.contextSnapshots = new ArrayList<com.ibm.wsspi.threadcontext.ThreadContext>(contextSnapshots);
            return clone;
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x); // should never occur
        }

    }

    @Override
    @Trivial
    public Map<String, String> getExecutionProperties() {
        return EMPTY_MAP;
    }

    @Override
    @Trivial
    public byte[] serialize() throws IOException {
        throw new NotSerializableException();
    }

    @Override
    @Trivial
    public void set(String providerName, com.ibm.wsspi.threadcontext.ThreadContext context) {
        throw new UnsupportedOperationException();
    }

    /**
     * Establish context on a thread before a contextual operation is started.
     *
     * @return list of thread context matching the order in which context has been applied to the thread.
     * @throws IllegalStateException if the application component is not started or deployed.
     * @throws RejectedExecutionException if context cannot be established on the thread.
     */
    @Override
    @Trivial // traced with greater granularity within method
    public ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> taskStarting() throws RejectedExecutionException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // EE Concurrency 3.3.4: All invocations to any of the proxied interface methods will fail with a
        // java.lang.IllegalStateException exception if the application component is not started or deployed.
        if (metadataIdentifier != null && concurrencyProvider.metadataIdentifierService.getMetaData(metadataIdentifier) == null)
            com.ibm.ws.context.service.serializable.ThreadContextDescriptorImpl.notAvailable(metadataIdentifier, "");

        ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextAppliedToThread = new ArrayList<com.ibm.wsspi.threadcontext.ThreadContext>(contextSnapshots.size());
        try {
            for (com.ibm.wsspi.threadcontext.ThreadContext contextSnapshot : contextSnapshots) {
                contextSnapshot = contextSnapshot.clone();

                contextSnapshot.taskStarting();

                contextAppliedToThread.add(contextSnapshot);

                if (trace && tc.isDebugEnabled()) {
                    if (contextSnapshot instanceof DeferredClearedContext) {
                        Object clearedContextController = ((DeferredClearedContext) contextSnapshot).clearedContextController;
                        if (clearedContextController != null)
                            Tr.debug(this, tc, "cleared context " + clearedContextController);
                    } else {
                        Tr.debug(this, tc, "applied context " + toString(contextSnapshot));
                    }
                }
            }
        } catch (RuntimeException | Error x) {
            // In the event of failure, undo all context propagation up to this point.
            for (int c = contextAppliedToThread.size() - 1; c >= 0; c--)
                try {
                    com.ibm.wsspi.threadcontext.ThreadContext appliedContext = contextAppliedToThread.get(c);
                    if (trace && tc.isDebugEnabled()) {
                        if (appliedContext instanceof DeferredClearedContext) {
                            Object clearedContextController = ((DeferredClearedContext) appliedContext).clearedContextController;
                            if (clearedContextController != null)
                                Tr.debug(this, tc, "restore context " + clearedContextController);
                        } else {
                            Tr.debug(this, tc, "restore context " + toString(appliedContext));
                        }
                    }
                    appliedContext.taskStopping();
                } catch (Throwable stopX) {
                }

            throw x;
        }

        return contextAppliedToThread;
    }

    /**
     * Remove context from the thread (in reverse of the order in which is was applied) after a contextual operation completes.
     *
     * @param threadContext list of context previously applied to thread, ordered according to the order in which it was applied to the thread.
     */
    @Override
    @Trivial // traced with greater granularity within method
    public void taskStopping(ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextAppliedToThread) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Throwable failure = null;
        for (int c = contextAppliedToThread.size() - 1; c >= 0; c--)
            try {
                com.ibm.wsspi.threadcontext.ThreadContext appliedContext = contextAppliedToThread.get(c);
                if (trace && tc.isDebugEnabled()) {
                    if (appliedContext instanceof DeferredClearedContext) {
                        Object clearedContextController = ((DeferredClearedContext) appliedContext).clearedContextController;
                        if (clearedContextController != null)
                            Tr.debug(this, tc, "restore context " + clearedContextController);
                    } else {
                        Tr.debug(this, tc, "restore context " + toString(appliedContext));
                    }
                }
                appliedContext.taskStopping();
            } catch (Throwable x) {
                if (failure == null)
                    failure = x;
            }

        if (failure != null)
            if (failure instanceof RuntimeException)
                throw (RuntimeException) failure;
            else if (failure instanceof Error)
                throw (Error) failure;
            else
                throw new RuntimeException(failure); // should never happen
    }

    /**
     * Returns text that uniquely identifies a context instance. This is only used for tracing.
     *
     * @param c a context
     * @return text formatted to include the class name and hashcode.
     */
    @Trivial
    private static String toString(com.ibm.wsspi.threadcontext.ThreadContext c) {
        if (c == null)
            return null;
        String s = c.toString();
        if (s.indexOf('@') < 0)
            s = c.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(c)) + ':' + s;
        return s;
    }
}