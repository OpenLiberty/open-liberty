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
package com.ibm.ws.microprofile.metrics23.helper;

import java.util.Map;

import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.SimpleTimer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.helper.PrometheusBuilder;

/**
 *
 */
public class PrometheusBuilder23 extends PrometheusBuilder {

    private static final TraceComponent tc = Tr.register(PrometheusBuilder23.class);

    public static void buildSimpleTimer(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        double conversionFactor = Constants.NANOSECONDCONVERSION;

        buildCounting(builder, name, description, currentMetricMap);

        String lineName = name + "_elapsedTime_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((SimpleTimer) currentMetricMap.get(mid)).getElapsedTime().toNanos() * conversionFactor, mid.getTagsAsString());
        }

    }

}
