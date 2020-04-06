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

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import com.ibm.ws.microprofile.faulttolerance.cdi.config.impl.CircuitBreakerConfigImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;

/**
 *
 */
public class CircuitBreakerConfig21Impl extends CircuitBreakerConfigImpl {

    private final AnnotationParameterConfig<Class<? extends Throwable>[]> skipOnConfig = getParameterConfigClassArray("skipOn", Throwable.class);

    public CircuitBreakerConfig21Impl(Class<?> annotatedClass, CircuitBreaker annotation) {
        super(annotatedClass, annotation);
    }

    public CircuitBreakerConfig21Impl(Method annotatedMethod, Class<?> annotatedClass, CircuitBreaker annotation) {
        super(annotatedMethod, annotatedClass, annotation);
    }

    private Class<? extends Throwable>[] skipOn() {
        return skipOnConfig.getValue();
    }

    @Override
    public CircuitBreakerPolicy generatePolicy() {
        CircuitBreakerPolicy circuitBreakerPolicy = super.generatePolicy();
        Class<? extends Throwable>[] skipOn = skipOn();
        circuitBreakerPolicy.setSkipOn(skipOn);
        return circuitBreakerPolicy;
    }

}
