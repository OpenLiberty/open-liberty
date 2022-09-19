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
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;

import io.openliberty.data.Data;
import io.openliberty.data.internal.QueryHandler;

public class RepositoryProducer<T> implements Producer<T> {
    private final Bean<T> bean;
    private final BeanManager beanMgr;
    private final Class<?> entityClass;

    public RepositoryProducer(Bean<T> bean, BeanManager beanMgr, Class<?> entityClass) {
        System.out.println("Producer created for " + bean + ". Entity is " + entityClass);
        this.bean = bean;
        this.beanMgr = beanMgr;
        this.entityClass = entityClass;
    }

    @Override
    public void dispose(T obj) {
        System.out.println("Producer.dispose for " + obj);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public T produce(CreationalContext<T> cc) {
        @SuppressWarnings("unchecked")
        Class<T> c = (Class<T>) bean.getBeanClass();
        System.out.println("Producer.produce for " + c + " with " + c.getAnnotation(Data.class));

        InterceptionFactory<T> interception = beanMgr.createInterceptionFactory(cc, c);

        boolean intercept = false;
        for (AnnotatedMethodConfigurator<? super T> method : beanMgr.createInterceptionFactory(cc, c).configure().methods())
            for (Annotation anno : method.getAnnotated().getAnnotations())
                if ("jakarta.enterprise.concurrent.Asynchronous".equals(anno.annotationType().getName())) {
                    intercept = true;
                    method.add(anno);
                    System.out.println("Add " + anno + " for " + method.getAnnotated().getJavaMember());
                }

        T instance = c.cast(Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] { c }, new QueryHandler<T>(bean, entityClass)));
        return intercept ? interception.createInterceptedInstance(instance) : instance;
    }
}