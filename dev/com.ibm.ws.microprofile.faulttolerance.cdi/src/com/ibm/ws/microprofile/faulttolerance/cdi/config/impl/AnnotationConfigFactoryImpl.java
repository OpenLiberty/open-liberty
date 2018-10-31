/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config.impl;

import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import com.ibm.ws.microprofile.faulttolerance.cdi.config.AnnotationConfigFactory;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.AsynchronousConfig;

/**
 * Factory for creating annotation config for FT 1.0
 * <p>
 * Features using this implementation should extend it and register it as a component
 */
public class AnnotationConfigFactoryImpl implements AnnotationConfigFactory {

    /** {@inheritDoc} */
    @Override
    public AsynchronousConfig createAsynchronousConfig(Method annotatedMethod, Class<?> annotatedClass, Asynchronous annotation) {
        return new AsynchronousConfigImpl(annotatedMethod, annotatedClass, annotation);
    }

    /** {@inheritDoc} */
    @Override
    public AsynchronousConfig createAsynchronousConfig(Class<?> annotatedClass, Asynchronous annotation) {
        return new AsynchronousConfigImpl(annotatedClass, annotation);
    }

}
