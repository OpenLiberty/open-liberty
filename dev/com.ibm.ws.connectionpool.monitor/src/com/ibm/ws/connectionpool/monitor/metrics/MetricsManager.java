/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.connectionpool.monitor.metrics;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

@Component(configurationPid = "com.ibm.ws.monitor.internal.MonitoringFrameworkExtender", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true)
public class MetricsManager {

    private static MetricsManager instance;

    private static final TraceComponent tc = Tr.register(MetricsManager.class);

    private final static String MONITORING_GROUP_FILTER = "filter";

    /*
     * By Default, without any monitor-1.0 filters on, all monitor components are enabled
     */
    private static volatile boolean isConnPoolEnabled = true;

    public static boolean isConnPoolEnabled() {
        return isConnPoolEnabled;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ConnectionPoolMetricAdapter> metricRuntimes;

    @Activate
    public void activate(ComponentContext context, Map<String, Object> properties) {
        resolveMonitorFilter(properties);
        instance = this;
    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        resolveMonitorFilter(properties);
    }

    @Deactivate
    public void deactivate() {

        instance = null;
    }

    public static MetricsManager getInstance() {

        //beta - return no instance
        if (!ProductInfo.getBetaEdition()) {
            return null;
        }

        if (instance != null) {
            return instance;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "No MetricsManager Instance available ");
        }
        return null;
    }

    /**
     *
     * @param poolName JNDI name of the pool (i.e., data source)
     * @param duration recorded Duration of the wait time
     */
    public void updateWaitTimeMetrics(String poolName, Duration duration) {
        //just in case
        if (!ProductInfo.getBetaEdition()) {
            return;
        }
        if (isConnPoolEnabled) {
            metricRuntimes.stream().forEach(adapters -> adapters.updateWaitTimeMetrics(poolName, duration));
        }

    }

    /**
     *
     * @param poolName JNDI name of the pool (i.e., data source)
     * @param Duration recorded Duration of the (in) use time.
     */
    public void updateInUseTimeMetrics(String poolName, Duration duration) {
        //just in case
        if (!ProductInfo.getBetaEdition()) {
            return;
        }

        if (isConnPoolEnabled) {
            metricRuntimes.stream().forEach(adapters -> adapters.updateInUseTimeMetrics(poolName, duration));
        }

    }

    private void resolveMonitorFilter(Map<String, Object> properties) {
        String filter;

        if ((filter = (String) properties.get(MONITORING_GROUP_FILTER)) != null && filter.length() != 0) {
            // Original MonitoringFrameWorkExtender matches case

            if (filter.length() > 0) {
                isConnPoolEnabled = Stream.of(filter.split(",")).anyMatch(item -> item.equals("ConnectionPool"));
            } else {
                // by default, every monitor component is enabled if length is 0
                isConnPoolEnabled = true;
            }
        } else if (filter == null) {
            /*
             * This bundle starts automatically with monitor-1.0 and connection pool dependent features
             * If `filter` is null, we'll assume that there was no config and we will enable by default.
             */
            isConnPoolEnabled = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, String.format("ConnectionPool filter is enabled set to: [%s]", isConnPoolEnabled));
        }
    }

}
