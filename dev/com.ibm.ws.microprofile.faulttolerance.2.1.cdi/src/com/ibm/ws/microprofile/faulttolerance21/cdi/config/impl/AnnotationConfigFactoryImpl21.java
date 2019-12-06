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

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;

import com.ibm.ws.microprofile.faulttolerance.cdi.config.AnnotationConfigFactory;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.AsynchronousConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.CircuitBreakerConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.FallbackConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.impl.AsynchronousConfigImpl20;

/**
 * Factory for creating annotation config for FT 2.1
 * <p>
 * Features using this implementation should extend it and register it as a component
 */
public class AnnotationConfigFactoryImpl21 implements AnnotationConfigFactory {

    /** {@inheritDoc} */
    @Override
    public AsynchronousConfig createAsynchronousConfig(Method annotatedMethod, Class<?> annotatedClass, Asynchronous annotation) {
        return new AsynchronousConfigImpl20(annotatedMethod, annotatedClass, annotation);
    }

    /** {@inheritDoc} */
    @Override
    public AsynchronousConfig createAsynchronousConfig(Class<?> annotatedClass, Asynchronous annotation) {
        return new AsynchronousConfigImpl20(annotatedClass, annotation);
    }

    /** {@inheritDoc} */
    @Override
    public CircuitBreakerConfig createCircuitBreakerConfig(Method annotatedMethod, Class<?> annotatedClass, CircuitBreaker annotation) {
        return new CircuitBreakerConfig21Impl(annotatedMethod, annotatedClass, annotation);
    }

    /** {@inheritDoc} */
    @Override
    public CircuitBreakerConfig createCircuitBreakerConfig(Class<?> annotatedClass, CircuitBreaker annotation) {
        return new CircuitBreakerConfig21Impl(annotatedClass, annotation);
    }

    /** {@inheritDoc} */
    @Override
    public FallbackConfig createFallbackConfig(Method annotatedMethod, Class<?> annotatedClass, Fallback annotation) {
        return new FallbackConfig21Impl(annotatedMethod, annotatedClass, annotation);
    }

    /** {@inheritDoc} */
    @Override
    public FallbackConfig createFallbackConfig(Class<?> annotatedClass, Fallback annotation) {
        return new FallbackConfig21Impl(annotatedClass, annotation);
    }

}
