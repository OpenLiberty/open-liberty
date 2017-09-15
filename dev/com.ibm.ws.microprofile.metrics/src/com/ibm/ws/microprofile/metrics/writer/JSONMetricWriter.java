/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.json.java.JSONObject;
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

    private JSONObject getJsonFromMetricMap(Map<String, Metric> metricMap) {
        JSONObject jsonObject = new JSONObject();
        for (Entry<String, Metric> entry : metricMap.entrySet()) {
            String metricName = entry.getKey();
            Metric metric = entry.getValue();
            if (Counter.class.isInstance(metric)) {
                jsonObject.put(metricName, ((Counter) metric).getCount());
            } else if (Gauge.class.isInstance(metric)) {
                jsonObject.put(metricName, ((Gauge) metric).getValue());
            } else if (Timer.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util.getTimerNumbers((Timer) metric)));
            } else if (Histogram.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util.getHistogramNumbers((Histogram) metric)));
            } else if (Meter.class.isInstance(metric)) {
                jsonObject.put(metricName, getJsonFromMap(Util.getMeterNumbers((Meter) metric)));
            } else {
                throw new RuntimeException("Unsupported Metric Type");
            }
        }
        return jsonObject;
    }

    private JSONObject getJsonFromMap(Map<String, Number> map) {
        JSONObject jsonObject = new JSONObject();
        for (Entry<String, Number> entry : map.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        return jsonObject;
    }

    private void serialize(JSONObject payload) throws IOException {
        payload.serialize(writer);
    }
}
