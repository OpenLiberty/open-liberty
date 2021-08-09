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

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.helper.Util;
import com.ibm.ws.microprofile.metrics.writer.JSONMetricWriter;
import com.ibm.ws.microprofile.metrics23.helper.Util23;

/**
 *
 */
public class JSONMetricWriter23 extends JSONMetricWriter {

    /**
     * @param writer
     */
    public JSONMetricWriter23(Writer writer) {
        super(writer);
    }

    private static final TraceComponent tc = Tr.register(JSONMetricWriter23.class);

    @Override
    @FFDCIgnore({ IllegalStateException.class })
    protected JSONObject getJsonFromMetricMap(Map<MetricID, Metric> metricMap, Map<String, Metadata> metricMetadataMap) {
        JSONObject jsonObject = new JSONObject();

        //For each Metric that was returned
        for (Entry<MetricID, Metric> entry : metricMap.entrySet()) {
            MetricID metricID = entry.getKey();
            Map<String, String> tagsMap = metricID.getTags();
            String metricName = metricID.getName();
            String metricNameWithTags = metricName;
            String tags = "";

            Metadata metricMetaData = metricMetadataMap.get(metricName);
            String unit = metricMetaData.getUnit().get();

            // If an invalid unit is entered, default to nanoseconds
            double conversionFactor = 1;

            switch (unit) {
                case MetricUnits.NANOSECONDS:
                    conversionFactor = 1;
                    break;
                case MetricUnits.MICROSECONDS:
                    conversionFactor = 1000;
                    break;
                case MetricUnits.MILLISECONDS:
                    conversionFactor = 1000000;
                    break;
                case MetricUnits.SECONDS:
                    conversionFactor = 1000000000L;
                    break;
                case MetricUnits.MINUTES:
                    conversionFactor = 60 * 1000000000L;
                    break;
                case MetricUnits.HOURS:
                    conversionFactor = 60 * 60 * 1000000000L;
                    break;
                case MetricUnits.DAYS:
                    conversionFactor = 24 * 60 * 60 * 1000000000L;
                    break;
            }

            if (tagsMap.size() != 0) {
                for (Entry<String, String> tagsMapEntrySet : tagsMap.entrySet()) {
                    String tagValue = tagsMapEntrySet.getValue();

                    /*
                     * As per MicroProfile metrics spec
                     * Need to convert semicolons found in the value of tags
                     * to underscores
                     */
                    if (tagValue.contains(";")) {
                        tagValue = tagValue.replaceAll(";", "_");
                    }

                    tags += ";" + tagsMapEntrySet.getKey() + "=" + tagValue;
                }
                metricNameWithTags = metricName + tags;
            }
            Metric metric = entry.getValue();

            if (Counter.class.isInstance(metric)) {
                jsonObject.put(metricNameWithTags, ((Counter) metric).getCount());
            } else if (ConcurrentGauge.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util.getConcurrentGaugeNumbers((ConcurrentGauge) metric, tags), metricName, jsonObject));
            } else if (Gauge.class.isInstance(metric)) {
                try {
                    jsonObject.put(metricNameWithTags, ((Gauge) metric).getValue());
                } catch (IllegalStateException e) {
                    // The forwarding gauge is likely unloaded. A warning has already been emitted
                }
            } else if (Timer.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util.getTimerNumbers((Timer) metric, tags, conversionFactor), metricName, jsonObject));
            } else if (SimpleTimer.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util23.getSimpleTimerNumbers((SimpleTimer) metric, tags, conversionFactor), metricName, jsonObject));
            } else if (Histogram.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util.getHistogramNumbers((Histogram) metric, tags), metricName, jsonObject));
            } else if (Meter.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util.getMeterNumbers((Meter) metric, tags), metricName, jsonObject));
            } else {
                Tr.event(tc, "Metric type '" + metric.getClass() + " for " + metricName + " is invalid.");
            }
        }
        return jsonObject;
    }

}
