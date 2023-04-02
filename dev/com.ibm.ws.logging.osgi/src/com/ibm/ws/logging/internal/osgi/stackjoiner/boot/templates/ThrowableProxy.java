/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.logging.internal.osgi.stackjoiner.boot.templates;

import java.io.PrintStream;
import java.lang.reflect.Method;

/**
 * ThrowableProxy is used by code running in the bootstrap classloader to access a method in BaseTraceService
 */
public final class ThrowableProxy {

    /**
     * The method to be fired upon
     */
    private static Method fireMethod;

    /**
     * The class object in which the method is fired
     */
    private static Object fireTarget;

    /**
     * Sets the fire target
     *
     * @param target instance in which the method resides
     * @param method to be fired
     */
    public final static void setFireTarget(Object target, Method method) {
        fireTarget = target;
        fireMethod = method;
    }

    /**
     * Invokes the method that attempts a printStackTrace override
     * 
     * @return true if Throwable.printStackTrace(PrintStream) is overriden, false otherwise
     */
    public final static boolean fireMethod(Throwable t, PrintStream originalStream) {
    	Boolean b = Boolean.FALSE;
        try {
            b = (Boolean) fireMethod.invoke(fireTarget, t, originalStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

}
