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

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.pmi.factory.StatisticActions;

/**
 *
 */
@Monitor(group = "HttpStats")
public class HttpMetricsMonitor extends StatisticActions {

    private static final TraceComponent tc = Tr.register(HttpMetricsMonitor.class);

    /*
     * Set singleton
     */
    {
        if (instance == null) {
            System.out.println("Registering singleton actual");
            instance = this;
        } else {
            Tr.debug(tc, "singleton already registered " + instance);
        }

    }

    public static HttpMetricsMonitor getInstance() {
        if (instance != null) {
            return instance;
        } else {
            System.out.println(" uh ohh");
        }

        return null;
    }

    public static HttpMetricsMonitor instance;

    @PublishedMetric
    public MeterCollection<HttpMetricStats> HttpConnByRoute = new MeterCollection<HttpMetricStats>("HttpMetrics", this);

    // sync static methods for adding/retrieving

    public void updateHttpStatDuration(String httpRoute) {

        if (HttpConnByRoute.get(httpRoute) == null) {
            iniitializeHttpStat(httpRoute);
        }

    }

    public void mockUpdate(String httpRouteKey) {
        if (HttpConnByRoute.get(httpRouteKey) == null) {
            System.out.println("mock initialize");
            iniitializeHttpStat(httpRouteKey);
        }
    }

    public synchronized void iniitializeHttpStat(String httpRoute) {
        // double check
        if (HttpConnByRoute.get("") != null)
            return;

        HttpMetricStats httpMetricStats = new HttpMetricStats();
        HttpConnByRoute.put(httpRoute, httpMetricStats);

        httpMetricStats.setDuration(123456789);

    }
}
