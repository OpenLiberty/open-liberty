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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics23.writer.JSONMetricWriter23;
import com.ibm.ws.microprofile.metrics30.helper.Util30;

/**
 *
 */
public class JSONMetricWriter30 extends JSONMetricWriter23 {

    /**
     * @param writer
     */
    public JSONMetricWriter30(Writer writer) {
        super(writer);
    }

    @Override
    protected JSONObject getMetricsAsJson(String registryName) throws NoSuchRegistryException, EmptyRegistryException {
        return getJsonFromMetricMap(Util30.getMetricsAsMap(registryName), Util30.getMetricsMetadataAsMap(registryName));
    }

    @Override
    protected JSONObject getMetricsAsJson(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, EmptyRegistryException {
        return getJsonFromMetricMap(Util30.getMetricsAsMap(registryName, metricName), Util30.getMetricsMetadataAsMap(registryName, metricName));
    }

    private static final TraceComponent tc = Tr.register(JSONMetricWriter30.class);

    @Override
    @FFDCIgnore({ IllegalStateException.class })
    protected JSONObject getJsonFromMetricMap(Map<MetricID, Metric> metricMap, Map<String, Metadata> metricMetadataMap) {
        JSONObject jsonObject = new JSONObject();

        //For each Metric that was returned
        for (Entry<MetricID, Metric> entry : metricMap.entrySet()) {
            MetricID metricID = entry.getKey();

            Map<String, String> tagsMap = metricID.getTags();

            Tag[] globalTags = Util30.getCachedGlobalTags();

            /*
             * Inject Global Tags with the Metric's tags. Use a Tree map to order alphabetically as is expected for JSON output.
             * Global Tags are added first so that application defined tags can override them.
             * This is the behaviour seen in MP Metrics 2.3 and below.
             */
            if (globalTags != null) {
                Tag[] metricTags = metricID.getTagsAsArray();

                TreeMap<String, String> tagsMapWithGlobalTags = new TreeMap<String, String>();
                for (Tag t : globalTags) {
                    tagsMapWithGlobalTags.put(t.getTagName(), t.getTagValue());
                }
                for (Tag t : metricTags) {
                    tagsMapWithGlobalTags.put(t.getTagName(), t.getTagValue());
                }
                tagsMap = tagsMapWithGlobalTags;
            }

            String metricName = metricID.getName();
            String metricNameWithTags = metricName;
            String tags = "";

            Metadata metricMetaData = metricMetadataMap.get(metricName);
            String unit = metricMetaData.getUnit();

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
                jsonObject.put(metricName, getJsonFromMap(Util30.getConcurrentGaugeNumbers((ConcurrentGauge) metric, tags), metricName, jsonObject));
            } else if (Gauge.class.isInstance(metric)) {
                try {
                    jsonObject.put(metricNameWithTags, ((Gauge) metric).getValue());
                } catch (IllegalStateException e) {
                    // The forwarding gauge is likely unloaded. A warning has already been emitted
                }
            } else if (Timer.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util30.getTimerNumbers((Timer) metric, tags, conversionFactor), metricName, jsonObject));
            } else if (SimpleTimer.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMapAsObject(Util30.getSimpleTimerNumbersAsObjects((SimpleTimer) metric, tags, conversionFactor), metricName, jsonObject));
            } else if (Histogram.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util30.getHistogramNumbers((Histogram) metric, tags), metricName, jsonObject));
            } else if (Meter.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util30.getMeterNumbers((Meter) metric, tags), metricName, jsonObject));
            } else {
                Tr.event(tc, "Metric type '" + metric.getClass() + " for " + metricName + " is invalid.");
            }
        }
        return jsonObject;
    }

    /*
     * This method compared to getJsonFromMap returns a map with a value as an Object.. This is because
     * the value can be a null object.
     */
    protected JSONObject getJsonFromMapAsObject(Map<String, Object> metricMap, String metricName, JSONObject parentJSONObject) {

        /*
         * Check if parent JsonObject has this "metric" already set in it.
         * If so, need to grow the JsonObject and then reinsert
         */
        JSONObject jsonObject;
        if (parentJSONObject.containsKey(metricName)) {
            jsonObject = (JSONObject) parentJSONObject.get(metricName);
        } else {
            jsonObject = new JSONObject();
        }

        //map already contains "keys" with the "tags"
        for (Entry<String, Object> entry : metricMap.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        return jsonObject;
    }

}
