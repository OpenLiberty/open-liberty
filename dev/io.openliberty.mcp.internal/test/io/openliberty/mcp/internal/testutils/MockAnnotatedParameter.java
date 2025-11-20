/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.testutils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;

import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;

/**
 * Mock for the CDI AnnotatedParamter which uses reflection
 */
public class MockAnnotatedParameter<X> extends MockAnnotated implements AnnotatedParameter<X> {

    Parameter parameter;
    AnnotatedMethod<X> parent;
    int position;

    public MockAnnotatedParameter(Parameter parameter, AnnotatedMethod<X> parent, int position) {
        this.parameter = parameter;
        this.parent = parent;
        this.position = position;
    }

    @Override
    public AnnotatedCallable<X> getDeclaringCallable() {
        return parent;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    protected AnnotatedElement getAnnotatedElement() {
        return parameter;
    }

}
