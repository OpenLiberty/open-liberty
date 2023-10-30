/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.metrics.internal.monitor.computed;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricID;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

public class ComputedMonitorGauge<T extends Number> implements Gauge<T> {

    private static final TraceComponent tc = Tr.register(ComputedMonitorMetricsHandler.class);

    ComputedMonitorMetricsHandler cmmh;
    MetricID metricId;

    public ComputedMonitorGauge(ComputedMonitorMetricsHandler cmmh, MetricID metricId) {
        this.cmmh = cmmh;
        this.metricId = metricId;
    }

    public T getValue() {
        try {
            T value = (T) cmmh.getComputedValue(metricId);
            return value;
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getValue exception message: ", e.getMessage());
                FFDCFilter.processException(e, getClass().getSimpleName(), "getValue:Exception");
            }
        }
        return null;
    }

}
