/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal.boot.templates;

import java.lang.reflect.Method;

/**
 * Template for a proxy implementation that notifies the monitoring
 * code where a new class has been initialized.
 */
public class ClassAvailableProxy {

    /**
     * Object instance that handles processing of probe candidates.
     */
    private static Object classAvailableTarget;

    /**
     * Method on {@link #classAvailableTarget} that implements the {@link #classAvailable} method.
     */
    private static Method classAvailableMethod;

    /**
     * Setup the proxy target.
     * 
     * @param target the object instance to call
     * @param method the method to invoke
     */
    final static void setClassAvailableTarget(Object target, Method method) {
        classAvailableTarget = target;
        classAvailableMethod = method;
    }

    /**
     * Notify the monitoring {@code classAvailable} target that a new class
     * instance is available.
     * 
     * @param clazz the initialized {@link Class}
     */
    public final static void classAvailable(Class<?> clazz) {
        Object target = classAvailableTarget;
        Method method = classAvailableMethod;
        if (target == null || method == null) {
            return;
        }
        try {
            method.invoke(target, clazz);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
