/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.metrics.integration;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

@Component(name = "com.ibm.ws.microprofile.faulttolerance.metrics.integration.MetricRecorderProviderImpl", service = MetricRecorderProvider.class, configurationPolicy = IGNORE)
public class MetricRecorderProvider30Impl implements MetricRecorderProvider {

    private final static SecureAction secureAction = AccessController.doPrivileged(SecureAction.get());

    /**
     * This lock must be held before reading or writing from the {@link #metricsEnabledCache}
     */
    private final ReentrantReadWriteLock metricsEnabledCacheLock = new ReentrantReadWriteLock();

    /**
     * Map from Classloader to whether metrics are disabled for that classloader
     * <p>
     * Must hold {@link #metricsEnabledCacheLock} before reading or writing
     */
    private final Map<ClassLoader, Boolean> metricsEnabledCache = new WeakHashMap<>();

    private final static String CONFIG_METRICS_ENABLED = "MP_Fault_Tolerance_Metrics_Enabled";

    @Reference
    protected SharedMetricRegistries sharedRegistries;

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
            return new MetricRecorder30Impl(getMetricName(method), registry, retryPolicy, circuitBreakerPolicy, timeoutPolicy, bulkheadPolicy, fallbackPolicy, isAsync);
        } else {
            return DummyMetricRecorder.get();
        }
    }

    private String getMetricName(Method method) {
        String name = method.getDeclaringClass().getCanonicalName() + "." + method.getName();
        return name;
    }

    private boolean isMetricsEnabled(Class<?> clazz) {

        ClassLoader cl = secureAction.getClassLoader(clazz);
        Boolean metricsEnabled = null;
        // Get the read lock before checking the cache
        metricsEnabledCacheLock.readLock().lock();
        try {
            metricsEnabled = metricsEnabledCache.get(cl);
            if (metricsEnabled == null) {
                // Classloader is not in the cache, let's add it
                // must release the read lock before acquiring the write lock (upgrading while holding the lock is not allowed)
                metricsEnabledCacheLock.readLock().unlock();
                metricsEnabledCacheLock.writeLock().lock();
                try {
                    // Now we have the write lock, recheck whether the classloader is in the cache
                    metricsEnabled = metricsEnabledCache.get(cl);
                    if (metricsEnabled == null) {
                        Config mpConfig = ConfigProvider.getConfig(cl);
                        metricsEnabled = mpConfig.getOptionalValue(CONFIG_METRICS_ENABLED, Boolean.class).orElse(Boolean.TRUE);
                    }
                    // Downgrade to the read lock (this is allowed)
                    metricsEnabledCacheLock.readLock().lock();
                } finally {
                    metricsEnabledCacheLock.writeLock().unlock();
                }
            }
        } finally {
            metricsEnabledCacheLock.readLock().unlock();
        }

        return metricsEnabled;
    }

}
