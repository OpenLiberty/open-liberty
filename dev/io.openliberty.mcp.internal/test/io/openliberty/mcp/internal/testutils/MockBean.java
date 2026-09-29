/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.testutils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

/**
 * Mock for CDI Bean interface
 */
public class MockBean<T> implements Bean<T> {

    private Class<T> clazz;

    private MockBean(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static <T> MockBean<T> of(Class<T> clazz) {
        return new MockBean<>(clazz);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.emptySet();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return Set.of(Object.class, clazz);
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public T create(CreationalContext<T> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy(T arg0, CreationalContext<T> arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getBeanClass() {
        return clazz;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

}
