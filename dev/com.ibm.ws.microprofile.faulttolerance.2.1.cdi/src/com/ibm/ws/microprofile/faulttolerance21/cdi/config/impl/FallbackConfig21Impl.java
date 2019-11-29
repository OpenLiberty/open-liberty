/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance21.cdi.config.impl;

import java.lang.reflect.Method;

import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Fallback;

import com.ibm.ws.microprofile.faulttolerance.cdi.config.impl.FallbackConfigImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;

public class FallbackConfig21Impl extends FallbackConfigImpl {

    private final AnnotationParameterConfig<Class<? extends Throwable>[]> applyOnConfig = getParameterConfigClassArray("applyOn", Throwable.class);
    private final AnnotationParameterConfig<Class<? extends Throwable>[]> skipOnConfig = getParameterConfigClassArray("skipOn", Throwable.class);

    public FallbackConfig21Impl(Method annotatedMethod, Class<?> annotatedClass, Fallback annotation) {
        super(annotatedMethod, annotatedClass, annotation);
    }

    public FallbackConfig21Impl(Class<?> annotatedClass, Fallback annotation) {
        super(annotatedClass, annotation);
    }

    private Class<? extends Throwable>[] applyOn() {
        return applyOnConfig.getValue();
    }

    private Class<? extends Throwable>[] skipOn() {
        return skipOnConfig.getValue();
    }

    @Override
    public FallbackPolicy generatePolicy(InvocationContext context, BeanManager beanManager) {
        FallbackPolicy FallbackPolicy = super.generatePolicy(context, beanManager);
        Class<? extends Throwable>[] applyOn = applyOn();
        Class<? extends Throwable>[] skipOn = skipOn();
        FallbackPolicy.setApplyOn(applyOn);
        FallbackPolicy.setSkipOn(skipOn);
        return FallbackPolicy;
    }

}
