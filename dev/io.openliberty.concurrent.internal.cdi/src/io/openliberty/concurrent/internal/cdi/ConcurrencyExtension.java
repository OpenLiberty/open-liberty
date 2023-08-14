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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
import jakarta.inject.Named;

public class ConcurrencyExtension implements Extension {
    private static final TraceComponent tc = Tr.register(ConcurrencyExtension.class);

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
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (ConcurrencyExtensionMetadata.eeVersion.getMajor() >= 11) {
            CDI<Object> cdi = CDI.current();

            for (Set<Annotation> qualifiers : contextServiceQualifiers) {
                if (cdi.select(ContextService.class, qualifiers.toArray(new Annotation[qualifiers.size()])).isResolvable()) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "ContextService already exists with qualifiers " + qualifiers);
                } else {
                    // It doesn't already exist, so try to add it:
                    String filter = null;
                    if (DEFAULT_QUALIFIER.equals(qualifiers))
                        filter = "(id=DefaultContextService)";
                    else // TODO replace with spec solution. Temporarily using @Named value to map to @ContextServiceDefinition name.
                        for (Annotation q : qualifiers)
                            if (Named.class.equals(q.annotationType())) {
                                String name = ((Named) q).value();
                                filter = new StringBuilder(name.length() + 21).append("(id=contextService[").append(name).append("])").toString();
                            }

                    if (filter == null) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "No producer or ContextServiceDefinition for qualifiers " + qualifiers);
                    } else {
                        Bean<ContextService> bean = new ConcurrencyResourceBean<>(ContextService.class, //
                                        filter, //
                                        Set.of(ContextService.class), //
                                        qualifiers);
                        event.addBean(bean);

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Added ContextService bean with qualifiers " + qualifiers);
                    }
                }
            }
            contextServiceQualifiers.clear();

            for (Set<Annotation> qualifiers : executorQualifiers) {
                if (cdi.select(ManagedExecutorService.class, qualifiers.toArray(new Annotation[qualifiers.size()])).isResolvable()) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "ManagedExecutorService already exists with qualifiers " + qualifiers);
                } else {
                    // It doesn't already exist, so try to add it:
                    String filter = null;
                    if (DEFAULT_QUALIFIER.equals(qualifiers))
                        filter = "(id=DefaultManagedExecutorService)";
                    else // TODO replace with spec solution. Temporarily using @Named value to map to @ContextServiceDefinition name.
                        for (Annotation q : qualifiers)
                            if (Named.class.equals(q.annotationType())) {
                                String name = ((Named) q).value();
                                filter = new StringBuilder(name.length() + 29).append("(id=managedExecutorService[").append(name).append("])").toString();
                            }

                    if (filter == null) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "No producer or ManagedExecutorDefinition for qualifiers " + qualifiers);
                    } else {
                        Bean<ManagedExecutorService> bean = new ConcurrencyResourceBean<>(ManagedExecutorService.class, //
                                        filter, //
                                        Set.of(ManagedExecutorService.class, ExecutorService.class, Executor.class), //
                                        qualifiers);
                        // TODO should ExecutorService.class, Executor.class be removed? If not, must avoid collisions with application's producers
                        event.addBean(bean);

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Added ManagedExecutorService bean with qualifiers " + qualifiers);
                    }
                }
            }
            executorQualifiers.clear();

            for (Set<Annotation> qualifiers : scheduledExecutorQualifiers) {
                if (cdi.select(ManagedScheduledExecutorService.class, qualifiers.toArray(new Annotation[qualifiers.size()])).isResolvable()) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "ManagedScheduledExecutorService already exists with qualifiers " + qualifiers);
                } else {
                    // It doesn't already exist, so try to add it:
                    String filter = null;
                    if (DEFAULT_QUALIFIER.equals(qualifiers))
                        filter = "(id=DefaultManagedScheduledExecutorService)";
                    else // TODO replace with spec solution. Temporarily using @Named value to map to @ManagedScheduledExecutorDefinition name.
                        for (Annotation q : qualifiers)
                            if (Named.class.equals(q.annotationType())) {
                                String name = ((Named) q).value();
                                filter = new StringBuilder(name.length() + 38).append("(id=managedScheduledExecutorService[").append(name).append("])").toString();
                            }

                    if (filter == null) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "No producer or ManagedScheduledExecutorDefinition for qualifiers " + qualifiers);
                    } else {
                        Bean<ManagedScheduledExecutorService> bean = new ConcurrencyResourceBean<>(ManagedScheduledExecutorService.class, //
                                        filter, //
                                        Set.of(ManagedScheduledExecutorService.class, ScheduledExecutorService.class), //
                                        qualifiers);
                        // TODO should ScheduledExecutorService.class be removed? If not, must avoid collisions with application's producers
                        event.addBean(bean);

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Added ManagedScheduledExecutorService bean with qualifiers " + qualifiers);
                    }
                }
            }
            scheduledExecutorQualifiers.clear();
        }
    }
}