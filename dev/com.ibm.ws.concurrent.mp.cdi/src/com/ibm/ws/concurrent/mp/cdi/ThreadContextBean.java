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
package com.ibm.ws.concurrent.mp.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.eclipse.microprofile.concurrent.NamedInstance;
import org.eclipse.microprofile.concurrent.ThreadContext;

import com.ibm.websphere.ras.annotation.Trivial;

public class ThreadContextBean implements Bean<ThreadContext>, PassivationCapable {

    private static final Set<Type> TYPES = Collections.singleton(ThreadContext.class);

    private final String injectionPointName;
    private final Set<Annotation> qualifiers;
    private final ThreadContext threadContext;

    public ThreadContextBean(String injectionPointName, String instanceName, ThreadContext threadContext) {
        this.injectionPointName = injectionPointName;
        this.threadContext = threadContext;
        Set<Annotation> qualifiers = new HashSet<>(2);
        qualifiers.add(Any.Literal.INSTANCE);
        qualifiers.add(NamedInstance.Literal.of(instanceName));
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
    }

    @Override
    public ThreadContext create(CreationalContext<ThreadContext> cc) {
        return threadContext;
    }

    @Override
    @Trivial
    public void destroy(ThreadContext threadContext, CreationalContext<ThreadContext> cc) {}

    @Override
    @Trivial
    public String getName() {
        return null; // because @Named qualifier is not present
    }

    @Override
    @Trivial
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    @Trivial
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    @Trivial
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    @Trivial
    public Set<Type> getTypes() {
        return TYPES;
    }

    @Override
    @Trivial
    public boolean isAlternative() {
        return false;
    }

    @Override
    @Trivial
    public String getId() {
        return injectionPointName;
    }

    @Override
    @Trivial
    public Class<?> getBeanClass() {
        return ThreadContext.class;
    }

    @Override
    @Trivial
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    @Trivial
    public boolean isNullable() {
        return false;
    }

    @Override
    @Trivial
    public String toString() {
        return getClass().getSimpleName() + '-' + getId();
    }
}
