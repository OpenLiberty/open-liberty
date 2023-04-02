/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.faulttolerance30.internal.metrics30.integration;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance.utils.DummyMetricRecorder;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

@Component(configurationPolicy = IGNORE)
public class MetricRecorderProvider30Impl implements MetricRecorderProvider {

    private final static SecureAction secureAction = AccessController.doPrivileged(SecureAction.get());

    /**
     * Map from Classloader to whether metrics are disabled for that classloader
     * <p>
     * Must hold lock on {@link #recorders} before reading or writing
     */
    private final Map<ClassLoader, Boolean> metricsEnabledCache = new WeakHashMap<>();

    private final static String CONFIG_METRICS_ENABLED = "MP_Fault_Tolerance_Metrics_Enabled";

    @Reference
    protected SharedMetricRegistries sharedRegistries;

    /**
     * Map from method to the recorder for that method
     * <p>
     * Must hold lock on this object before reading or writing
     */
    private final WeakHashMap<Method, MetricRecorder> recorders = new WeakHashMap<>();

    /** {@inheritDoc} */
    @Override
    public MetricRecorder getMetricRecorder(Method method, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy, TimeoutPolicy timeoutPolicy,
                                            BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync) {
        synchronized (recorders) {
            MetricRecorder recorder = recorders.get(method);
            if (recorder == null) {
                recorder = createNewRecorder(method, retryPolicy, circuitBreakerPolicy, timeoutPolicy, bulkheadPolicy, fallbackPolicy, isAsync);
                recorders.put(method, recorder);
            }
            return recorder;
        }
    }

    private MetricRecorder createNewRecorder(Method method, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy, TimeoutPolicy timeoutPolicy,
                                             BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync) {
        if (isMetricsEnabled(method.getDeclaringClass())) {
            MetricRegistry registry = sharedRegistries.getOrCreate(MetricRegistry.Type.BASE.getName());
            return new MetricRecorder30Metrics30Impl(getMetricName(method), registry, retryPolicy, circuitBreakerPolicy, timeoutPolicy, bulkheadPolicy, fallbackPolicy, isAsync);
        } else {
            return DummyMetricRecorder.get();
        }
    }

    private String getMetricName(Method method) {
        String name = method.getDeclaringClass().getCanonicalName() + "." + method.getName();
        return name;
    }

    /**
     * Find out if metrics are enabled for the given class
     * <p>
     * Caller must hold lock on {@link #recorders}
     *
     * @param clazz
     * @return whether metrics are enabled
     */
    private boolean isMetricsEnabled(Class<?> clazz) {

        ClassLoader cl = secureAction.getClassLoader(clazz);
        Boolean metricsEnabled = metricsEnabledCache.get(cl);

        if (metricsEnabled == null) {
            Config mpConfig = ConfigProvider.getConfig(cl);
            metricsEnabled = mpConfig.getOptionalValue(CONFIG_METRICS_ENABLED, Boolean.class).orElse(Boolean.TRUE);
            metricsEnabledCache.put(cl, metricsEnabled);
        }

        return metricsEnabled;
    }

}
