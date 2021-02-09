/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics.internal.cdi30.interceptors;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;

import io.astefanutti.metrics.cdi30.MetricResolver;
import io.openliberty.microprofile.metrics.internal.cdi30.helper.Utils;

@ConcurrentGauge
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
public class ConcurrentGaugeInterceptor {

    private final Bean<?> bean;

    private final MetricRegistry registry;

    private final MetricResolver resolver;

    @Inject
    private ConcurrentGaugeInterceptor(@Intercepted Bean<?> bean, MetricRegistry registry, MetricResolver resolver) {
        this.bean = bean;
        this.registry = registry;
        this.resolver = resolver;
    }

    @AroundConstruct
    private Object concurrentGaugeConstructor(InvocationContext context) throws Exception {
        return concurrentGaugeCallable(context, context.getConstructor());
    }

    @AroundInvoke
    private Object concurrentGaugeMethod(InvocationContext context) throws Exception {
        return concurrentGaugeCallable(context, context.getMethod());
    }

    @AroundTimeout
    private Object concurrentGaugeTimeout(InvocationContext context) throws Exception {
        return concurrentGaugeCallable(context, context.getMethod());
    }

    private <E extends Member & AnnotatedElement> Object concurrentGaugeCallable(InvocationContext context, E element) throws Exception {
        MetricResolver.Of<ConcurrentGauge> concurrentGaugeAnno = resolver.concurentGauged(bean.getBeanClass(), element);
        MetricID tmid = new MetricID(concurrentGaugeAnno.metricName(), Utils.tagsToTags(concurrentGaugeAnno.tags()));
        org.eclipse.microprofile.metrics.ConcurrentGauge concurrentGauge = (org.eclipse.microprofile.metrics.ConcurrentGauge) registry.getMetric(tmid);
        if (concurrentGauge == null)
            throw new IllegalStateException("No concurrent gauge with metricID [" + tmid + "] found in registry [" + registry + "]");

        concurrentGauge.inc();
        try {
            return context.proceed();
        } finally {
            concurrentGauge.dec();
        }
    }
}
