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
package com.ibm.ws.microprofile.graphql.component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.Timer.Context;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
@Dependent
@GraphQLApi
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class MetricsInterceptor {

    private final static String PREFIX = "mp_graphql_";
    private final static TraceComponent tc = Tr.register(MetricsInterceptor.class);
    final static MetricRegistry metrics;

    static {
        Supplier<MetricRegistry> metricsSupplier = null;
        if (canLoad("com.ibm.ws.microprofile.metrics.cdi.producer.MetricRegistryFactory")) {
            metricsSupplier = () -> {
                return com.ibm.ws.microprofile.metrics.cdi.producer.MetricRegistryFactory.getVendorRegistry();
            };
        } else if (canLoad("com.ibm.ws.microprofile.metrics.cdi20.producer.MetricRegistryFactory")) {
            metricsSupplier = () -> {
                return com.ibm.ws.microprofile.metrics.cdi20.producer.MetricRegistryFactory.getVendorRegistry();
            };
        }
        if (metricsSupplier != null) {
            metrics = metricsSupplier.get();
        } else {
            metrics = null;
        }
    }

    private static boolean canLoad(String className) {
        try {
            return Class.forName(className) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @AroundInvoke
    public Object captureMetrics(InvocationContext ctx) throws Exception {
        Method m = ctx.getMethod();
        String fqMethodName = PREFIX + m.getDeclaringClass().getName() + "." + m.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "invoking " + fqMethodName );
        }
        try (Context c = metrics.timer(fqMethodName + ".time").time()) {
            return ctx.proceed();
        } finally {
            Counter counter = metrics.counter(fqMethodName + ".count");
            counter.inc();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "invoked " + counter.getCount() + " times");
            }
        }
    }
}
