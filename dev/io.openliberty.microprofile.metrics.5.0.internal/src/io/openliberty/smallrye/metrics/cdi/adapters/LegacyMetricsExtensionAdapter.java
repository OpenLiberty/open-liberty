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

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.enterprise.inject.spi.WithAnnotations;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Timed;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.metrics50.helper.Util;

/**
 *
 */
public class LegacyMetricsExtensionAdapter implements Extension {

    Class<?> srLegacyMetricsExtensionClass;
    Object srLegacymetricsExtensionObj;
    private static final TraceComponent tc = Tr.register(LegacyMetricsExtensionAdapter.class);

    public LegacyMetricsExtensionAdapter() {

        srLegacyMetricsExtensionClass = Util.SR_LEGACY_METRIC_REGISTRY_EXTENSION_CLASS;

        if (srLegacyMetricsExtensionClass == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The SmallRye LegacyMetricsExtension class was not resolved.");

                // Use an Error message like the following?!?!
                // Tr.error(tc, "MicroProfile Metrics encountered a class loading error.");
            }
        } else {
            /*
             * Create instance of underlying SmallRye class that we will proxy calls to from
             * this proxy
             */
            try {
                srLegacymetricsExtensionObj = srLegacyMetricsExtensionClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                /*
                 * If this fails, this is due to changed API. This is the issue with using
                 * reflection to load.
                 */
            }
        }

    }

    public Object getLegacyMetricExtensionObject() {
        return srLegacymetricsExtensionObj;
    }

    void registerAnnotatedTypes(@Observes BeforeBeanDiscovery bbd, BeanManager manager) {
        if (srLegacymetricsExtensionObj == null)
            return;
        try {

            Method method = srLegacymetricsExtensionObj.getClass().getMethod("registerAnnotatedTypesProxy",
                    BeforeBeanDiscovery.class, BeanManager.class);
            method.invoke(srLegacymetricsExtensionObj, bbd, manager);

            bbd.addAnnotatedType(manager.createAnnotatedType(MetricRegistryProducerAdapter.class),
                    LegacyMetricsExtensionAdapter.class.getName() + "_"
                            + MetricRegistryProducerAdapter.class.getName());

            bbd.addAnnotatedType(manager.createAnnotatedType(MetricProducerAdapter.class),
                    LegacyMetricsExtensionAdapter.class.getName() + "_"
                            + MetricRegistryProducerAdapter.class.getName());

        } catch (Exception e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }

    }

    <X> void findAnnotatedInterfaces(
            @Observes @WithAnnotations({ Counted.class, Gauge.class, Timed.class }) ProcessAnnotatedType<X> pat) {
        if (srLegacymetricsExtensionObj == null)
            return;
        try {

            Method method = srLegacymetricsExtensionObj.getClass().getMethod("findAnnotatedInterfaces",
                    ProcessAnnotatedType.class);
            method.invoke(srLegacymetricsExtensionObj, pat);

        } catch (Exception e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }

    <X> void applyMetricsBinding(@Observes @WithAnnotations({ Gauge.class }) ProcessAnnotatedType<X> pat) {
        if (srLegacymetricsExtensionObj == null)
            return;
        try {

            Method method = srLegacymetricsExtensionObj.getClass().getMethod("applyMetricsBinding",
                    ProcessAnnotatedType.class);
            method.invoke(srLegacymetricsExtensionObj, pat);

        } catch (Exception e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }

    <X> void findAnnotatedMethods(@Observes ProcessManagedBean<X> bean) {
        if (srLegacymetricsExtensionObj == null)
            return;
        try {

            Method method = srLegacymetricsExtensionObj.getClass().getMethod("findAnnotatedMethods",
                    ProcessManagedBean.class);
            method.invoke(srLegacymetricsExtensionObj, bean);

        } catch (Exception e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }

    void registerMetrics(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        try {

            Method method = srLegacymetricsExtensionObj.getClass().getMethod("registerMetrics",
                    AfterDeploymentValidation.class, BeanManager.class);
            method.invoke(srLegacymetricsExtensionObj, adv, manager);

        } catch (Exception e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }

}
