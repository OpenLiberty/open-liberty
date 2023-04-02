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
package io.openliberty.smallrye.metrics.adapters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.ToDoubleFunction;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class SRMetricRegistryAdapter {
    private static final TraceComponent tc = Tr.register(SRMetricRegistryAdapter.class);
    private Method addNameToApplicationMapMethod = null;
    private Method unRegisterApplicationMetricsMethod_StringParam = null;
    private Method functionCounter = null;
    private final Object srMetricRegistryObject;

    /**
     * Creates the SRMetricRegistryAdapter which is the proxy of the SmallRye
     * Metrics LegacyMetricRegistry. Callers of SrMetricRegistryAdapter are those
     * that need access to specific methods available in the LegacyMetricRegistry
     * and not the MP Metrics {@link MetricRegistry}.
     *
     * Casters of this are expected to receive a {@link MetricRegistry} Object and
     * attempt to cast it to LegacyMetricRegistry first.
     * <p>
     * Example
     *
     * <pre>
     * try {
     *     Object cast = Util.SR_LEGACY_METRIC_REGISTRY_CLASS.cast(metricRegistry);
     *     SRMetricRegistryAdapter srMetricRegistry = new SRMetricRegistryAdapter(cast);
     *     ...
     * } catch (ClassCastException e) {
     *     // exception handling
     * }
     * </pre>
     *
     * @param metricRegistryObject
     */
    public SRMetricRegistryAdapter(Object metricRegistryObject) {
        srMetricRegistryObject = metricRegistryObject;

        try {
            addNameToApplicationMapMethod = srMetricRegistryObject.getClass().getMethod("addNameToApplicationMap",
                    MetricID.class);

            functionCounter = srMetricRegistryObject.getClass().getMethod("counter", Metadata.class, Object.class,
                    ToDoubleFunction.class, Tag[].class);

            unRegisterApplicationMetricsMethod_StringParam = srMetricRegistryObject.getClass()
                    .getMethod("unRegisterApplicationMetrics", String.class);
        } catch (NoSuchMethodException | SecurityException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }

    }

    public void addNameToApplicationMap(MetricID metricID) {
        try {
            if (addNameToApplicationMapMethod == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                            "Unable to load by reflection the expected LegacyMetricRegistry.addNameToApplicationMap(MetricID) method.");
                }
                return;
            }
            addNameToApplicationMapMethod.invoke(srMetricRegistryObject, metricID);
            // NullPointer to catch if Method was not reflectively loaded properly
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }

    public void unRegisterApplicationMetrics(String name) {
        try {
            if (unRegisterApplicationMetricsMethod_StringParam == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                            "Unable to load by reflection the expected LegacyMetricRegistry.unRegisterApplicationMetrics(String) method.");
                }
                return;
            }

            unRegisterApplicationMetricsMethod_StringParam.invoke(srMetricRegistryObject, name);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }

    public <T> void functionCounter(Metadata metadata, T obj, ToDoubleFunction<T> function, Tag... tags) {
        try {
            if (functionCounter == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                            "Unable to load by reflection the expected LegacyMetricRegistry.counter(Metadata, T, ToDoubleFunction<T>, Tag...) method.");
                }
                return;
            }
            functionCounter.invoke(srMetricRegistryObject, metadata, obj, function, tags);
            // NullPointer to catch if Method was not reflectively loaded properly
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }
}
