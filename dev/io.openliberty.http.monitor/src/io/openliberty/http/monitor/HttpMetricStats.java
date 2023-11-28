/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;

import com.ibm.websphere.monitor.jmx.Meter;
import com.ibm.websphere.monitor.meters.StatisticsMeter;

/**
 *
 */
public class HttpMetricStats extends Meter implements HttpStatsMXBean {

    private final StatisticsMeter responseTime;

    public HttpMetricStats() {

        responseTime = new StatisticsMeter();
        responseTime.setDescription("Cumulative Response Time (NanoSeconds) for a HTTP connection");
        responseTime.setUnit("ns");
    }

    public void setDuration(long duration) {
        responseTime.addDataPoint(duration);
    }

    @Override
    public double getDuration() {
        return responseTime.getTotal();
    }

}
