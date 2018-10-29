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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.context.ContainerContextProvider;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Programmatically built ThreadContext instance - either via ThreadContextBuilder
 * or injected by CDI and possibly annotated by <code>@ThreadContextConfig</code>
 */
class ThreadContextDescriptorImpl implements ThreadContextDescriptor {
    private static final TraceComponent tc = Tr.register(ThreadContextDescriptorImpl.class);

    private final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    /**
     * List of thread context snapshots (either captured from the requesting thread or cleared/empty)
     * which is ordered based on thread context provider prerequisites.
     */
    private ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots = new ArrayList<com.ibm.wsspi.threadcontext.ThreadContext>();

    ThreadContextDescriptorImpl(LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider) {
        // create snapshots of captured or cleared context here, per the configured instructions for each type
        for (Map.Entry<ThreadContextProvider, ContextOp> entry : configPerProvider.entrySet()) {
            ThreadContextProvider provider = entry.getKey();
            if (provider instanceof ContainerContextProvider) {
                for (com.ibm.wsspi.threadcontext.ThreadContextProvider cp : ((ContainerContextProvider) provider).toContainerProviders()) {
                    com.ibm.wsspi.threadcontext.ThreadContext tc = entry.getValue() == ContextOp.CLEARED //
                                    ? cp.createDefaultThreadContext(EMPTY_MAP) //
                                    : cp.captureThreadContext(EMPTY_MAP, EMPTY_MAP); // PROPAGATED
                    contextSnapshots.add(tc);
                }
            } else {
                // TODO support MP context types
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
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
        return Collections.emptyMap();
    }

    @Override
    @Trivial
    public byte[] serialize() throws IOException {
        throw new NotSerializableException();
    }

    @Override
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
    public ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> taskStarting() throws RejectedExecutionException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // TODO enforce application availability

        ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextAppliedToThread = new ArrayList<com.ibm.wsspi.threadcontext.ThreadContext>(contextSnapshots.size());
        try {
            for (com.ibm.wsspi.threadcontext.ThreadContext contextSnapshot : contextSnapshots) {
                contextSnapshot = contextSnapshot.clone();

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "begin context " + toString(contextSnapshot));
                contextSnapshot.taskStarting();

                contextAppliedToThread.add(contextSnapshot);
            }
        } catch (RuntimeException | Error x) {
            // In the event of failure, undo all context propagation up to this point.

            for (int c = contextAppliedToThread.size() - 1; c >= 0; c--)
                try {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "end context " + toString(contextAppliedToThread.get(c)));
                    contextAppliedToThread.get(c).taskStopping();
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
    public void taskStopping(ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextAppliedToThread) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Throwable failure = null;
        for (int c = contextAppliedToThread.size() - 1; c >= 0; c--)
            try {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "end context " + toString(contextAppliedToThread.get(c)));
                contextAppliedToThread.get(c).taskStopping();
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