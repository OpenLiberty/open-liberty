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

    public static void buildSimpleTimer(StringBuilder builder, String name, SimpleTimer simpleTimer, String description, String tags) {
        double conversionFactor = Constants.NANOSECONDCONVERSION;

        buildCounting(builder, name, simpleTimer, description, tags);

        String lineName = name + "_elapsedTime_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, "gauge");

        getPromValueLine(builder, lineName, simpleTimer.getElapsedTime().toNanos() * conversionFactor, tags);

    }

}
