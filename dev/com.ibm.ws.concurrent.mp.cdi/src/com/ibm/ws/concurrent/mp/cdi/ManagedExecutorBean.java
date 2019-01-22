/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.NamedInstance;

import com.ibm.websphere.ras.annotation.Trivial;

public class ManagedExecutorBean implements Bean<ManagedExecutor>, PassivationCapable {

    private static final Type[] TYPE_ARR = { ManagedExecutor.class, ExecutorService.class, Executor.class };
    private static final Set<Type> TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(TYPE_ARR)));

    private final String injectionPointName;
    private final String instanceName;
    private final Set<Annotation> qualifiers;
    private final ManagedExecutor executor;

    public ManagedExecutorBean(String injectionPointName, String instanceName, ManagedExecutor executor) {
        Objects.requireNonNull(injectionPointName);
        Objects.requireNonNull(instanceName);
        this.injectionPointName = injectionPointName;
        this.instanceName = instanceName;
        this.executor = executor;
        Set<Annotation> qualifiers = new HashSet<>(2);
        qualifiers.add(Any.Literal.INSTANCE);
        qualifiers.add(NamedInstance.Literal.of(instanceName));
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
    }

    @Override
    public ManagedExecutor create(CreationalContext<ManagedExecutor> cc) {
        return executor;
    }

    @Override
    public void destroy(ManagedExecutor me, CreationalContext<ManagedExecutor> cc) {
        me.shutdown();
    }

    @Override
    @Trivial
    public String getName() {
        return instanceName; // TODO should change this to null because @Named qualifier is not present ?
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
        return ManagedExecutor.class;
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
        return this.getClass().getSimpleName() + '-' + getName();
    }

}
