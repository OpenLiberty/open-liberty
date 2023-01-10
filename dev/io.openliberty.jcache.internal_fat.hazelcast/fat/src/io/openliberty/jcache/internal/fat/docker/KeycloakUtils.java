/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal.fat.docker;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

/**
 * A collection of utilities for the {@link KeycloakContainer}.
 */
@SuppressWarnings("restriction")
public class KeycloakUtils {
    /**
     * Get a JSON array from the input JSON string.
     *
     * @param json The JSON string to create the array from.
     * @return The JSON array.
     */
    public static JsonArray getJsonArray(String json) {
        JsonReader jsonReader = null;
        try {
            jsonReader = Json.createReader(new StringReader(json));
            return jsonReader.readArray();
        } finally {
            if (jsonReader != null) {
                jsonReader.close();
            }
        }
    }

    /**
     * Get a JSON object from the input JSON string.
     *
     * @param json The JSON string to create the object from.
     * @return The JSON object.
     */
    public static JsonObject getJsonObject(String json) {
        JsonReader jsonReader = null;
        try {
            jsonReader = Json.createReader(new StringReader(json));
            return jsonReader.readObject();
        } finally {
            if (jsonReader != null) {
                jsonReader.close();
            }
        }
    }

    /**
     * Get the String response from the HTTP response.
     *
     * @param response The response to get the String from.
     * @return The String response.
     * @throws ParseException If there was an error parsing the response content.
     * @throws IOException    If there was an error reading the response content.
     */
    public static String getStringResponse(CloseableHttpResponse response) throws ParseException, IOException {
        /*
         * Get the response entity.
         */
        HttpEntity entity = response.getEntity();
        Header encodingHeader = entity.getContentEncoding();
        Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charset.forName(encodingHeader.getValue());

        /*
         * Verify we got a response.
         */
        return EntityUtils.toString(entity, encoding);
    }

    /**
     * Get an insecure {@link HttpClient}.
     *
     * @return The {@link HttpClient}.
     * @throws Exception If there was an issue getting the client.
     */
    public static CloseableHttpClient getInsecureHttpClient() throws Exception {
        SSLContext sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(null, (certificate, authType) -> true)
                        .build();
        return HttpClients.custom()
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(new NoopHostnameVerifier())
                        .build();
    }
}
