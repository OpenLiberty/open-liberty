/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package io.openliberty.threading.virtual;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * An interface for virtual thread-related operations that require Java 21.
 * Code that is compiled to earlier Java levels can compile against this interface
 * and use it to conditionally invoke into the Java 21 operations only when
 * running on Java 21+. This interface also checks for embedder's implementation of ThreadTypeOverride to disable creation of VirtualThreads.
 *
 * TODO Once Java 8, 11, and 17 are no longer supported, this interface should be
 * removed in favor of directly using the API from Java.
 */
public interface VirtualThreadOps {

    /**
     * Delegates to:
     *
     * <pre>
     * Thread.ofVirtual()
     * .name(namePrefix, initialCountValue)
     * .inheritInheritableThreadLocals(inherit)
     * .uncaughtExceptionHandler(uncaughtHandler) // if uncaughtHandler is not null
     * .factory();
     *
     * <pre>
     *
     * @param namePrefix        prefix for thread names
     * @param initialCountValue initial value of the count that is appended to thread names
     * @param inherit           indicates whether created threads inherit the initial values of inheritable-thread-local variables
     * @param uncaughtHandler   if not null, this is set as the uncaughtExceptionHandler on the Thread.Builder
     * @return new thread factory for virtual threads.
     * @throws UnsupportedOperationException if running on less than Java SE 21.
     */
    ThreadFactory createFactoryOfVirtualThreads(String namePrefix,
                                                long initialCountValue,
                                                boolean inherit,
                                                UncaughtExceptionHandler uncaughtHandler);

    /**
     * Delegates to:
     *
     * <pre>
     * Thread.ofVirtual()
     * .name(name)
     * .inheritInheritableThreadLocals(inherit)
     * .uncaughtExceptionHandler(uncaughtHandler) // if uncaughtHandler is not null
     * .unstarted(runnable);
     *
     * <pre>
     *
     * @param name            name of the thread to create.
     * @param inherit         indicates whether created threads inherit the initial values of inheritable-thread-local variables
     * @param uncaughtHandler if not null, this is set as the uncaughtExceptionHandler on the Thread.Builder
     * @param runnable        action that the thread runs when it starts.
     * @return a new virtual thread that has not been started yet.
     * @throws UnsupportedOperationException if running on less than Java SE 21.
     */
    Thread createVirtualThread(String name,
                               boolean inherit,
                               UncaughtExceptionHandler uncaughtHandler,
                               Runnable runnable);

    /**
     * Indicates whether or not virtual threads are supported by the java level.
     *
     * @return true if virtual threads are supported, otherwise false.
     */
    boolean isSupported();

    /**
     * Indicates whether or not virtual threads should be created by checking if a ThreadTypeOverride exists
     *
     * @return true if virtual threads should be created, otherwise false.
     */
    boolean isVirtualThreadCreationEnabled();

    /**
     * Invokes <code>isVirtual</code> on the supplied Thread.
     *
     * @param thread thread instance.
     * @return true if the supplied thread is a virtual thread, otherwise false.
     * @throws UnsupportedOperationException if running on less than Java SE 21.
     */
    boolean isVirtual(Thread thread);
}