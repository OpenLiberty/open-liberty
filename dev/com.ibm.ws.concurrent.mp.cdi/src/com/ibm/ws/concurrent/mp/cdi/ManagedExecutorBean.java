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
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutor.Builder;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.NamedInstance;

public class ManagedExecutorBean implements Bean<ManagedExecutor>, PassivationCapable {

    private static final Type[] TYPE_ARR = { ManagedExecutor.class, ExecutorService.class, Executor.class };
    private static final Set<Type> TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(TYPE_ARR)));

    // This instance is used as a marker for when no configured is specified for a String[]. The reference is compared; its content does not matter.
    private static final String[] UNSPECIFIED_ARRAY = new String[] {};

    private final String name;
    private final Set<Annotation> qualifiers;
    private final ManagedExecutorConfig config;
    private final ConcurrencyCDIExtension cdiExtension;

    public ManagedExecutorBean(ConcurrencyCDIExtension cdiExtension) {
        this.name = getClass().getCanonicalName();
        this.config = null;
        this.cdiExtension = cdiExtension;
        Set<Annotation> qualifiers = new HashSet<>(2);
        qualifiers.add(Any.Literal.INSTANCE);
        qualifiers.add(Default.Literal.INSTANCE);
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
    }

    public ManagedExecutorBean(String name, ManagedExecutorConfig config, ConcurrencyCDIExtension cdiExtension) {
        Objects.requireNonNull(name);
        this.name = name;
        this.config = config;
        this.cdiExtension = cdiExtension;
        Set<Annotation> qualifiers = new HashSet<>(2);
        qualifiers.add(Any.Literal.INSTANCE);
        qualifiers.add(NamedInstance.Literal.of(this.name));
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
    }

    @Override
    public ManagedExecutor create(CreationalContext<ManagedExecutor> cc) {
        Builder b = ManagedExecutor.builder();
        MPConfigAccessor configAccessor = cdiExtension.mpConfigAccessor;
        if (configAccessor == null) {
            if (config != null) {
                b.maxAsync(config.maxAsync());
                b.maxQueued(config.maxQueued());
                b.propagated(config.propagated());
                b.cleared(config.cleared());
            }
        } else {
            Object mpConfig = cdiExtension.mpConfig;

            int start = name.length() + 1;
            int len = start + 10;
            StringBuilder propName = new StringBuilder(len).append(name).append('.');

            // In order to efficiently reuse StringBuilder, properties are added in the order of the length of their names,

            propName.append("cleared");
            String[] c = configAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.cleared());
            if (c != UNSPECIFIED_ARRAY)
                b.cleared(c);

            propName.replace(start, len, "maxAsync");
            Integer a = configAccessor.get(mpConfig, propName.toString(), config == null ? null : config.maxAsync());
            if (a != null)
                b.maxAsync(a);

            propName.replace(start, len, "maxQueued");
            Integer q = configAccessor.get(mpConfig, propName.toString(), config == null ? null : config.maxQueued());
            if (q != null)
                b.maxQueued(q);

            propName.replace(start, len, "propagated");
            String[] p = configAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.propagated());
            if (p != UNSPECIFIED_ARRAY)
                b.propagated(p);
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
