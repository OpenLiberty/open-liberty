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
 * Source level implementation of the boot delegation proxy. This
 * class will be massaged at runtime into a form that's visible to
 * all bundles' class space.
 */
public final class ProbeProxy {

    /**
     * Object instance that is handles the fireProbe method.
     */
    private static Object fireProbeTarget;

    /**
     * Method on {@link fireProbeTarget} that implements the {@code fireProbe} method.
     */
    private static Method fireProbeMethod;

    /**
     * Set up the method to be reflectively invoked where firing a probe.
     * 
     * @param target the object instance that handles {@code fireProbe}
     * @param fireMethod the method that implements {@code fireProbe}
     */
    final static void setFireProbeTarget(Object target, Method method) {
        fireProbeTarget = target;
        fireProbeMethod = method;
    }

    /**
     * Fire a probe event to the registered target.
     * 
     * @param probeId the generated probe identifier
     * @param instance the object instance emitting the probe or null
     * @param args the probe payload
     */
    public final static void fireProbe(long probeId, Object instance, Object target, Object args) {
        // Load statics onto the stack to avoid a window where they can be cleared
        // between the test for null and the invocation of the method
        Object proxyTarget = fireProbeTarget;
        Method method = fireProbeMethod;
        if (proxyTarget == null || method == null) {
            return;
        }
        try {
            method.invoke(proxyTarget, probeId, instance, target, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
