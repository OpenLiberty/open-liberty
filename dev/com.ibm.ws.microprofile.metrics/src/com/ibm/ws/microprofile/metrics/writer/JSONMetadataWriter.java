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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.microprofile.metrics.Metadata;

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

    private final Writer writer;
    private final Locale locale;

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

    private JSONObject getMetricsMetadataAsJson(String registryName) throws NoSuchRegistryException, EmptyRegistryException {
        return getJsonFromMetricMetadataMap(Util.getMetricsMetadataAsMap(registryName));
    }

    private JSONObject getMetricsMetadataAsJson(String registryName, String metric) throws NoSuchRegistryException, EmptyRegistryException, NoSuchMetricException {
        return getJsonFromMetricMetadataMap(Util.getMetricsMetadataAsMap(registryName, metric));
    }

    private JSONObject getJsonFromMetricMetadataMap(Map<String, Metadata> metadataMap) {
        JSONObject jsonObject = new JSONObject();
        for (Entry<String, Metadata> entry : metadataMap.entrySet()) {
            jsonObject.put(entry.getKey(), getJsonFromObject(entry.getValue()));
        }
        return jsonObject;
    }

    private JSONObject getJsonFromObject(Metadata metadata) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", metadata.getName());
            jsonObject.put("displayName", metadata.getDisplayName());
            jsonObject.put("description", Tr.formatMessage(tc, locale, metadata.getDescription()));
            jsonObject.put("type", metadata.getType());
            jsonObject.put("unit", metadata.getUnit());
            jsonObject.put("tags", getJsonFromMap(metadata.getTags()));
        } catch (Exception e) {
        }
        return jsonObject;
    }

    private JSONObject getJsonFromMap(Map<String, String> map) {
        JSONObject jsonObject = new JSONObject();
        for (Entry<String, String> entry : map.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        return jsonObject;
    }

    private void serialize(JSONObject payload) throws IOException {
        payload.serialize(writer);
    }
}
