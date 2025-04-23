/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.logging.internal_fat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Assert;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class TestUtils {

    private static Class<?> c = TestUtils.class;

    //Sample trace headers for each propagator.
    public static String W3C_TRACE_DATA = "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01";
    public static String B3_TRACE_DATA = "e5fee0b8184e2a838aafe4aa959aa21c-4626864da5e71e37-1";
    public static String JAEGER_TRACE_DATA = "322b8ac131b128bcaf56c0c41b84aff5:956ff8b1abbd7993:0:1";
    public static final String ACCESS_TRACE_W3C_HEADER_NAME = "traceparent";
    public static final String ACCESS_TRACE_JAEGER_HEADER_NAME = "uber-trace-id";
    public static final String ACCESS_TRACE_B3_HEADER_NAME = "b3";

    public static void runApp(LibertyServer server, String type) {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/MpTelemetryLogApp";
        if (type.equals("access")) {
            url = url + "/AccessURL";
        } else if (type.equals("ffdc1")) {
            url = url + "?isFFDC=true";
        } else if (type.equals("ffdc2")) {
            url = url + "?secondFFDC=true";
        } else if (type.equals("ffdc3")) {
            url = url + "?thirdFFDC=true";
        } else if (type.equals("trace")) {
            url = url + "/TraceURL";
        } else if (type.equals("logServlet")) {
            url = url + "/LogURL";
        } else if (type.equals("extension")) {
            url = url + "/ExtURL";
        } else if (type.equals("exception")) {
            url = url + "/ExceptionURL";
        } else if (type.equals("landingPage")) {
            url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
        }

        Log.info(c, "runApp", "---> Running the application with url : " + url);

        try {
            runGetMethod(url, null, null);
        } catch (Exception e) {
            Log.info(c, "runApp", " ---> Exception : " + e.getMessage());
        }
    }

    public static void runAccessApp(LibertyServer server, String type, String propagator) {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/MpTelemetryLogApp/AccessURL";
        Log.info(c, "runApp", "---> Running the application with url : " + url);

        String requestHeader = null;
        String traceData = null;
        if (propagator.equals("w3c")) {
            requestHeader = ACCESS_TRACE_W3C_HEADER_NAME;
            traceData = W3C_TRACE_DATA;
        } else if (propagator.equals("b3")) {
            requestHeader = ACCESS_TRACE_B3_HEADER_NAME;
            traceData = B3_TRACE_DATA;
        } else if (propagator.equals("jaeger")) {
            requestHeader = ACCESS_TRACE_JAEGER_HEADER_NAME;
            traceData = JAEGER_TRACE_DATA;
        }

        try {
            if (requestHeader != null)
                runGetMethod(url, requestHeader, traceData);
            else
                runGetMethod(url, null, null);
        } catch (Exception e) {
            Log.info(c, "runApp", " ---> Exception : " + e.getMessage());
        }
    }

    static String runGetMethod(String urlStr, String requestHeader, String requestValue) throws Exception {
        Log.info(c, "runGetMethod", "URL = " + urlStr);
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            if (requestHeader != null) {
                con.setRequestProperty(requestHeader, requestValue);
            }

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            return lines.toString();
        } finally {
            con.disconnect();
        }
    }

    /*
     * Compares telemetry logs to the provided map to verify the bridged attributes and values match.
     */
    static void checkJsonMessage(String line, Map<String, String> attributeMap) {
        final String method = "checkJsonMessage";

        String delimiter = "scopeInfo: io.openliberty.microprofile.telemetry:]";
        int index = line.indexOf(delimiter);

        line = line.substring(index + delimiter.length()).trim();
        line = fixJSON(line);

        JsonReader reader = Json.createReader(new StringReader(line));
        JsonObject jsonObj = reader.readObject();
        reader.close();
        String value = null;
        ArrayList<String> invalidFields = new ArrayList<String>();

        for (String key : jsonObj.keySet()) {
            if (attributeMap.containsKey((key))) {
                value = jsonObj.get(key).toString();
                Log.info(c, method, "key=" + key + ", value=" + (value.replace("\"", "")));

                String mapValue = attributeMap.get(key);

                if (!mapValue.equals("")) {
                    if (mapValue.equals(value.replace("\"", "")))
                        attributeMap.remove(key);
                } else {
                    attributeMap.remove(key);
                }
            }
        }

        if (attributeMap.size() > 0) {
            Log.info(c, method, "Mandatory keys missing: " + attributeMap.toString());
            Assert.fail("Mandatory keys missing: " + attributeMap.toString() + ". Actual JSON was: " + line);
        }
    }

    /*
     * Convert bridges Telemetry logs to valid JSON
     */
    private static String fixJSON(String input) {
        String processed = input.replaceAll("([a-zA-Z0-9_.]+)=", "\"$1\":");

        processed = processed.replaceAll("=([a-zA-Z_][a-zA-Z0-9_.]*)", ":\"$1\"")
                        .replaceAll("=([0-9]+\\.[0-9]+)", ":$1")
                        .replaceAll("=([0-9]+)", ":$1");

        return processed;
    }
}
