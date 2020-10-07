/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************
 * Copyright © 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.cdi20.interceptors;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.cdi.binding.MetricsBinding;
import com.ibm.ws.microprofile.metrics.cdi20.helper.Utils;
import com.ibm.ws.microprofile.metrics.cdi20.producer.MetricRegistryFactory;

import io.astefanutti.metrics.cdi20.MetricResolver;
import io.astefanutti.metrics.cdi20.MetricsExtension;

@Interceptor
@MetricsBinding
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class MetricsInterceptor {

    private static final TraceComponent tc = Tr.register(MetricsInterceptor.class);

    private final MetricRegistry registry;

    private final MetricResolver resolver;

    private final MetricsExtension extension;

    @Inject
    private MetricsInterceptor(MetricRegistry registry, MetricResolver resolver, MetricsExtension extension) {
        this.registry = registry;
        this.resolver = resolver;
        this.extension = extension;
    }

    @AroundConstruct
    private Object metrics(InvocationContext context) throws Exception {
        Class<?> bean = context.getConstructor().getDeclaringClass();

        // We'll skip re-visiting methods that we already created.
        // We don't skip Gauges because they should fail if re-registered
        if (!extension.getBeansVisited().contains(bean)) {
            extension.getBeansVisited().add(bean);
            // Registers the bean constructor metrics
            registerMetrics(bean, context.getConstructor());

            // Registers the methods metrics over the bean type hierarchy
            Class<?> type = bean;
            do {
                // TODO: discover annotations declared on implemented interfaces
                for (Method method : getDeclaredMethods(type))
                    if (!method.isSynthetic() && !Modifier.isPrivate(method.getModifiers()))
                        registerMetrics(bean, method);
                type = type.getSuperclass();
            } while (!Object.class.equals(type));

        }

        Object target = context.proceed();

        // Registers the gauges over the bean type hierarchy after the target is constructed as it is required for the gauge invocations
        Class<?> type = bean;
        do {
            // TODO: discover annotations declared on implemented interfaces
            for (Method method : getDeclaredMethods(type)) {
                MetricResolver.Of<Gauge> gauge = resolver.gauge(bean, method);
                if (gauge.isPresent()) {
                    registry.register(gauge.metadata(), new ForwardingGauge(method, context.getTarget()), Utils.tagsToTags(gauge.tags()));
                    MetricID mid = new MetricID(gauge.metadata().getName(), Utils.tagsToTags(gauge.tags()));
                    extension.addMetricID(mid);
                }
            }
            type = type.getSuperclass();
        } while (!Object.class.equals(type));

        return target;
    }

    private <E extends Member & AnnotatedElement> void registerMetrics(Class<?> bean, E element) {
        MetricResolver.Of<Counted> counted = resolver.counted(bean, element);
        if (counted.isPresent()) {
            registry.counter(counted.metadata(), Utils.tagsToTags(counted.tags()));
            MetricID mid = new MetricID(counted.metadata().getName(), Utils.tagsToTags(counted.tags()));
            extension.addMetricID(element, counted.metricAnnotation(), mid);
        }

        MetricResolver.Of<ConcurrentGauge> concurrentGauge = resolver.concurentGauged(bean, element);
        if (concurrentGauge.isPresent()) {
            registry.concurrentGauge(concurrentGauge.metadata(), Utils.tagsToTags(concurrentGauge.tags()));
            MetricID mid = new MetricID(concurrentGauge.metadata().getName(), Utils.tagsToTags(concurrentGauge.tags()));
            extension.addMetricID(element, concurrentGauge.metricAnnotation(), mid);
        }

        MetricResolver.Of<Metered> metered = resolver.metered(bean, element);
        if (metered.isPresent()) {
            registry.meter(metered.metadata(), Utils.tagsToTags(metered.tags()));
            MetricID mid = new MetricID(metered.metadata().getName(), Utils.tagsToTags(metered.tags()));
            extension.addMetricID(element, metered.metricAnnotation(), mid);
        }

        MetricResolver.Of<Timed> timed = resolver.timed(bean, element);
        if (timed.isPresent()) {
            registry.timer(timed.metadata(), Utils.tagsToTags(timed.tags()));
            MetricID mid = new MetricID(timed.metadata().getName(), Utils.tagsToTags(timed.tags()));
            extension.addMetricID(element, timed.metricAnnotation(), mid);
        }
    }

    private static final class ForwardingGauge implements org.eclipse.microprofile.metrics.Gauge<Object> {

        private final Method method;

        private final Object object;

        private ForwardingGauge(Method method, Object object) {
            this.method = method;
            this.object = object;
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                method.setAccessible(true);
                return null;
            });
        }

        @FFDCIgnore({ IllegalStateException.class })
        @Override
        public Object getValue() {
            try {
                return invokeMethod(method, object);
            } catch (IllegalStateException e) {
                // This is likely due to an Application being unloaded, we need to unregister the invalid metric
                // Need to use Factory to get registry.
                MetricRegistryFactory.getApplicationRegistry().removeMatching(new MetricFilter() {

                    @Override
                    public boolean matches(MetricID metricID, Metric metric) {
                        if (metric.equals(ForwardingGauge.this)) {
                            Tr.warning(tc, "cannot.resolve.metric.warning.CWMMC3000W", metricID.getName());
                            return true;
                        }
                        return false;
                    }
                });
                throw e;
            }
        }
    }

    @FFDCIgnore({ InvocationTargetException.class })
    private static Object invokeMethod(Method method, Object object) {
        try {
            return method.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException cause) {
            throw new IllegalStateException("Error while calling method [" + method + "]", cause);
        }
    }

    private static Method[] getDeclaredMethods(Class<?> type) {
        if (System.getSecurityManager() == null) {
            return type.getDeclaredMethods();
        }
        return AccessController.doPrivileged((PrivilegedAction<Method[]>) () -> {
            return type.getDeclaredMethods();
        });
    }
}
