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

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.time.Duration;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

@Component(configurationPolicy = IGNORE, immediate = true)
public class MetricsManager {

    private static MetricsManager instance;

    private static final TraceComponent tc = Tr.register(MetricsManager.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ConnectionPoolMetricAdapter> metricRuntimes;

    @Activate
    public void activate() {
        instance = this;
    }

    @Deactivate
    public void deactivate() {

        instance = null;
    }

    public static MetricsManager getInstance() {
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
        metricRuntimes.stream().forEach(adapters -> adapters.updateWaitTimeMetrics(poolName, duration, getApplicationName()));
    }

    /**
     *
     * @param poolName JNDI name of the pool (i.e., data source)
     * @param Duration recorded Duration of the (in) use time.
     */
    public void updateInUseTimeMetrics(String poolName, Duration duration) {
        metricRuntimes.stream().forEach(adapters -> adapters.updateInUseTimeMetrics(poolName, duration, getApplicationName()));
    }

    private String getApplicationName() {
        ComponentMetaData metaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (metaData != null) {
            J2EEName name = metaData.getJ2EEName();
            if (name != null) {
                return name.getApplication();
            }
        }
        return null;
    }

}
