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
import java.util.TreeMap;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
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

    private JSONObject getMetricsAsJson(String registryName) throws NoSuchRegistryException, EmptyRegistryException {
        return getJsonFromMetricMap(Util.getMetricsAsMap(registryName));
    }

    private JSONObject getMetricsAsJson(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, EmptyRegistryException {
        return getJsonFromMetricMap(Util.getMetricsAsMap(registryName, metricName));
    }

    private static final TraceComponent tc = Tr.register(JSONMetricWriter.class);

    @FFDCIgnore({ IllegalStateException.class })
    private JSONObject getJsonFromMetricMap(Map<MetricID, Metric> metricMap) {
        JSONObject jsonObject = new JSONObject();

        //For each Metric that was returned
        for (Entry<MetricID, Metric> entry : metricMap.entrySet()) {
            MetricID metricID = entry.getKey();
            TreeMap<String, String> alphabeticalMap = new TreeMap<String, String>(metricID.getTags());
            String tags = "";

            String metricName = metricID.getName();
            String metricNameWithTags = metricName;

            if (alphabeticalMap.size() != 0) {
                for (Entry<String, String> metricsMap : alphabeticalMap.entrySet()) {
                    String value = metricsMap.getValue();
                    if (value.contains(";")) {
                        value = value.replaceAll(";", "_");
                    }
                    tags += ";" + metricsMap.getKey() + "=" + value;
                }

                //tags = ";" + alphabeticalMap.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(";"));
                metricNameWithTags = metricName + tags;
            }
            Metric metric = entry.getValue();

            //Problem after this was that for each tagged MetricA , the "put" would overwrite the previous value.

            if (Counter.class.isInstance(metric)) {
                jsonObject.put(metricNameWithTags, ((Counter) metric).getCount());
            } else if (ConcurrentGauge.class.isInstance(metric)) {
//old code that lists the cg resources separately
//                jsonObject.put(metricNameWithTags, ((ConcurrentGauge) metric).getCount());
//                jsonObject.put(metricName + "_min" + tags, ((ConcurrentGauge) metric).getMin());
//                jsonObject.put(metricName + "_max" + tags, ((ConcurrentGauge) metric).getMax());
                jsonObject.put(metricName, getJsonFromMap(Util.getConcurrentGaugeNumbers((ConcurrentGauge) metric, tags), metricName, jsonObject));
            } else if (Gauge.class.isInstance(metric)) {
                try {
                    jsonObject.put(metricNameWithTags, ((Gauge) metric).getValue());
                } catch (IllegalStateException e) {
                    // The forwarding gauge is likely unloaded. A warning has already been emitted
                }
            } else if (Timer.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util.getTimerNumbers((Timer) metric, tags), metricName, jsonObject));
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

    private JSONObject getJsonFromMap(Map<String, Number> map, String metricName, JSONObject parentJSONObject) {

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
        for (Entry<String, Number> entry : map.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        return jsonObject;
    }

    private void serialize(JSONObject payload) throws IOException {
        payload.serialize(writer);
    }

}
