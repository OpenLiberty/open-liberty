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
package com.ibm.ws.microprofile.metrics23.writer;

import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.helper.PrometheusBuilder;
import com.ibm.ws.microprofile.metrics.writer.PrometheusMetricWriter;
import com.ibm.ws.microprofile.metrics23.helper.PrometheusBuilder23;

/**
 *
 */
public class PrometheusMetricWriter23 extends PrometheusMetricWriter implements OutputWriter {

    private static final TraceComponent tc = Tr.register(PrometheusMetricWriter23.class);

    public PrometheusMetricWriter23(Writer writer, Locale locale) {
        super(writer, locale);
    }

    @Override
    protected void writeMetricMapAsPrometheus(StringBuilder builder, String registryName, Map<MetricID, Metric> metricMap, Map<String, Metadata> metricMetadataMap) {
        for (Entry<String, Metadata> metadataEntry : metricMetadataMap.entrySet()) {

            String metricName = metadataEntry.getKey();
            String metricNamePrometheus = registryName + "_" + metricName;
            Metadata metricMetadata = metadataEntry.getValue();

            Map<MetricID, Metric> currentMetricMap = new HashMap<MetricID, Metric>();

            for (Entry<MetricID, Metric> metricEntry : metricMap.entrySet()) {
                if (metricEntry.getKey().getName().equals(metricName)) {
                    currentMetricMap.put(metricEntry.getKey(), metricEntry.getValue());
                }
            }
            //If current metadata that we are parsing does not have a matching metric... skip
            if (currentMetricMap.isEmpty()) {
                continue;
            }

            //Get Description
            String description = (!metricMetadata.getDescription().isPresent()
                                  || metricMetadata.getDescription().get().trim().isEmpty()) ? "" : Tr.formatMessage(tc, locale, metricMetadata.getDescription().get());

            //Get Unit
            String unit = metricMetadata.getUnit().get();

            //Unit determination / translation
            Map.Entry<String, Double> conversionAppendEntry = resolveConversionFactorXappendUnitEntry(unit);
            double conversionFactor = conversionAppendEntry.getValue();
            String appendUnit = conversionAppendEntry.getKey();

            if (metricMetadata.getTypeRaw().equals(MetricType.COUNTER)) {
                PrometheusBuilder.buildCounter(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.CONCURRENT_GAUGE)) {
                PrometheusBuilder.buildConcurrentGauge(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.GAUGE)) {
                PrometheusBuilder.buildGauge(builder, metricNamePrometheus, description, currentMetricMap, conversionFactor, appendUnit);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.TIMER)) {
                PrometheusBuilder.buildTimer(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.HISTOGRAM)) {
                PrometheusBuilder.buildHistogram(builder, metricNamePrometheus, description, currentMetricMap, conversionFactor, appendUnit);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.METERED)) {
                PrometheusBuilder.buildMeter(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.SIMPLE_TIMER)) {
                PrometheusBuilder23.buildSimpleTimer(builder, metricNamePrometheus, description, currentMetricMap);
            } else {
                Tr.event(tc, "Metadata " + metricMetadata.toString() + " does not have an appropriate Metric Type");
            }

        }
    }

}
