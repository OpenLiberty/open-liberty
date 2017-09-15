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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A Utility class for FT
 */
public class FTUtils {
    public final static String FALLBACKHANDLE_METHOD_NAME = "handle";
    public final static String PROXY_CLASS_SIGNATURE = "$Proxy$_$$_WeldSubclass";
    public final static String ENV_NONFALLBACK_ENABLED = "MP_Fault_Tolerance_NonFallback_Enabled";

    /**
     * Return whether the class is a weld proxy
     *
     * @param clazz
     * @return true if it is a proxy
     */
    public static boolean isWeldProxy(Class<?> clazz) {
        boolean result = clazz.getSimpleName().contains(PROXY_CLASS_SIGNATURE);
        return result;

    }

    /**
     * Get the real class. If it is proxy, get its superclass, which will be the real class.
     *
     * @param clazz
     * @return the real class.
     */
    public static Class<?> getRealClass(Class<?> clazz) {
        Class<?> realClazz = clazz;
        if (isWeldProxy(clazz)) {
            realClazz = clazz.getSuperclass();
        }
        return realClazz;
    }

    /**
     * Get the classloader for the given class
     *
     * @param clazz
     * @return the class' classloader.
     */
    public static ClassLoader getClassLoader(Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return clazz.getClassLoader();
            }
        });
    }
}
