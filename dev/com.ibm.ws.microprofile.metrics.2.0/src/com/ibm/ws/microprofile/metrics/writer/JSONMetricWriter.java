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
package com.ibm.ws.microprofile.metrics.writer;

import java.io.IOException;
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
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics.helper.Util;

/**
 *
 */
public class JSONMetricWriter implements OutputWriter {

    private final Writer writer;

    public JSONMetricWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * {@inheritDoc}
     *
     * @throws EmptyRegistryException
     */
    @Override
    public void write(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, IOException, EmptyRegistryException {
        serialize(getMetricsAsJson(registryName, metricName));
    }

    /** {@inheritDoc} */
    @Override
    public void write(String registryName) throws NoSuchRegistryException, EmptyRegistryException, IOException {
        serialize(getMetricsAsJson(registryName));
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ EmptyRegistryException.class, NoSuchRegistryException.class })
    public void write() throws IOException {
        JSONObject payload = new JSONObject();
        for (String registryName : Constants.REGISTRY_NAMES_LIST) {
            try {
                payload.put(registryName, getMetricsAsJson(registryName));
            } catch (NoSuchRegistryException e) { // Ignore
            } catch (EmptyRegistryException e) { // Ignore
            }
        }
        serialize(payload);
    }

    protected JSONObject getMetricsAsJson(String registryName) throws NoSuchRegistryException, EmptyRegistryException {
        return getJsonFromMetricMap(Util.getMetricsAsMap(registryName), Util.getMetricsMetadataAsMap(registryName));
    }

    protected JSONObject getMetricsAsJson(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, EmptyRegistryException {
        return getJsonFromMetricMap(Util.getMetricsAsMap(registryName, metricName), Util.getMetricsMetadataAsMap(registryName, metricName));
    }

    private static final TraceComponent tc = Tr.register(JSONMetricWriter.class);

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

    /*
     * After retrieving a map of metrics from the registry
     */
    protected JSONObject getJsonFromMap(Map<String, Number> metricMap, String metricName, JSONObject parentJSONObject) {

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
        for (Entry<String, Number> entry : metricMap.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        return jsonObject;
    }

    private void serialize(JSONObject payload) throws IOException {
        payload.serialize(writer);
    }

}
