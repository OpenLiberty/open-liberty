/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.smallrye.metrics.cdi.adapters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.metrics50.helper.Util;

/**
 *
 */
@ApplicationScoped
public class MetricProducerAdapter {
    static Class<?> srMetricProducerClass;
    static Object srMetricProducerObj;
    private static final TraceComponent tc = Tr.register(MetricProducerAdapter.class);

    @Inject
    public MetricProducerAdapter(LegacyMetricsExtensionAdapter extensionAdapter) {
        srMetricProducerClass = Util.SR_METRICS_PRODUCER_CLASS;

        if (srMetricProducerClass == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The SmallRye MetricProducer class was not resolved.");

                // Use an Error message like the following?!?!
                // Tr.error(tc, "MicroProfile Metrics encountered a class loading error.");
            }
        } else {
            /*
             * Create instance of underlying SmallRye class that we will proxy calls to from
             * this proxy
             */
            try {
                srMetricProducerObj = srMetricProducerClass
                        .getConstructor(Util.SR_LEGACY_METRIC_REGISTRY_EXTENSION_CLASS)
                        .newInstance(Util.SR_LEGACY_METRIC_REGISTRY_EXTENSION_CLASS
                                .cast(extensionAdapter.getLegacyMetricExtensionObject()));

            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                /*
                 * If this fails, this is due to changed API. This is the issue with using
                 * reflection to load.
                 */
            }
        }
    }

    @Produces
    public <T extends Number> Gauge<T> getGauge(InjectionPoint ip) {
        if (srMetricProducerObj == null) {
            return null;
        }
        try {
            Method method = srMetricProducerObj.getClass().getMethod("getGauge", InjectionPoint.class);
            return (Gauge<T>) method.invoke(srMetricProducerObj, ip);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
        return null;

    }

    @Produces
    public Counter getCounter(InjectionPoint ip) {
        if (srMetricProducerObj == null) {
            return null;
        }
        try {
            Method method = srMetricProducerObj.getClass().getMethod("getCounter", InjectionPoint.class);
            return (Counter) method.invoke(srMetricProducerObj, ip);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
        return null;

    }

    @Produces
    public Timer getTimer(InjectionPoint ip) {
        if (srMetricProducerObj == null) {
            return null;
        }
        try {
            Method method = srMetricProducerObj.getClass().getMethod("getTimer", InjectionPoint.class);
            return (Timer) method.invoke(srMetricProducerObj, ip);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
        return null;

    }

    @Produces
    public Histogram getHistogram(InjectionPoint ip) {
        if (srMetricProducerObj == null) {
            return null;
        }
        try {
            Method method = srMetricProducerObj.getClass().getMethod("getHistogram", InjectionPoint.class);
            return (Histogram) method.invoke(srMetricProducerObj, ip);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
        return null;
    }
}
