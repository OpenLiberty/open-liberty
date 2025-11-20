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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;

/**
 * Mock for the CDI AnnotatedMethod which uses reflection
 */
public class MockAnnotatedMethod<X> extends MockAnnotated implements AnnotatedMethod<X> {

    private Method method;

    public MockAnnotatedMethod(Method method) {
        this.method = method;
    }

    @Override
    public List<AnnotatedParameter<X>> getParameters() {
        Parameter[] types = method.getParameters();
        List<AnnotatedParameter<X>> result = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            result.add(new MockAnnotatedParameter<>(types[i], this, i));
        }
        return result;
    }

    @Override
    public AnnotatedType<X> getDeclaringType() {
        return null;
    }

    @Override
    public boolean isStatic() {
        return (method.getModifiers() & Modifier.STATIC) != 0;
    }

    @Override
    public Method getJavaMember() {
        return method;
    }

    /** {@inheritDoc} */
    @Override
    protected AnnotatedElement getAnnotatedElement() {
        return method;
    }

}
