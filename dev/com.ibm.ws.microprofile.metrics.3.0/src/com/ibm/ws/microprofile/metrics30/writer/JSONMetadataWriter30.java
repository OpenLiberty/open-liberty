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
import java.util.Locale;

import org.eclipse.microprofile.metrics.Metadata;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics.writer.JSONMetadataWriter;
import com.ibm.ws.microprofile.metrics30.helper.Util30;

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
}
