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
package com.ibm.ws.microprofile.metrics23.writer;

import java.io.Writer;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
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
        for (Entry<MetricID, Metric> entry : metricMap.entrySet()) {
            Metric metric = entry.getValue();
            MetricID metricID = entry.getKey();
            String metricName = metricID.getName();
            String metricNamePrometheus = registryName + "_" + metricName;

            //description
            Metadata metricMetaData = metricMetadataMap.get(metricName);

            String description = "";

            if (!metricMetaData.getDescription().isPresent() || metricMetaData.getDescription().get().trim().isEmpty()) {
                description = "";
            } else {
                description = Tr.formatMessage(tc, locale, metricMetaData.getDescription().get());
            }

            String tags = metricID.getTagsAsString();

            //appending unit to the metric name
            String unit = metricMetaData.getUnit().get();

            //Unit determination / translation
            double conversionFactor = 0;
            String appendUnit = null;

            if (unit == null || unit.trim().isEmpty() || unit.equals(MetricUnits.NONE)) {

                conversionFactor = Double.NaN;
                appendUnit = null;

            } else if (unit.equals(MetricUnits.NANOSECONDS)) {

                conversionFactor = Constants.NANOSECONDCONVERSION;
                appendUnit = Constants.APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.MICROSECONDS)) {

                conversionFactor = Constants.MICROSECONDCONVERSION;
                appendUnit = Constants.APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.SECONDS)) {

                conversionFactor = Constants.SECONDCONVERSION;
                appendUnit = Constants.APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.MINUTES)) {

                conversionFactor = Constants.MINUTECONVERSION;
                appendUnit = Constants.APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.HOURS)) {

                conversionFactor = Constants.HOURCONVERSION;
                appendUnit = Constants.APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.DAYS)) {

                conversionFactor = Constants.DAYCONVERSION;
                appendUnit = Constants.APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.PERCENT)) {

                conversionFactor = Double.NaN;
                appendUnit = Constants.APPENDEDPERCENT;

            } else if (unit.equals(MetricUnits.BYTES)) {

                conversionFactor = Constants.BYTECONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.KILOBYTES)) {

                conversionFactor = Constants.KILOBYTECONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.MEGABYTES)) {

                conversionFactor = Constants.MEGABYTECONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.GIGABYTES)) {

                conversionFactor = Constants.GIGABYTECONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.KILOBITS)) {

                conversionFactor = Constants.KILOBITCONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.MEGABITS)) {

                conversionFactor = Constants.MEGABITCONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.GIGABITS)) {

                conversionFactor = Constants.GIGABITCONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.KIBIBITS)) {

                conversionFactor = Constants.KIBIBITCONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.MEBIBITS)) {

                conversionFactor = Constants.MEBIBITCONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.GIBIBITS)) {

                conversionFactor = Constants.GIBIBITCONVERSION;
                appendUnit = Constants.APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.MILLISECONDS)) {

                conversionFactor = Constants.MILLISECONDCONVERSION;
                appendUnit = Constants.APPENDEDSECONDS;

            } else {

                conversionFactor = Double.NaN;
                appendUnit = "_" + unit;
            }

            if (Counter.class.isInstance(metric)) {
                PrometheusBuilder23.buildCounter(builder, metricNamePrometheus, (Counter) metric, description, tags);
            } else if (ConcurrentGauge.class.isInstance(metric)) {
                PrometheusBuilder23.buildConcurrentGauge(builder, metricNamePrometheus, (ConcurrentGauge) metric, description, tags);
            } else if (Gauge.class.isInstance(metric)) {
                PrometheusBuilder23.buildGauge(builder, metricNamePrometheus, (Gauge) metric, description, conversionFactor, tags, appendUnit);
            } else if (Timer.class.isInstance(metric)) {
                PrometheusBuilder23.buildTimer(builder, metricNamePrometheus, (Timer) metric, description, tags);
            } else if (Histogram.class.isInstance(metric)) {
                PrometheusBuilder23.buildHistogram(builder, metricNamePrometheus, (Histogram) metric, description, conversionFactor, tags, appendUnit);
            } else if (Meter.class.isInstance(metric)) {
                PrometheusBuilder23.buildMeter(builder, metricNamePrometheus, (Meter) metric, description, tags);
            } else if (SimpleTimer.class.isInstance(metric)) {
                PrometheusBuilder23.buildSimpleTimer(builder, metricNamePrometheus, (SimpleTimer) metric, description, tags);
            } else {
                Tr.event(tc, "Metric type '" + metric.getClass() + " for " + metricName + " is invalid.");
            }
        }
    }
}
