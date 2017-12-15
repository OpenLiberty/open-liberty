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
package com.ibm.ws.security.wim;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Some helper methods for working with {@link AccessController} operations.
 */
public class AccessControllerHelper {

    /**
     * Convenience method to get the context ClassLoader for the current thread
     * using {@link AccessController#doPrivileged(PrivilegedAction)}.
     *
     * @return The {@link ClassLoader} returned from
     *         {@link Thread#currentThread()#getContextClassLoader()}.
     */
    public static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    /**
     * Convenience method to get a system property using
     * {@link AccessController#doPrivileged(PrivilegedAction)}.
     *
     * @param property The property to retrieve.
     * @return The value returned from {@link System#getProperty(String)}.
     */
    public static String getSystemProperty(final String property) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(property);
            }
        });
    }

    /**
     * Convenience method to set the context ClassLoader for the current thread
     * using {@link AccessController#doPrivileged(PrivilegedAction)}.
     *
     * @param clazz The class to get the {@link ClassLoader} to set when calling.
     *            {@link Thread#currentThread()#setContextClassLoader(ClassLoader)}.
     */
    public static void setContextClassLoader(final Class<?> clazz) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
                return null;
            }
        });
    }

    /**
     * Convenience method to set the context ClassLoader for the current thread
     * using {@link AccessController#doPrivileged(PrivilegedAction)}.
     *
     * @param classLoader The {@link ClassLoader} to set when calling.
     *            {@link Thread#currentThread()#setContextClassLoader(ClassLoader)}.
     */
    public static void setContextClassLoader(final ClassLoader classLoader) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(classLoader);
                return null;
            }
        });
    }
}
