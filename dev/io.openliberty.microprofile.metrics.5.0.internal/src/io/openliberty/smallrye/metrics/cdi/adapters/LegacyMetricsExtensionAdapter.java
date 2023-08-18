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

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Timed;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.metrics50.helper.Util;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.enterprise.inject.spi.WithAnnotations;

/**
 *
 */
public class LegacyMetricsExtensionAdapter implements Extension {

    private static final String FQ_V51_METRIC_REGISTRY_PRODUCER_ADAPTER = "io.openliberty.smallrye.metrics.cdi.adapters51.MetricRegistryProducerAdapter";

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

    @FFDCIgnore(ClassNotFoundException.class)
    void registerAnnotatedTypes(@Observes BeforeBeanDiscovery bbd, BeanManager manager) {
        if (srLegacymetricsExtensionObj == null)
            return;
        try {

            Method method = srLegacymetricsExtensionObj.getClass().getMethod("registerAnnotatedTypesProxy",
                    BeforeBeanDiscovery.class, BeanManager.class);
            method.invoke(srLegacymetricsExtensionObj, bbd, manager);

            bbd.addAnnotatedType(manager.createAnnotatedType(MetricProducerAdapter.class),
                    LegacyMetricsExtensionAdapter.class.getName() + "_"
                            + MetricRegistryProducerAdapter.class.getName());

            /*
             * Attempt to load the MetricRegistryProducerAdapter from the
             * io.openliberty.microprofile.metrics.5.1.internal bundle. If this succeeds, we
             * are running mpMetrics-5.1 then we will load this class and provide it to the
             * CDI runtime. We need to load this separately due to the change in 5.1
             * where @RegistryScope is a qualifier. This affects how the producer is
             * implemented.
             *
             * This works because the io.openliberty.microprofile.metrics.5.1.internal
             * bundle re-exports all packages + classes from this
             * io.openliberty.microprofile.metrics.5.0.internal bundle. Therefore, when
             * io.openliberty.microprofile.metrics.5.1.internal is active, this class will
             * be running in that bundle and have class visibility to
             * io.openliberty.smallrye.metrics.cdi.adapters51.MetricRegistryProducerAdapter.
             *
             * Furthermore, since this class itself is in the
             * io.openliberty.microprofile.metrics.5.0.internal bundle we wouldn't have
             * visibility to
             * io.openliberty.smallrye.metrics.cdi.adapters51.MetricRegistryProducerAdapter
             * during build time. Hence the use of reflection!
             *
             *
             * If that fails, that means we are running mpMetrics-5.0. And we will load the
             * MetricRegistryProducerAdapter class found in the
             * io.openliberty.microprofile.metrics.5.0.internal bundle
             */
            try {
                Class<?> metricRegistryProducerAdapter51Class = Class.forName(FQ_V51_METRIC_REGISTRY_PRODUCER_ADAPTER);
                bbd.addAnnotatedType(manager.createAnnotatedType(metricRegistryProducerAdapter51Class),
                        LegacyMetricsExtensionAdapter.class.getName() + "_"
                                + MetricRegistryProducerAdapter.class.getName());
                return;
            } catch (ClassNotFoundException exception) {
                // Nothing to do - we're not running mpMetrics-5.1
            }

            bbd.addAnnotatedType(manager.createAnnotatedType(MetricRegistryProducerAdapter.class),
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
