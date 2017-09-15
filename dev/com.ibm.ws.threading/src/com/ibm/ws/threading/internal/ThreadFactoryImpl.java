/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * The default thread factory to use when one is not specified.
 */
final class ThreadFactoryImpl implements ThreadFactory {

    private final static TraceComponent tc = Tr.register(ThreadFactoryImpl.class);
    private final static boolean TRACE_ENABLED_AT_STARTUP = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
    private final AtomicInteger createdThreadCount = new AtomicInteger();

    /**
     * The name of the executor associated with this factory.
     */
    private final String executorName;

    /**
     * The thread group to associate with newly created threads.
     */
    private final ThreadGroup threadGroup;

    /**
     * The context class loader to associate with newly created threads.
     */
    private final ClassLoader contextClassLoader;

    /**
     * Create a thread factory that creates threads associated with a
     * named thread group.
     * 
     * @param executorName the name of the owning executor that serves as
     * @param threadGroupName the name of the thread group to create
     */
    ThreadFactoryImpl(final String executorName, final String threadGroupName) {
        this.executorName = executorName;
        this.threadGroup = AccessController.doPrivileged(new PrivilegedAction<ThreadGroup>() {
            @Override
            public ThreadGroup run() {
                return new ThreadGroup(threadGroupName);
            }
        });
        this.contextClassLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    /**
     * Create a new thread.
     * 
     * @param runnable the task to run
     */
    @Override
    public Thread newThread(final Runnable runnable) {
        int threadId = createdThreadCount.incrementAndGet();
        final String name = executorName + "-thread-" + threadId;
        // The AccessControlContext is implicitly copied from the creating
        // thread, so use doPrivileged to prevent that.
        return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
            @Override
            public Thread run() {
                final Thread thread;
                if (TRACE_ENABLED_AT_STARTUP) {
                    // We don't want a mixed pool of LibertyThreads and regular Threads, which
                    // could occur if a user were to change the trace settings after startup.
                    // Also, using the LibertyThread will have a (very) slight impact on 
                    // performance as it will require a check to the trace component and an
                    // additional method call.  As such, we will only use LibertyThreads if
                    // the trace was enabled at server startup - the user can subsequently
                    // disable the trace while the server is still up, which will stop the
                    // LibertyThreads from printing stack traces, but may still be slightly
                    // slower than using normal Threads.
                    thread = new LibertyThread(threadGroup, runnable, name);
                } else {
                    thread = new Thread(threadGroup, runnable, name);
                }
                // The daemon, priority, and context class loader are implicitly
                // copied from the creating thread, so reset them all.
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setContextClassLoader(contextClassLoader);
                return thread;
            }
        });
    }

    /**
     * This thread subclass is used for diagnostics. When the user specifies debug
     * level for this trace component (com.ibm.ws.threading.internal.ThreadFactoryImpl),
     * at startup, then the ThreadFactoryImpl will return instances of LibertyThread
     * rather than Thread. LibertyThread will continue to check the trace settings
     * (for performance), and if still enabled, will print a stack trace and additional
     * diagnostics when key methods, like interrupt() or setContextClassLoader(...) are
     * called on it.
     */
    static class LibertyThread extends Thread {

        LibertyThread(ThreadGroup tg, Runnable r, String name) {
            super(tg, r, name);
        }

        @Override
        @Trivial
        // marked trivial since we only want the stack trace, not entry/exit
        public void interrupt() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Throwable t = new Throwable("stack trace");
                Tr.debug(tc, "Thread.interrupt() called on " + this, t);
            }
            super.interrupt();
        }

        @Override
        @Trivial
        // marked trivial since we only want the stack trace, not entry/exit
        public void setContextClassLoader(ClassLoader cl) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Throwable t = new Throwable("stack trace");
                Tr.debug(tc, "Thread.setContextClassLoader( " + cl + " ) called on " + this, t);
            }
            super.setContextClassLoader(cl);
        }
    }
}