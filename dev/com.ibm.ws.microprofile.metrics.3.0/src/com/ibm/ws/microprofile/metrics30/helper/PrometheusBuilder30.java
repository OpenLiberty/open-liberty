/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics30.helper;

import java.util.Map;

import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics23.helper.PrometheusBuilder23;

/**
 *
 */
public class PrometheusBuilder30 extends PrometheusBuilder23 {

    private static final TraceComponent tc = Tr.register(PrometheusBuilder30.class);

    public static void buildTimer(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        buildMetered(builder, name, description, currentMetricMap);
        double conversionFactor = Constants.NANOSECONDCONVERSION;

        String lineName = name + "_elapsedTime_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((Timer) currentMetricMap.get(mid)).getElapsedTime().toNanos() * conversionFactor, mid.getTagsAsString());
        }

        // Build Histogram
        buildSampling(builder, name, description, currentMetricMap, conversionFactor, Constants.APPENDEDSECONDS);
    }

    public static void buildSimpleTimer(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        double conversionFactor = Constants.NANOSECONDCONVERSION;

        buildCounting(builder, name, description, currentMetricMap);

        String lineName = name + "_elapsedTime_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((SimpleTimer) currentMetricMap.get(mid)).getElapsedTime().toNanos() * conversionFactor, mid.getTagsAsString());
        }

        lineName = name + "_maxTimeDuration_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : currentMetricMap.keySet()) {
            Number value = (((SimpleTimer) currentMetricMap.get(mid)).getMaxTimeDuration() != null) ? ((SimpleTimer) currentMetricMap.get(mid)).getMaxTimeDuration().toNanos()
                                                                                                      * conversionFactor : Double.NaN;
            getPromValueLine(builder, lineName, value, mid.getTagsAsString());
        }

        lineName = name + "_minTimeDuration_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : currentMetricMap.keySet()) {
            Number value = (((SimpleTimer) currentMetricMap.get(mid)).getMinTimeDuration() != null) ? ((SimpleTimer) currentMetricMap.get(mid)).getMinTimeDuration().toNanos()
                                                                                                      * conversionFactor : Double.NaN;
            getPromValueLine(builder, lineName, value, mid.getTagsAsString());
        }

    }

}
