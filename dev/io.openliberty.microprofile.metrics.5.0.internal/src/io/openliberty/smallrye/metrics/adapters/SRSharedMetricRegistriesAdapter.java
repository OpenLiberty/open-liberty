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

import org.eclipse.microprofile.metrics.MetricRegistry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.metrics50.helper.Util;

/**
 *
 */
public class SRSharedMetricRegistriesAdapter {
    private static final TraceComponent tc = Tr.register(SRSharedMetricRegistriesAdapter.class);
    private static SRSharedMetricRegistriesAdapter instance;
    private static Method getOrCreateMethod = null;
    private static Method getOrCreateMethodAppNameResolver = null;
    private final Class<?> srSharedMetricRegistryClass;

    private SRSharedMetricRegistriesAdapter() {
        srSharedMetricRegistryClass = Util.SR_SHARED_METRIC_REGISTRIES_CLASS;

        if (srSharedMetricRegistryClass == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The SmallRye SharedMetricRegistries class was not resolved.");

                // Use an Error message like the following?!?!
                // Tr.error(tc, "MicroProfile Metrics encountered a class loading error.");
            }
        } else {
            try {
                getOrCreateMethod = this.srSharedMetricRegistryClass.getMethod("getOrCreate", String.class);

                getOrCreateMethodAppNameResolver = this.srSharedMetricRegistryClass.getMethod("getOrCreate",
                        String.class, Util.SR_APPLICATION_NAME_RESOLVER_INTERFACE);
            } catch (NoSuchMethodException | SecurityException e) {
                /*
                 * If this fails, this is due to changed API. This is the issue with using
                 * reflection to load.
                 */
            }
        }
    }

    public static synchronized SRSharedMetricRegistriesAdapter getInstance() {
        if (instance == null) {
            instance = new SRSharedMetricRegistriesAdapter();
        }
        return instance;
    }

    public MetricRegistry getOrCreate(String scope) {

        /*
         * If the class is null, no proxy was created or if Method was unable to be
         * resolved
         */
        if (srSharedMetricRegistryClass == null || getOrCreateMethod == null) {
            if (getOrCreateMethod == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                            "Unable to load by reflection the expected SharedMetricRegistries.getOrCreate(String) method");
                }
            }
            return null;
        }
        MetricRegistry metricRegistry = null;
        Object metricRegistryObject;
        try {
            metricRegistryObject = getOrCreateMethod.invoke(null, scope);

            if (metricRegistryObject != null) {
                metricRegistry = (MetricRegistry) metricRegistryObject;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to retrieve MetricRegistry for scope " + scope);

                    // MAYBE WARNING OR ERROR LIKE SO?
                    // Tr.error(tc, "Unable to retrieve MetricRegistry for scope {0}", scope);
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }

        return metricRegistry;
    }

    public MetricRegistry getOrCreate(String scope, Object o) {

        /*
         * If the class is null, no proxy was created or if Method was unable to be
         * resolved
         */
        if (srSharedMetricRegistryClass == null || getOrCreateMethodAppNameResolver == null) {

            if (getOrCreateMethodAppNameResolver == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                            "Unable to load by reflection the expected SharedMetricRegistries.getOrCreate(String, AppNameResolver) method");
                }
            }

            return null;
        }
        MetricRegistry metricRegistry = null;

        Object metricRegistryObject;
        try {
            metricRegistryObject = getOrCreateMethodAppNameResolver.invoke(null, scope,
                    Util.SR_APPLICATION_NAME_RESOLVER_INTERFACE.cast(o));

            if (metricRegistryObject != null) {
                metricRegistry = (MetricRegistry) metricRegistryObject;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to retrieve MetricRegistry for scope " + scope);

                    // MAYBE WARNING OR ERROR LIKE SO?
                    // Tr.error(tc, "Unable to retrieve MetricRegistry for scope {0}", scope);
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }

        return metricRegistry;
    }
}
