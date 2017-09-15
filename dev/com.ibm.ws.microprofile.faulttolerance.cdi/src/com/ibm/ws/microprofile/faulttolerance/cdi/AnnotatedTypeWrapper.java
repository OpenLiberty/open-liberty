/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class AnnotatedTypeWrapper<T> extends AnnotatedWrapper implements AnnotatedType<T> {

    private static final TraceComponent tc = Tr.register(AnnotatedTypeWrapper.class);

    private final AnnotatedType<T> wrapped;
    private final Set<AnnotatedMethod<? super T>> methods = new HashSet<>();
    private final Map<AnnotatedMethod<? super T>, AnnotatedMethodWrapper<? super T>> wrappedMethods = new HashMap<>();

    public AnnotatedTypeWrapper(BeanManager beanManager, AnnotatedType<T> wrapped, boolean interceptedClass, Set<AnnotatedMethod<?>> interceptedMethods) {
        super(wrapped, interceptedClass);
        this.wrapped = wrapped;

        for (AnnotatedMethod<? super T> method : this.wrapped.getMethods()) {
            if (interceptedMethods.contains(method)) {
                AnnotatedType<?> declaringType = method.getDeclaringType();
                if (declaringType.equals(wrapped)) {
                    AnnotatedMethodWrapper<T> methodWrapper = new AnnotatedMethodWrapper<T>(this, (AnnotatedMethod<T>) method);
                    this.methods.add(methodWrapper);
                    this.wrappedMethods.put(method, methodWrapper);
                } else {
                    throw new RuntimeException(Tr.formatMessage(tc, "internal.error.CWMFT4999E"));
                }
            } else {
                this.methods.add(method);
            }
        }
    }

    private <X> AnnotatedTypeWrapper<X> newAnnotatedTypeWrapper(BeanManager beanManager, AnnotatedType<X> wrapped, Set<AnnotatedMethod<?>> interceptedMethods) {
        AnnotatedTypeWrapper<X> superTypeWrapper = new AnnotatedTypeWrapper<X>(beanManager, wrapped, false, interceptedMethods);
        return superTypeWrapper;
    }

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors() {
        return wrapped.getConstructors();
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        return wrapped.getFields();
    }

    @Override
    public Class<T> getJavaClass() {
        return wrapped.getJavaClass();
    }

    @Override
    public Set<AnnotatedMethod<? super T>> getMethods() {
        return methods;
    }

}
