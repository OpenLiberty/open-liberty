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
package io.openliberty.smallrye.metrics.cdi.adapters51;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryScope;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.metrics50.helper.Util;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

/**
 *
 */
@ApplicationScoped
public class MetricRegistryProducerAdapter {
    private static final TraceComponent tc = Tr.register(MetricRegistryProducerAdapter.class);
    static Class<?> srMetricsProducerClass;
    static Object srMetricsProducerObj;

    static {
        // MetricRegistry.Type.
        srMetricsProducerClass = Util.SR_METRIC_REGISTRY_PRODUCER_CLASS;

        if (srMetricsProducerClass == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The SmallRye MetricRegistryProducer class was not resolved.");
                // Use an Error message like the following?!?!
                // Tr.error(tc, "MicroProfile Metrics encountered a class loading error.");
            }
        } else {

            /*
             * Create instance of underlying SmallRye class that we will proxy calls to from
             * this proxy
             */
            try {
                srMetricsProducerObj = srMetricsProducerClass.getConstructor().newInstance();
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
    @RegistryScope
    @Default
    public MetricRegistry getMetricRegistry(InjectionPoint ip) {
        if (srMetricsProducerObj == null)
            return null;

        MetricRegistry mr = null;
        try {
            Method method = srMetricsProducerObj.getClass().getMethod("getMetricRegistry", InjectionPoint.class);
            mr = (MetricRegistry) method.invoke(srMetricsProducerObj, ip);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
        return mr;
    }

    @Produces
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    public static MetricRegistry getApplicationRegistry() {
        if (srMetricsProducerObj == null)
            return null;

        MetricRegistry mr = null;
        try {
            Method method = srMetricsProducerObj.getClass().getMethod("getApplicationRegistry");
            mr = (MetricRegistry) method.invoke(srMetricsProducerObj);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
        return mr;
    }

    @Produces
    @RegistryType(type = MetricRegistry.Type.BASE)
    public static MetricRegistry getBaseRegistry() {
        if (srMetricsProducerObj == null)
            return null;

        MetricRegistry mr = null;
        try {
            Method method = srMetricsProducerObj.getClass().getMethod("getBaseRegistry");
            mr = (MetricRegistry) method.invoke(srMetricsProducerObj);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
        return mr;
    }

    @Produces
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    public static MetricRegistry getVendorRegistry() {
        if (srMetricsProducerObj == null)
            return null;

        MetricRegistry mr = null;
        try {
            Method method = srMetricsProducerObj.getClass().getMethod("getVendorRegistry");
            mr = (MetricRegistry) method.invoke(srMetricsProducerObj);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
        return mr;
    }
}
