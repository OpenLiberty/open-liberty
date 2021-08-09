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
package com.ibm.ws.microprofile.config.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.AnnotationLiteral;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * DummyInjectionPoint is used to obtain a reference to the current InjectionPoint. It takes advantage of non-portable behaviour in Weld
 * where Weld will wrap an instance of this class in a proxy which returns the real information we want. None of the values returned by
 * this class are ever really used.
 */
@Trivial
public class DummyInjectionPoint implements InjectionPoint {

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return InjectionPoint.class;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("serial")
    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.<Annotation> singleton(new AnnotationLiteral<Default>() {});
    }

    /** {@inheritDoc} */
    @Override
    public Bean<?> getBean() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Member getMember() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Annotated getAnnotated() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDelegate() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTransient() {
        return false;
    }

}
