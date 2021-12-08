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
package com.ibm.ws.artifact.zip.internal;

import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

/**
 * Utilities for invoking {@link System} methods.
 */
public class SystemUtils {
    /**
     * Run {@link System#getProperty(String)}.
     *
     * @param propertyName The name of the property which is to be retrieved.

     * @return The string property value.  Null if the property is not set.
     */
    public static String getProperty(final String propertyName) {
        return System.getProperty(propertyName);
    }

    /**
     * Get the system time in nano-seconds.
     *
     * @return The system time in nano-seconds.
     */
    public static long getNanoTime() {
        return System.nanoTime();
    }

    /**
     * Add a shutdown hook as a privileged action.
     *
     * See {@link Runtime#addShutdownHook(Thread)}.
     *
     * @param shutdownHook The thread to add as a shutdown hook.
     */
    public static void addShutdownHook(final Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
}
