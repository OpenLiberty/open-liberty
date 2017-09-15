/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.bundle.threading;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactoryImpl implements ThreadFactory {
    private final AtomicInteger createdThreadCount = new AtomicInteger();
    private final ThreadGroup threadGroup;
    private final ClassLoader contextClassLoader;

    public ThreadFactoryImpl() {
        this.threadGroup = AccessController.doPrivileged(new PrivilegedAction<ThreadGroup>() {
            @Override
            public ThreadGroup run() {
                return new ThreadGroup("testThreadGroup");
            }
        });
        this.contextClassLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        int threadId = createdThreadCount.incrementAndGet();
        final String name = "com.ibm.ws.threading_fat_ThreadFactoryImpl-thread-" + threadId;
        // The AccessControlContext is implicitly copied from the creating
        // thread, so use doPrivileged to prevent that.
        return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
            @Override
            public Thread run() {
                Thread thread = new Thread(threadGroup, runnable, name);
                // The daemon, priority, and context class loader are implicitly
                // copied from the creating thread, so reset them all.
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setContextClassLoader(contextClassLoader);
                return thread;
            }
        });
    }
}
