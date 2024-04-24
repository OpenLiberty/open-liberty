/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * <p>
 * The CVE Service Client makes the connection to the cloud service and then
 * gets back the response containing a JSON Array which contains the list of
 * CVE's that could potentially have impact.
 * </p>
 */
public class CVEServiceClient {

    private static final TraceComponent tc = Tr.register(CVEServiceClient.class);

    /**
     * <p>
     * Retrieves the CVE Data from the cloud service.
     * </p>
     *
     * @param data    Map<String, String>
     * @param urlLink URL link which is set in the Server.xml
     * @return json JSONObject
     * @throws IOException
     */
    public JSONObject retrieveCVEData(Map<String, String> data, String urlLink) throws IOException {

        String jsonData = buildJsonString(data);
        JSONObject json = new JSONObject();

        if (!urlLink.startsWith("https")) {
            throw new MalformedURLException("Invalid protocol, expected https");
        }

        URL url = new URL(urlLink);

        HttpsURLConnection connection = getConnection(url);
        if (connection != null) {
            sendData(connection, jsonData);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, jsonData);
        }

        try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            json = JSONObject.parse(reader);
        }
        return json;
    }

    /**
     * <p>
     * Creates a connection with the cloud Service.
     * </p>
     *
     * @param url URL
     * @return
     * @throws IOException
     */
    private static HttpsURLConnection getConnection(URL url) throws IOException {
        HttpsURLConnection connection = null;
        connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        return connection;
    }

    /**
     * <p>
     * Sends the data collected from the server to the cloud service.
     * </p>
     *
     *
     * @param connection HttpsURLConnection
     * @param jsonData   String
     * @throws ConnectException
     * @throws IOException
     */
    private static void sendData(HttpsURLConnection connection, String jsonData) throws ConnectException, IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    /**
     * <p>
     * Builds the data collected as a string (JSONString) to send.
     * </p>
     *
     * <pre>
     * 	Example:
     * 			 {"productEdition": "edition", "features": ["feature1", "feature2", "feature3"], "productVersion": "12.3.4.56", "iFixes": ["ifix1", "ifix2", "ifix3"], "javaVersion": "17.0.8+7", "id": "STRING", "javaVendor": "javaVendor"}
     * </pre>
     *
     *
     *
     * @param data A Map<String, String>
     * @return A string
     */
    protected String buildJsonString(Map<String, String> data) {
        if (data.isEmpty()) {
            return "{}";
        }
        StringBuilder jsonData = new StringBuilder("{");
        for (String key : data.keySet()) {
            if ("features".equals(key) || "iFixes".equals(key)) {
                jsonData.append("\"").append(key).append("\": [");
                if (data.get(key).length() > 0) {
                    String[] featuresOrIFixes = data.get(key).split(",");
                    for (String featureOrIfix : featuresOrIFixes) {
                        jsonData.append("\"").append(featureOrIfix).append("\", ");
                    }

                    jsonData.setLength(jsonData.length() - 2);
                }

                jsonData.append("], ");

            } else {
                jsonData.append("\"").append(key).append("\": \"").append(data.get(key)).append("\", ");
            }
        }
        jsonData.setLength(jsonData.length() - 2);
        jsonData.append("}");

        return jsonData.toString();
    }

}