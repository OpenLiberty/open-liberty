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
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import com.ibm.ws.cdi.CDIServiceUtils;

import io.openliberty.concurrent.internal.cdi.interceptor.AsyncInterceptor;
import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
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
     * Set of qualifier lists found on ContextService injection points.
     */
    private final Set<Set<Annotation>> contextServiceQualifiers = new HashSet<>();

    /**
     * Set of qualifier lists found on ManagedExecutorService injection points.
     */
    private final Set<Set<Annotation>> executorQualifiers = new HashSet<>();

    /**
     * Set of qualifier lists found on ManagedScheduledExecutorService injection points.
     */
    private final Set<Set<Annotation>> scheduledExecutorQualifiers = new HashSet<>();

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
     * @Inject {@Qualifier1 @Qualifier2 ...} ManagedExecutorService executor;
     *
     * @param <T>   bean class that has the injection point
     * @param event event
     */
    public <T> void processContextServiceInjectionPoint(@Observes ProcessInjectionPoint<T, ContextService> event) {
        if (ConcurrencyExtensionMetadata.eeVersion.getMajor() >= 11) {
            InjectionPoint injectionPoint = event.getInjectionPoint();
            Set<Annotation> qualifiers = injectionPoint.getQualifiers();
            contextServiceQualifiers.add(qualifiers);
        }
    }

    /**
     * Invoked for each matching injection point:
     *
     * @Inject {@Qualifier1 @Qualifier2 ...} ManagedExecutorService executor;
     *
     * @param <T>   bean class that has the injection point
     * @param event event
     */
    public <T> void processExecutorInjectionPoint(@Observes ProcessInjectionPoint<T, ManagedExecutorService> event) {
        if (ConcurrencyExtensionMetadata.eeVersion.getMajor() >= 11) {
            InjectionPoint injectionPoint = event.getInjectionPoint();
            Set<Annotation> qualifiers = injectionPoint.getQualifiers();
            executorQualifiers.add(qualifiers);
        }
    }

    /**
     * Invoked for each matching injection point:
     *
     * @Inject {@Qualifier1 @Qualifier2 ...} ManagedScheduledExecutorService scheduledExecutor;
     *
     * @param <T>   bean class that has the injection point
     * @param event event
     */
    public <T> void processScheduledExecutorInjectionPoint(@Observes ProcessInjectionPoint<T, ManagedScheduledExecutorService> event) {
        if (ConcurrencyExtensionMetadata.eeVersion.getMajor() >= 11) {
            InjectionPoint injectionPoint = event.getInjectionPoint();
            Set<Annotation> qualifiers = injectionPoint.getQualifiers();
            scheduledExecutorQualifiers.add(qualifiers);
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        if (ConcurrencyExtensionMetadata.eeVersion.getMajor() >= 11) {
            CDI<Object> cdi = CDI.current();

            for (Set<Annotation> qualifiers : contextServiceQualifiers) {
                if (cdi.select(ContextService.class, qualifiers.toArray(new Annotation[qualifiers.size()])).isResolvable()) {
                    System.out.println("ContextService with qualifiers " + qualifiers + " already exists.");
                } else {
                    // It doesn't already exist, so try to add it:
                    if (DEFAULT_QUALIFIER.equals(qualifiers)) {
                        Bean<ContextService> bean = new ConcurrencyResourceBean<>(ContextService.class, //
                                        "(id=DefaultContextService)", //
                                        Set.of(ContextService.class), //
                                        qualifiers);
                        event.addBean(bean);
                        System.out.println("Added ContextService with qualifiers " + qualifiers);
                    } // TODO else configured ContextService instances with qualifiers
                      // TODO if the same qualifiers list is used for both MES and MSES, create as MSES instead?
                }
            }
            contextServiceQualifiers.clear();

            for (Set<Annotation> qualifiers : executorQualifiers) {
                if (cdi.select(ManagedExecutorService.class, qualifiers.toArray(new Annotation[qualifiers.size()])).isResolvable()) {
                    System.out.println("ManagedExecutorService with qualifiers " + qualifiers + " already exists.");
                } else {
                    // It doesn't already exist, so try to add it:
                    if (DEFAULT_QUALIFIER.equals(qualifiers)) {
                        Bean<ManagedExecutorService> bean = new ConcurrencyResourceBean<>(ManagedExecutorService.class, //
                                        "(id=DefaultManagedExecutorService)", //
                                        Set.of(ManagedExecutorService.class, ExecutorService.class, Executor.class), //
                                        qualifiers);
                        event.addBean(bean);
                        System.out.println("Added ManagedExecutorService with qualifiers " + qualifiers);
                    } // TODO else configured ManagedExecutorService instances with qualifiers
                      // TODO if the same qualifiers list is used for both MES and MSES, create as MSES instead?
                }
            }
            executorQualifiers.clear();

            for (Set<Annotation> qualifiers : scheduledExecutorQualifiers) {
                if (cdi.select(ManagedScheduledExecutorService.class, qualifiers.toArray(new Annotation[qualifiers.size()])).isResolvable()) {
                    System.out.println("ManagedScheduledExecutorService with qualifiers " + qualifiers + " already exists.");
                } else {
                    // It doesn't already exist, so try to add it:
                    if (DEFAULT_QUALIFIER.equals(qualifiers)) {
                        Bean<ManagedScheduledExecutorService> bean = new ConcurrencyResourceBean<>(ManagedScheduledExecutorService.class, //
                                        "(id=DefaultManagedScheduledExecutorService)", //
                                        Set.of(ManagedScheduledExecutorService.class, ScheduledExecutorService.class,
                                               ManagedExecutorService.class, ExecutorService.class, Executor.class), // TODO will these collide?
                                        qualifiers);
                        event.addBean(bean);
                        System.out.println("Added ManagedScheduledExecutorService with qualifiers " + qualifiers);
                    } // TODO else configured ManagedScheduledExecutorService instances with qualifiers
                }
            }
            scheduledExecutorQualifiers.clear();
        }
    }
}