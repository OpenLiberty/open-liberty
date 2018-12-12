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
package com.ibm.ws.concurrent.mp.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutor.Builder;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;

public class ManagedExecutorBean implements Bean<ManagedExecutor>, PassivationCapable {

    private static final Type[] TYPE_ARR = { ManagedExecutor.class, ExecutorService.class, Executor.class };
    private static final Set<Type> TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(TYPE_ARR)));

    private final String name;
    private final Set<Annotation> qualifiers;
    private final ManagedExecutorConfig config;

    public ManagedExecutorBean() {
        this.name = getClass().getCanonicalName();
        this.config = null;
        Set<Annotation> qualifiers = new HashSet<>(2);
        qualifiers.add(Any.Literal.INSTANCE);
        qualifiers.add(Default.Literal.INSTANCE);
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
    }

    public ManagedExecutorBean(String name, ManagedExecutorConfig config) {
        Objects.requireNonNull(name);
        this.name = name;
        this.config = config;
        Set<Annotation> qualifiers = new HashSet<>(2);
        qualifiers.add(Any.Literal.INSTANCE);
        qualifiers.add(ConcurrencyCDIExtension.createNamedInstance(this.name));
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
    }

    @Override
    public ManagedExecutor create(CreationalContext<ManagedExecutor> cc) {
        Builder b = ManagedExecutor.builder();
        if (config != null) {
            b.maxAsync(config.maxAsync());
            b.maxQueued(config.maxQueued());
            b.propagated(config.propagated());
            b.cleared(config.cleared());
        }
        return b.build();
    }

    @Override
    public void destroy(ManagedExecutor me, CreationalContext<ManagedExecutor> cc) {
        me.shutdown();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
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
        return TYPES;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public Class<?> getBeanClass() {
        return ManagedExecutor.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '-' + getName();
    }

}
