/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.cdi;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.DataProvider;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.ProducerFactory;
import jakarta.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;

/**
 * Producer for repository implementation that is provided by the container/runtime.
 *
 * @param <R> repository interface.
 */
public class RepositoryProducer<R, P> implements Producer<R> {
    private final static TraceComponent tc = Tr.register(RepositoryProducer.class);

    /**
     * Factory class for repository producers.
     */
    @Trivial
    static class Factory<P> implements ProducerFactory<P> {
        private final BeanManager beanMgr;
        private final Class<?> entityClass;
        private final DataProvider provider;

        Factory(BeanManager beanMgr, DataProvider provider, Class<?> entityClass) {
            this.beanMgr = beanMgr;
            this.entityClass = entityClass;
            this.provider = provider;
        }

        @Override
        public <R> Producer<R> createProducer(Bean<R> bean) {
            return new RepositoryProducer<>(bean, this);
        }
    }

    private final Bean<R> bean;
    private final Factory<P> factory;

    public RepositoryProducer(Bean<R> bean, Factory<P> factory) {
        this.bean = bean;
        this.factory = factory;
    }

    @Override
    public void dispose(R repository) {
        // TODO
    }

    @Override
    @Trivial
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    @Trivial
    public R produce(CreationalContext<R> cc) {
        @SuppressWarnings("unchecked")
        Class<R> repositoryInterface = (Class<R>) bean.getBeanClass();

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "produce", cc, repositoryInterface.getName());

        InterceptionFactory<R> interception = factory.beanMgr.createInterceptionFactory(cc, repositoryInterface);

        boolean intercept = false;
        for (AnnotatedMethodConfigurator<? super R> method : factory.beanMgr.createInterceptionFactory(cc, repositoryInterface).configure().methods())
            for (Annotation anno : method.getAnnotated().getAnnotations())
                if ("jakarta.enterprise.concurrent.Asynchronous".equals(anno.annotationType().getName())) {
                    intercept = true;
                    method.add(anno);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "add " + anno + " for " + method.getAnnotated().getJavaMember());
                }

        R instance = factory.provider.createRepository(repositoryInterface, factory.entityClass);

        instance = intercept ? interception.createInterceptedInstance(instance) : instance;

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "produce", instance.toString());
        return instance;
    }
}