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

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Producer;

import io.openliberty.data.Data;

public class BeanProducer<T> implements Producer<T> {
    private final Bean<T> bean;

    public BeanProducer(Bean<T> bean) {
        System.out.println("Producer created for " + bean);
        this.bean = bean;
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

        return c.cast(Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] { c }, new QueryHandler<T>(c)));
    }
}