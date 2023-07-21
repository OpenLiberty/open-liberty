/*******************************************************************************
 * Copyright (c) 2021,2023 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.ibm.ws.cdi.CDIServiceUtils;

import io.openliberty.concurrent.internal.cdi.interceptor.AsyncInterceptor;
import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;

public class ConcurrencyExtension implements Extension {
    private static final Set<Annotation> DEFAULT_QUALIFIER = Set.of(Default.Literal.INSTANCE);

    /**
     * Set of qualifier lists found on injection points.
     */
    private final Set<Set<Annotation>> executorQualifiers = new HashSet<>();

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        // register the interceptor binding and the interceptor
        AnnotatedType<Asynchronous> bindingType = beanManager.createAnnotatedType(Asynchronous.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<AsyncInterceptor> interceptorType = beanManager.createAnnotatedType(AsyncInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));
    }

    /**
     * Invoked for each matching injection point:
     *
     * @Inject @Qualifier1 @Qualifier2 ...
     *         ManagedExecutorService executor;
     *
     * @param <T>         bean class that has the injection point
     * @param event       event
     * @param beanManager bean manager
     */
    public <T> void processExecutorInjectionPoint(@Observes ProcessInjectionPoint<T, ManagedExecutorService> event, BeanManager beanManager) {
        if (ConcurrencyExtensionMetadata.eeVersion.getMajor() >= 11) {
            // TODO check if producer already exists for the injection point and skip
            InjectionPoint injectionPoint = event.getInjectionPoint();
            Set<Annotation> qualifiers = injectionPoint.getQualifiers();
            executorQualifiers.add(qualifiers);
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        if (ConcurrencyExtensionMetadata.eeVersion.getMajor() >= 11) {
            for (Iterator<Set<Annotation>> it = executorQualifiers.iterator(); it.hasNext();) {
                Set<Annotation> qualifiers = it.next();
                it.remove();
                if (CDI.current().select(ManagedExecutorService.class, qualifiers.toArray(new Annotation[qualifiers.size()])).isUnsatisfied()) {
                    // It doesn't already exist, so try to add it:
                    if (DEFAULT_QUALIFIER.equals(qualifiers)) {
                        Bean<ManagedExecutorService> bean = new ConcurrencyResourceBean<>(ManagedExecutorService.class, //
                                        "(id=DefaultManagedExecutorService)", //
                                        Set.of(ManagedExecutorService.class, ExecutorService.class, Executor.class), //
                                        qualifiers);
                        event.addBean(bean);
                        System.out.println("Added " + bean.getBeanClass().getName() + " with qualifiers " + qualifiers);
                    } // TODO else configured ManagedExecutorService instances with qualifiers
                }
            }
        }
    }
}