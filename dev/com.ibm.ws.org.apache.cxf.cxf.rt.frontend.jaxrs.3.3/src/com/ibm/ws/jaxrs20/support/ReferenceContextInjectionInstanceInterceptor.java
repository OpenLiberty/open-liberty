/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.support;

import java.util.Map;

import com.ibm.ws.jaxrs20.support.JaxRsInstanceManager.InstanceInterceptor;
import com.ibm.ws.jaxrs20.support.JaxRsInstanceManager.InterceptException;
import com.ibm.ws.jaxrs20.support.JaxRsInstanceManager.InterceptorContext;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 *
 */
public class ReferenceContextInjectionInstanceInterceptor implements InstanceInterceptor {

    private final Map<Class<?>, ReferenceContext> referenceContextMap;

    public ReferenceContextInjectionInstanceInterceptor(Map<Class<?>, ReferenceContext> referenceContextMap) {
        this.referenceContextMap = referenceContextMap;
    }

    @Override
    public void postNewInstance(InterceptorContext ctx) throws InterceptException {
        try {
            Object instance = ctx.getInstance();
            ReferenceContext referenceContext = referenceContextMap.get(instance.getClass());
            InjectionTarget[] injectionTargets = referenceContext.getInjectionTargets(instance.getClass());
            if (injectionTargets == null || injectionTargets.length == 0) {
                return;
            }
            for (InjectionTarget injectionTarget : injectionTargets) {
                injectionTarget.inject(instance, null);
            }
        } catch (InjectionException e) {
            throw new InterceptException(e);
        }
    }

    @Override
    public void postInjectInstance(InterceptorContext ctx) {}

    @Override
    public void preDestroyInstance(InterceptorContext ctx) throws InterceptException {}

}
