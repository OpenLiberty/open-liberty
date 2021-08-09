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

import java.lang.reflect.Method;
import java.util.List;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedMethodWrapper<T> extends AnnotatedWrapper implements AnnotatedMethod<T> {

    private final AnnotatedMethod<T> wrapped;
    private final AnnotatedType<T> declaringType;

    public AnnotatedMethodWrapper(AnnotatedType<T> declaringType, AnnotatedMethod<T> wrapped) {
        super(wrapped, true);
        this.declaringType = declaringType;
        this.wrapped = wrapped;
    }

    /** {@inheritDoc} */
    @Override
    public List<AnnotatedParameter<T>> getParameters() {
        return wrapped.getParameters();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isStatic() {
        return wrapped.isStatic();
    }

    /** {@inheritDoc} */
    @Override
    public AnnotatedType<T> getDeclaringType() {
        return declaringType;
    }

    /** {@inheritDoc} */
    @Override
    public Method getJavaMember() {
        return wrapped.getJavaMember();
    }

}
