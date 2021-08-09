/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config;

import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;

/**
 * Factory for creating annotation config objects
 * <p>
 * Different versions of Fault Tolerance have different rules about what values are valid so different versions of Fault Tolerance will provide different implementations of this
 * interface which create different config implementations.
 */
public interface AnnotationConfigFactory {

    public AsynchronousConfig createAsynchronousConfig(Method annotatedMethod, Class<?> annotatedClass, Asynchronous annotation);

    public AsynchronousConfig createAsynchronousConfig(Class<?> annotatedClass, Asynchronous annotation);

    public CircuitBreakerConfig createCircuitBreakerConfig(Method annotatedMethod, Class<?> annotatedClass, CircuitBreaker annotation);

    public CircuitBreakerConfig createCircuitBreakerConfig(Class<?> annotatedClass, CircuitBreaker annotation);

    public FallbackConfig createFallbackConfig(Method annotatedMethod, Class<?> annotatedClass, Fallback annotation);

    public FallbackConfig createFallbackConfig(Class<?> annotatedClass, Fallback annotation);
}
