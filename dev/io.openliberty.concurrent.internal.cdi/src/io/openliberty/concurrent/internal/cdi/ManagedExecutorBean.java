/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.resource.ResourceFactory;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;

/**
 * Bean that delegates to the OSGi service registry to obtain ManagedExecutorService resources.
 */
public class ManagedExecutorBean implements Bean<ManagedExecutorService>, PassivationCapable {
    private final static TraceComponent tc = Tr.register(ManagedExecutorBean.class);

    /**
     * Injectable bean types.
     */
    private final Set<Type> beanTypes = Set.of(ManagedExecutorService.class);

    /**
     * Resource factory that creates the resource.
     */
    private final ResourceFactory factory;

    /**
     * Qualifiers for the injection points for this bean.
     */
    private final Set<Annotation> qualifiers;

    /**
     * Construct a new bean for this resource.
     *
     * @param factory        resource factory.
     * @param qualifierNames names of qualifier annotations for the bean.
     */
    ManagedExecutorBean(ResourceFactory factory, List<String> qualifierNames) throws ClassNotFoundException {
        this.factory = factory;
        this.qualifiers = new LinkedHashSet<Annotation>();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        for (String qualifierClassName : qualifierNames) {
            Class<?> qualifierClass = loader.loadClass(qualifierClassName);
            if (!qualifierClass.isInterface())
                throw new IllegalArgumentException("The " + qualifierClassName + " class is not a valid qualifier class" +
                                                   " because it is not an annotation."); // TODO NLS
            qualifiers.add(Annotation.class.cast(Proxy.newProxyInstance(loader,
                                                                        new Class<?>[] { Annotation.class, qualifierClass },
                                                                        new QualifierProxy(qualifierClass))));
        }
    }

    /**
     * Construct a new bean for this resource.
     *
     * @param factory    resource factory.
     * @param qualifiers qualifiers for the bean.
     */
    ManagedExecutorBean(ResourceFactory factory, Set<Annotation> qualifiers) {
        this.factory = factory;
        this.qualifiers = qualifiers;
    }

    @Override
    @Trivial
    public ManagedExecutorService create(CreationalContext<ManagedExecutorService> cc) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "create", cc, factory, qualifiers);

        ManagedExecutorService instance;
        try {
            instance = (ManagedExecutorService) factory.createResource(null);
        } catch (RuntimeException x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "create", x);
            throw x;
        } catch (Exception x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "create", x);
            throw new RuntimeException(x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "create", instance);
        return instance;
    }

    @Override
    public void destroy(ManagedExecutorService instance, CreationalContext<ManagedExecutorService> creationalContext) {
    }

    @Override
    @Trivial
    public Class<ManagedExecutorService> getBeanClass() {
        return ManagedExecutorService.class;
    }

    /**
     * @return unique identifier for PassivationCapable.
     */
    @Override
    @Trivial
    public String getId() {
        return new StringBuilder(getClass().getName()) //
                        .append(":").append(qualifiers) //
                        .append(':').append(factory) //
                        .toString();
    }

    @Override
    @Trivial
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    @Trivial
    public String getName() {
        return null;
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
        return beanTypes;
    }

    @Override
    @Trivial
    public boolean isAlternative() {
        return false;
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())) //
                        .append(' ').append(factory) //
                        .append(" with qualifiers ").append(qualifiers) //
                        .toString();
    }
}