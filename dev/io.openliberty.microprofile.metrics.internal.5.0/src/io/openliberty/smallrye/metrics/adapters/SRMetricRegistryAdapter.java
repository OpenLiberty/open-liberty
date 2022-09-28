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
package io.openliberty.smallrye.metrics.adapters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.ToDoubleFunction;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Tag;

/**
 *
 */
public class SRMetricRegistryAdapter {
    private Method addNameToApplicationMapMethod = null;
    private Method unRegisterApplicationMetricsMethod_StringParam = null;
    private Method functionCounter = null;
    private final Object srMetricRegistryObject;

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
            addNameToApplicationMapMethod.invoke(srMetricRegistryObject, metricID);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }

    public void unRegisterApplicationMetrics(String name) {
        try {
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
            functionCounter.invoke(srMetricRegistryObject, metadata, obj, function, tags);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }
}
