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

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

/**
 * Utilities for invoking {@link System} methods.
 */
public class SystemUtils {
    /**
     * Run {@link System#getProperty(String)} as a privileged action.
     *
     * @param propertyName The name of the property which is to be retrieved.

     * @return The string property value.  Null if the property is not set.
     */
    public static String getProperty(final String propertyName) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged( new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(propertyName);
                }

            } );
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Run {@link System#getNanoTime} as a privileged action.
     *
     * @return The system time in nano-seconds.
     */
    public static long getNanoTime() {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged( new PrivilegedAction<Long>() {
                @Override
                public Long run() {
                    return System.nanoTime();
                }
            } ).longValue();
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Add a shutdown hook as a privileged action.
     *
     * See {@link Runtime#addShutdownHook(Thread)}.
     *
     * @param shutdownHook The thread to add as a shutdown hook.
     */
    public static void addShutdownHook(final Thread shutdownHook) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            AccessController.doPrivileged( new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Runtime.getRuntime().addShutdownHook(shutdownHook);
                    return null; // Nothing to return
                }
            } );
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }
}
