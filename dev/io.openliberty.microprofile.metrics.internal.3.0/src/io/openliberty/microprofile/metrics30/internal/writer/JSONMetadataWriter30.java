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
package io.openliberty.microprofile.metrics30.internal.writer;

import java.io.Writer;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics.writer.JSONMetadataWriter;

import io.openliberty.microprofile.metrics30.internal.helper.Util30;

/**
 *
 */
public class JSONMetadataWriter30 extends JSONMetadataWriter implements OutputWriter {

    private static final TraceComponent tc = Tr.register(JSONMetadataWriter30.class);

    public JSONMetadataWriter30(Writer writer, Locale locale) {
        super(writer, locale);
    }

    @Override
    protected JSONObject getMetricsMetadataAsJson(String registryName) throws NoSuchRegistryException, EmptyRegistryException {

        return getJsonFromMetricMetadataMap(Util30.getMetricsMetadataAsMap(registryName), Util30.getMetricsAsMap(registryName));
    }

    @Override
    protected JSONObject getMetricsMetadataAsJson(String registryName, String metricName) throws NoSuchRegistryException, EmptyRegistryException, NoSuchMetricException {
        return getJsonFromMetricMetadataMap(Util30.getMetricsMetadataAsMap(registryName, metricName), Util30.getMetricsAsMap(registryName, metricName));
    }

    @Override
    protected JSONObject getJsonFromObject(Metadata metadata) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", sanitizeMetadata(metadata.getName()));
        jsonObject.put("displayName", sanitizeMetadata(metadata.getDisplayName()));
        //Check TR.formatMessage for performance impact

        //DefaultMetaData returns Optional<String>, if a null value is returned then provide an empty string otherwise a NoSuchElementException occurs
        String description = (metadata.description().isPresent()) ? metadata.description().get() : "";
        jsonObject.put("description", Tr.formatMessage(tc, locale, description));
        jsonObject.put("type", sanitizeMetadata(metadata.getType()));
        jsonObject.put("unit", sanitizeMetadata(metadata.getUnit()));

        return jsonObject;
    }

    @Override
    protected JSONArray getJsonArrayTags(Map<MetricID, Metric> metricMap, String metricName) {
        JSONArray jsonArray = new JSONArray();
        //for each metric in metric map
        for (MetricID metricID : metricMap.keySet()) {
            //that has matching names
            if (metricID.getName().equals(metricName)) {
                JSONArray metricTagJsonArray = new JSONArray();
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

                // format the tags
                if (tagsMap != null) {
                    for (Entry<String, String> tagEntry : tagsMap.entrySet()) {
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
}
