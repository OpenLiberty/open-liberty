/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics21.helper;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Sampling;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.helper.PrometheusBuilder;

/**
 *
 */
public class PrometheusBuilder21 extends PrometheusBuilder{

    private static final TraceComponent tc = Tr.register(PrometheusBuilder21.class);


    @FFDCIgnore({ IllegalStateException.class })
    public static void buildGauge21(StringBuilder builder, String name, Gauge<?> gauge, String description, Double conversionFactor, String tags, String appendUnit) {
        // Skip non number values
        Number gaugeValNumber = null;
        Object gaugeValue = null;
        try {
            gaugeValue = gauge.getValue();
        } catch (IllegalStateException e) {
            // The forwarding gauge is likely unloaded. A warning has already been emitted
            return;
        }
        if (!Number.class.isInstance(gaugeValue)) {
            Tr.event(tc, "Skipping Prometheus output for Gauge: " + name + " of type " + gauge.getValue().getClass());
            return;
        }
        gaugeValNumber = (Number) gaugeValue;
        if (!(Double.isNaN(conversionFactor))) {
            gaugeValNumber = gaugeValNumber.doubleValue() * conversionFactor;
        }
        getPromTypeLine(builder, name, "gauge", appendUnit);
        getPromHelpLine(builder, name, description, appendUnit);
        getPromValueLine(builder, name, gaugeValNumber, tags, appendUnit);
    }
}
