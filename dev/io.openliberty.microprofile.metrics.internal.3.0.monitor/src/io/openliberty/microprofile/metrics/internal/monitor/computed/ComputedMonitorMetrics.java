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

import org.eclipse.microprofile.metrics.MetricID;

public class ComputedMonitorMetrics {

    private MetricID monitorMetricID;
    private String computationType;
    private double prevValue;
    private String monitorMetricScope;
    private String metricUnit;
    private String appName;
    private String mpAppNameConfigValue;
    
    public ComputedMonitorMetrics(String metricScope, MetricID metricID, String computationType, String metricUnit) {
        this(metricScope, metricID, computationType, metricUnit, null, null);
     }

    public ComputedMonitorMetrics(String metricScope, MetricID metricID, String computationType, String metricUnit, String appName, String mpAppNameConfigValue) {
        this.monitorMetricScope = metricScope;
        this.monitorMetricID = metricID;
        this.computationType = computationType;
        this.metricUnit = metricUnit;
        this.appName = appName;
        this.mpAppNameConfigValue = mpAppNameConfigValue;
        this.prevValue = (double) 0.0;
    }

    public double getPreviousMetricValue() {
        return prevValue;
    }

    public void setPreviousMetricValue(double previousMetricValue) {
        this.prevValue = previousMetricValue;
    }

    public String getComputationType() {
        return this.computationType;
    }

    public MetricID getMonitorMetricID() {
        return this.monitorMetricID;
    }

    public String getMonitorMetricScope() {
        return this.monitorMetricScope;
    }
    
    public String getMetricUnit() {
        return this.metricUnit;
    }
    
    public String getAppName() {
        return this.appName;
    }
    
    public String getMpAppNameConfigValue() {
        return this.mpAppNameConfigValue;
    }

    public Double getDifference(double current) {
        double diff = 0.0;
        diff = current - this.prevValue;
        // set the previous value to the current value.
        setPreviousMetricValue(current);
        return diff;
    }

}
