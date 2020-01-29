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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;

import com.ibm.json.java.JSONArray;
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
public class JSONMetadataWriter implements OutputWriter {

    private static final TraceComponent tc = Tr.register(JSONMetadataWriter.class);

    protected final Writer writer;
    protected final Locale locale;

    public JSONMetadataWriter(Writer writer, Locale locale) {
        this.writer = writer;
        this.locale = locale;
    }

    /** {@inheritDoc} */
    @Override
    public void write(String registryName, String metric) throws NoSuchRegistryException, NoSuchMetricException, IOException {
        try {
            serialize(getMetricsMetadataAsJson(registryName, metric));
        } catch (EmptyRegistryException e) {

        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(String registryName) throws NoSuchRegistryException, EmptyRegistryException, IOException {
        serialize(getMetricsMetadataAsJson(registryName));
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ EmptyRegistryException.class, NoSuchRegistryException.class })
    public void write() throws IOException {
        JSONObject payload = new JSONObject();
        for (String registryName : Constants.REGISTRY_NAMES_LIST) {
            try {
                payload.put(registryName, getMetricsMetadataAsJson(registryName));
            } catch (NoSuchRegistryException e) { // Ignore
            } catch (EmptyRegistryException e) { // Ignore
            }
        }
        serialize(payload);
    }

    protected JSONObject getMetricsMetadataAsJson(String registryName) throws NoSuchRegistryException, EmptyRegistryException {

        return getJsonFromMetricMetadataMap(Util.getMetricsMetadataAsMap(registryName), Util.getMetricsAsMap(registryName));
    }

    protected JSONObject getMetricsMetadataAsJson(String registryName, String metricName) throws NoSuchRegistryException, EmptyRegistryException, NoSuchMetricException {
        return getJsonFromMetricMetadataMap(Util.getMetricsMetadataAsMap(registryName, metricName), Util.getMetricsAsMap(registryName, metricName));
    }

    protected JSONObject getJsonFromMetricMetadataMap(Map<String, Metadata> metadataMap, Map<MetricID, Metric> metricMap) {
        JSONObject jsonObject = new JSONObject();

        //for each metric name in metadata map
        for (Entry<String, Metadata> entry : metadataMap.entrySet()) {
            String metricName = entry.getKey();
            JSONObject metaDataJSONObject = getJsonFromObject(metadataMap.get(metricName));

            metaDataJSONObject.put("tags", getJsonArrayTags(metricMap, metricName));
            jsonObject.put(metricName, metaDataJSONObject);
        }

        return jsonObject;
    }

    protected JSONArray getJsonArrayTags(Map<MetricID, Metric> metricMap, String metricName) {
        JSONArray jsonArray = new JSONArray();
        //for each metric in metric map
        for (MetricID metricIDSet : metricMap.keySet()) {
            //that has matching names
            if (metricIDSet.getName().equals(metricName)) {
                JSONArray metricTagJsonArray = new JSONArray();
                Map<String, String> tagMap = metricIDSet.getTags();
                //of which the tagMap is not empty
                if (tagMap != null) {
                    for (Entry<String, String> tagEntry : tagMap.entrySet()) {
                        metricTagJsonArray.add(tagEntry.getKey() + "=" + tagEntry.getValue());
                    }
                    if (metricTagJsonArray.size() != 0) {
                        jsonArray.add(metricTagJsonArray);
                    }
                }
            }
        }
        return jsonArray;
    }

    protected JSONObject getJsonFromObject(Metadata metadata) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", sanitizeMetadata(metadata.getName()));
        jsonObject.put("displayName", sanitizeMetadata(metadata.getDisplayName()));
        //Check TR.formatMessage for performance impact

        //DefaultMetaData returns Optional<String>, if a null value is returned then provide an empty string otherwise a NoSuchElementException occurs
        String description = (metadata.getDescription().isPresent()) ? metadata.getDescription().get() : "";
        jsonObject.put("description", Tr.formatMessage(tc, locale, description));
        jsonObject.put("type", sanitizeMetadata(metadata.getType()));
        jsonObject.put("unit", sanitizeMetadata(metadata.getUnit().get()));

        return jsonObject;
    }

    protected String sanitizeMetadata(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "";
        } else {
            return s;
        }
    }

    protected String getJsonFromMap(Map<String, String> map) {
        if (map == null)
            return null;
        StringBuilder tagList = new StringBuilder();
        String delimiter = "";
        for (Entry<String, String> entry : map.entrySet()) {
            tagList.append(delimiter).append(entry.getKey()).append('=').append(entry.getValue());
            delimiter = ",";
        }
        return tagList.toString();
    }

    protected void serialize(JSONObject payload) throws IOException {
        payload.serialize(writer);
    }
}
