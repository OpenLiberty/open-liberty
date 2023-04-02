/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
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
package com.ibm.ws.concurrent.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import javax.enterprise.concurrent.ManageableThread;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.internal.ManagedThreadFactoryService.ManagedThreadFactoryImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * ForkJoinWorkerThread created by a managed thread factory.
 */
class ManagedForkJoinWorkerThread extends ForkJoinWorkerThread implements ManageableThread {
    private static final TraceComponent tc = Tr.register(ManagedForkJoinWorkerThread.class);

    /**
     * Associates ForkJoinWorkerThreads with a ThreadGroup.
     * Although not actually in the thread group, this allows us to interrupt these
     * threads upon application stop and/or removal of the configured managedThreadFactory.
     */
    static final ConcurrentHashMap<ManagedForkJoinWorkerThread, ThreadGroup> ACTIVE_THREADS = new ConcurrentHashMap<>();

    /**
     * Managed thread factory instance that created this thread.
     */
    private final ManagedThreadFactoryImpl threadFactory;

    /**
     * Construct a new managed thread.
     *
     * @param threadFactory managed thread factory
     * @param pool          ForkJoinPool in which the thread runs
     */
    @Trivial
    ManagedForkJoinWorkerThread(ManagedThreadFactoryImpl threadFactory, ForkJoinPool pool) {
        super(pool);
        this.threadFactory = threadFactory;

        if (threadFactory.service.createDaemonThreads != isDaemon()
            || threadFactory.service.defaultPriority != null && threadFactory.service.defaultPriority != getPriority())
            AccessController.doPrivileged(new InitAction(), threadFactory.serverAccessControlContext);
    }

    /**
     * @see java.lang.Thread#interrupt()
     */
    @Override
    public void interrupt() {
        // EE Concurrency 3.4.1: The application component thread has permission to interrupt the thread. All other modifications to the
        // thread are subject to the security manager, if present.
        if (threadFactory.sameMetaDataIdentity())
            AccessController.doPrivileged(new InterruptAction(), threadFactory.serverAccessControlContext);
        else
            super.interrupt();
    }

    /**
     * This method exists so that the inner class can invoke an operation on the superclass. Is there a better way?
     */
    @Trivial
    private final void interruptSuper() {
        super.interrupt();
    }

    /**
     * @see javax.enterprise.concurrent.ManageableThread#isShutdown()
     */
    @Override
    public final boolean isShutdown() {
        return threadFactory.service.isShutdown.get() || getState() == Thread.State.TERMINATED;
    }

    @Override
    protected void onStart() {
        // EE Concurrency 3.4.4: Threads that are created by a ManagedThreadFactory instance but are started after the
        // ManagedThreadFactory has shut down [are] required to start with an interrupted status.
        if (threadFactory.service.isShutdown.get())
            interrupt();
        else
            ACTIVE_THREADS.put(this, threadFactory.threadGroup);

        super.onStart();
    }

    /**
     * @see java.lang.Thread#run()
     */
    @Override
    @Trivial
    public void run() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "run");

        try {
            ArrayList<ThreadContext> contextAppliedToThread = threadFactory.threadContextDescriptor.taskStarting();
            try {
                super.run();
            } finally {
                threadFactory.threadContextDescriptor.taskStopping(contextAppliedToThread);
            }
        } catch (Error x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run", x);
            throw x;
        } catch (RuntimeException x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run", x);
            throw x;
        } finally {
            ACTIVE_THREADS.remove(this);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "run");
    }

    // Do not override start() - it is not invoked in Java 19 and above

    /**
     * Include the instance information in trace:
     * ManagedForkJoinWorkerThread@07718601:Thread[ForkJoinPool-1-worker-3,3,Default Executor Thread Group]
     */
    @Override
    @Trivial
    public String toString() {
        return getClass().getSimpleName() + '@' + Integer.toHexString(hashCode()) + ':' + super.toString();
    }

    /**
     * Privileged action that initializes a thread.
     */
    @Trivial
    private class InitAction implements PrivilegedAction<Void> {
        /**
         * Initialize a thread's priority and isDaemon.
         */
        @Override
        public Void run() {
            if (threadFactory.service.createDaemonThreads != isDaemon())
                setDaemon(threadFactory.service.createDaemonThreads);

            if (threadFactory.service.defaultPriority != null && threadFactory.service.defaultPriority != getPriority())
                setPriority(threadFactory.service.defaultPriority);

            return null;
        }
    }

    /**
     * Privileged action to interrupt a managed thread.
     */
    @Trivial
    private class InterruptAction implements PrivilegedAction<Void> {
        /**
         * Interrupt a managed thread.
         */
        @Override
        public Void run() {
            interruptSuper();
            return null;
        }
    }
}