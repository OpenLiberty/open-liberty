/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.internal.ManagedThreadFactoryService.ManagedThreadFactoryImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * Thread created by a managed thread factory.
 */
class ManagedThreadImpl extends Thread implements jakarta.enterprise.concurrent.ManageableThread, javax.enterprise.concurrent.ManageableThread {
    private static final TraceComponent tc = Tr.register(ManagedThreadImpl.class);

    /**
     * Indicates if the managed thread factory has been marked for shutdown.
     */
    private final ManagedThreadFactoryImpl threadFactory;

    /**
     * Construct a new managed thread.
     *
     * @param threadFactory managed thread factory
     * @param runnable task to run on the thread
     * @param name name for the thread
     */
    ManagedThreadImpl(ManagedThreadFactoryImpl threadFactory, Runnable runnable, String name) {
        super(threadFactory.threadGroup, runnable, name);
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
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "run");
    }

    /**
     * @see java.lang.Thread#start()
     */
    @Override
    public void start() {
        // EE Concurrency 3.4.4: Threads that are created by a ManagedThreadFactory instance but are started after the
        // ManagedThreadFactory has shut down [are] required to start with an interrupted status.
        if (threadFactory.service.isShutdown.get())
            interrupt();
        super.start();
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