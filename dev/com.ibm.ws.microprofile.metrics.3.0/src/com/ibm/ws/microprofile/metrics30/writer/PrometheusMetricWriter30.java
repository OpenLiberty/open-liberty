/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics30.writer;

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
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics23.writer.PrometheusMetricWriter23;
import com.ibm.ws.microprofile.metrics30.helper.PrometheusBuilder30;
import com.ibm.ws.microprofile.metrics30.helper.Util30;

/**
 *
 */
public class PrometheusMetricWriter30 extends PrometheusMetricWriter23 implements OutputWriter {

    private static final TraceComponent tc = Tr.register(PrometheusMetricWriter30.class);

    public PrometheusMetricWriter30(Writer writer, Locale locale) {
        super(writer, locale);
    }

    @Override
    protected void writeMetricsAsPrometheus(StringBuilder builder, String registryName) throws NoSuchRegistryException, EmptyRegistryException {
        writeMetricMapAsPrometheus(builder, registryName, Util30.getMetricsAsMap(registryName), Util30.getMetricsMetadataAsMap(registryName));
    }

    @Override
    protected void writeMetricsAsPrometheus(StringBuilder builder, String registryName,
                                            String metricName) throws NoSuchRegistryException, NoSuchMetricException, EmptyRegistryException {
        writeMetricMapAsPrometheus(builder, registryName, Util30.getMetricsAsMap(registryName, metricName), Util30.getMetricsMetadataAsMap(registryName));
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
            String description = (!metricMetadata.description().isPresent()
                                  || metricMetadata.description().get().trim().isEmpty()) ? "" : Tr.formatMessage(tc, locale, metricMetadata.description().get());

            //Get Unit
            String unit = metricMetadata.getUnit();

            //Unit determination / translation
            Map.Entry<String, Double> conversionAppendEntry = resolveConversionFactorXappendUnitEntry(unit);
            double conversionFactor = conversionAppendEntry.getValue();
            String appendUnit = conversionAppendEntry.getKey();

            if (metricMetadata.getTypeRaw().equals(MetricType.COUNTER)) {
                PrometheusBuilder30.buildCounter30(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.CONCURRENT_GAUGE)) {
                PrometheusBuilder30.buildConcurrentGauge30(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.GAUGE)) {
                PrometheusBuilder30.buildGauge30(builder, metricNamePrometheus, description, currentMetricMap, conversionFactor, appendUnit);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.TIMER)) {
                PrometheusBuilder30.buildTimer30(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.HISTOGRAM)) {
                PrometheusBuilder30.buildHistogram30(builder, metricNamePrometheus, description, currentMetricMap, conversionFactor, appendUnit);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.METERED)) {
                PrometheusBuilder30.buildMeter30(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.SIMPLE_TIMER)) {
                PrometheusBuilder30.buildSimpleTimer30(builder, metricNamePrometheus, description, currentMetricMap);
            } else {
                Tr.event(tc, "Metadata " + metricMetadata.toString() + " does not have an appropriate Metric Type");
            }

        }
    }

}
