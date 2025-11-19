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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.inject.spi.Annotated;

/**
 * Mock for the CDI Annotated interface
 */
public abstract class MockAnnotated implements Annotated {

    abstract protected AnnotatedElement getAnnotatedElement();

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> type) {
        return getAnnotatedElement().getAnnotation(type);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return Set.of(getAnnotatedElement().getAnnotations());
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> type) {
        return getAnnotatedElement().isAnnotationPresent(type);
    }

    @Override
    public Type getBaseType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Type> getTypeClosure() {
        // TODO Auto-generated method stub
        return null;
    }

}
