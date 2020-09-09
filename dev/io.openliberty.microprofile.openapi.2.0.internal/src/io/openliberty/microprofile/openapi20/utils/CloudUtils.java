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
package io.openliberty.microprofile.openapi20.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class CloudUtils {
    private static final TraceComponent tc = Tr.register(CloudUtils.class);

    /**
     * The getVCAPHost method checks whether a "VCAP_APPLICATION" environment variable has been set.  In Cloud Foundry
     * applications (where Bluemix runs) this will be set to the actual host that is visible to the user.  In that
     * environment the VHost from Liberty is private and not accessible externally. If the environment variable is set,
     * it extracts the host and returns it;.
     * 
     * @return String
     *          The host specified in VCAP_APPLICATION or null if it is not set.
     */
    @FFDCIgnore(Exception.class)
    public static String getVCAPHost() {
        // Create the variable to return
        String vcapHost = null;
        
        String vcapApplication = System.getenv(Constants.ENV_VAR_VCAP_APPLICATION);
        if (vcapApplication != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode node = objectMapper.readValue(vcapApplication, JsonNode.class);
                ArrayNode uris = (ArrayNode) node.get("uris");
                if (uris != null && uris.size() > 0 && uris.get(0) != null) {
                    vcapHost = uris.get(0).textValue();
                }
            } catch (Exception e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Exception while parsing VCAP_APPLICATION env: " + e.getMessage());
                }
            }
        }
        
        return vcapHost;
    }

    /**
     * Get the resource at the specified URL as an InputStream
     *
     * @param url - resource location
     * @param acceptValue - Request property for 'Accept' header
     */
    public static InputStream getUrlAsStream(URL url, String acceptValue) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(HttpMethod.GET);
        if (acceptValue != null && !acceptValue.trim().isEmpty()) {
            connection.setRequestProperty(HttpHeaders.ACCEPT, acceptValue);
        }
        final int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        } else {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Did not find resource at " + url + ".  ResponseCode: " + responseCode);
            }
        }
        return null;
    }

    private CloudUtils() {
        // This class is not meant to be instantiated.
    }
}
