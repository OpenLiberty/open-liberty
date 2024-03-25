/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import io.openliberty.data.internal.persistence.QueryInfo;
import io.openliberty.data.internal.persistence.RepositoryImpl;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.ProducerFactory;
import jakarta.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

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
        private final EntityManagerBuilder entityManagerBuilder;
        private final DataExtension extension;
        private RepositoryImpl<?> handler;
        private final ReentrantReadWriteLock handlerLock = new ReentrantReadWriteLock();
        private final Class<?> primaryEntityClass;
        private final DataExtensionProvider provider;
        private final Map<Class<?>, List<QueryInfo>> queriesPerEntityClass;
        private final Class<?> repositoryInterface;

        Factory(Class<?> repositoryInterface, BeanManager beanMgr, DataExtensionProvider provider, DataExtension extension,
                EntityManagerBuilder entityManagerBuilder, Class<?> primaryEntityClass, Map<Class<?>, List<QueryInfo>> queriesPerEntityClass) {
            this.beanMgr = beanMgr;
            this.entityManagerBuilder = entityManagerBuilder;
            this.extension = extension;
            this.primaryEntityClass = primaryEntityClass;
            this.provider = provider;
            this.queriesPerEntityClass = queriesPerEntityClass;
            this.repositoryInterface = repositoryInterface;
        }

        @Override
        public <R> Producer<R> createProducer(Bean<R> bean) {
            return new RepositoryProducer<>(bean, this);
        }

        /**
         * Lazily initialize the repository implementation.
         * TODO This could be moved to produce if we use CDI to guarantee only a single instance is produced.
         *
         * @return repository implementation.
         */
        private RepositoryImpl<?> getHandler() {
            handlerLock.readLock().lock();
            try {
                if (handler == null)
                    try {
                        // Switch to write lock for lazy initialization
                        handlerLock.readLock().unlock();
                        handlerLock.writeLock().lock();

                        if (handler == null)
                            handler = new RepositoryImpl<>(provider, extension, entityManagerBuilder, //
                                            repositoryInterface, primaryEntityClass, queriesPerEntityClass);
                    } finally {
                        // Downgrade to read lock for rest of method
                        handlerLock.readLock().lock();
                        handlerLock.writeLock().unlock();
                    }

                return handler;
            } finally {
                handlerLock.readLock().unlock();
            }
        }
    }

    private final Bean<R> bean;
    private final Factory<P> factory;
    private final Map<R, R> intercepted = new ConcurrentHashMap<>();

    public RepositoryProducer(Bean<R> bean, Factory<P> factory) {
        this.bean = bean;
        this.factory = factory;
    }

    @Override
    public void dispose(R repository) {
        R r = intercepted.remove(repository);
        if (r != null)
            repository = r;

        RepositoryImpl<?> handler = (RepositoryImpl<?>) Proxy.getInvocationHandler(repository);
        handler.beanDisposed();
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
        AnnotatedTypeConfigurator<R> configurator = interception.configure();
        for (Annotation anno : configurator.getAnnotated().getAnnotations())
            if (factory.beanMgr.isInterceptorBinding(anno.annotationType())) {
                intercept = true;
                configurator.add(anno);
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "add " + anno + " for " + configurator.getAnnotated().getJavaClass());
            }
        for (AnnotatedMethodConfigurator<? super R> method : configurator.methods())
            for (Annotation anno : method.getAnnotated().getAnnotations())
                if (factory.beanMgr.isInterceptorBinding(anno.annotationType())) {
                    intercept = true;
                    method.add(anno);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "add " + anno + " for " + method.getAnnotated().getJavaMember());
                }

        R instance = repositoryInterface.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(),
                                                                     new Class<?>[] { repositoryInterface },
                                                                     factory.getHandler()));

        if (intercept) {
            R r = interception.createInterceptedInstance(instance);
            intercepted.put(r, instance);
            instance = r;
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "produce", instance.toString());
        return instance;
    }
}