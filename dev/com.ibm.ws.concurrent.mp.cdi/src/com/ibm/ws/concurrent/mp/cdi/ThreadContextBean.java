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
import org.eclipse.microprofile.concurrent.ThreadContext.Builder;
import org.eclipse.microprofile.concurrent.ThreadContextConfig;

import com.ibm.websphere.ras.annotation.Trivial;

public class ThreadContextBean implements Bean<ThreadContext>, PassivationCapable {

    private static final Set<Type> TYPES = Collections.singleton(ThreadContext.class);

    // This instance is used as a marker for when no configured is specified for a String[]. The reference is compared; its content does not matter.
    private static final String[] UNSPECIFIED_ARRAY = new String[] {};

    private final String injectionPointName;
    private final Set<Annotation> qualifiers;
    private final ThreadContextConfig config;
    private final ConcurrencyCDIExtension cdiExtension;

    public ThreadContextBean(String injectionPointName, String instanceName, ThreadContextConfig config, ConcurrencyCDIExtension cdiExtension) {
        this.injectionPointName = injectionPointName;
        this.config = config;
        this.cdiExtension = cdiExtension;
        Set<Annotation> qualifiers = new HashSet<>(2);
        qualifiers.add(Any.Literal.INSTANCE);
        qualifiers.add(NamedInstance.Literal.of(instanceName));
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
    }

    @Override
    public ThreadContext create(CreationalContext<ThreadContext> cc) {
        Builder b = ThreadContext.builder();
        MPConfigAccessor configAccessor = cdiExtension.mpConfigAccessor;
        if (configAccessor == null) {
            if (config != null) {
                b.cleared(config.cleared());
                b.propagated(config.propagated());
                b.unchanged(config.unchanged());
            }
        } else {
            Object mpConfig = cdiExtension.mpConfig;

            int start = injectionPointName.length() + 1;
            int len = start + 10;
            StringBuilder propName = new StringBuilder(len).append(injectionPointName).append('.');

            // In order to efficiently reuse StringBuilder, properties are added in the order of the length of their names,

            propName.append("cleared");
            String[] c = configAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.cleared());
            if (c != UNSPECIFIED_ARRAY)
                b.cleared(c);

            propName.replace(start, len, "unchanged");
            String[] u = configAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.unchanged());
            if (u != UNSPECIFIED_ARRAY)
                b.unchanged(u);

            propName.replace(start, len, "propagated");
            String[] p = configAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.propagated());
            if (p != UNSPECIFIED_ARRAY)
                b.propagated(p);
        }

        return b.build();
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
