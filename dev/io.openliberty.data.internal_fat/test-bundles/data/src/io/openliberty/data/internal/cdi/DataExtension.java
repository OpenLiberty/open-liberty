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

import java.util.ArrayList;
import java.util.HashSet;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;

import io.openliberty.data.Data;

public class DataExtension implements Extension {
    private final ArrayList<Bean<?>> beans = new ArrayList<>();
    private final HashSet<AnnotatedType<?>> beanTypes = new HashSet<>();

    public <T> void processAnnotatedType(@Observes @WithAnnotations(Data.class) ProcessAnnotatedType<T> event) {
        System.out.println("processAnnotatedType");

        AnnotatedType<T> type = event.getAnnotatedType();
        System.out.println("    found " + type.getAnnotation(Data.class) + " on " + type.getJavaClass());
        beanTypes.add(type);
    }

    public void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanMgr) {
        System.out.println("afterTypeDiscovery");

        for (AnnotatedType<?> type : beanTypes) {
            BeanAttributes<?> attrs = beanMgr.createBeanAttributes(type);
            beans.add(beanMgr.createBean(attrs, type.getJavaClass(), new BeanProducerFactory<>()));
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        System.out.println("afterBeanDiscovery");

        for (Bean<?> bean : beans) {
            System.out.println("    adding " + bean);
            event.addBean(bean);
        }
    }
}