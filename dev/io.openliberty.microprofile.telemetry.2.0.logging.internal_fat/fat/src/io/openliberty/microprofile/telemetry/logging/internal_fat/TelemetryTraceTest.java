/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal_fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * HTTP request tracing tests
 */
@RunWith(FATRunner.class)
public class TelemetryTraceTest extends FATServletClient {

    private static Class<?> c = TelemetryTraceTest.class;

    public static final String SERVER_NAME = "TelemetryTrace";
    public static final String APP_NAME = "MpTelemetryLogApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @Before
    public void testSetup() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");
        server.startServer();

    }

    @After
    public void testTearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Ensures trace logs are bridged and all attributes are present. Also ensures that both runtime
     * and app trace logs are routed when the runtime OTel SDK instance is used.
     */
    @Test
    public void testTelemetryTrace() throws Exception {
        String runtimeLine = server.waitForStringInLog("Returning io.openliberty.microprofile.telemetry.runtime OTEL instance.", server.getConsoleLogFile());
        TestUtils.runApp(server, "logServlet");
        String appLine = server.waitForStringInLog("finest trace", server.getConsoleLogFile());

        Map<String, String> runtimeAttributeMap = new HashMap<String, String>() {
            {
                put("io.openliberty.type", "liberty_trace");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfoFactoryImpl");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        Map<String, String> appAttributeMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.type", "liberty_trace");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        assertNotNull("Returning otel instance log could not be found.", runtimeLine);
        assertTrue("MPTelemetry did not log the correct log level", runtimeLine.contains("TRACE"));
        assertTrue("MPTelemetry did not log the correct message", runtimeLine.contains("Returning io.openliberty.microprofile.telemetry.runtime OTEL instance."));
        checkJsonMessage(runtimeLine, runtimeAttributeMap);

        assertNotNull("App Trace message could not be found.", appLine);
        assertTrue("MPTelemetry did not log the correct message", appLine.contains("finest trace"));
        assertTrue("MPTelemetry did not log the correct log level", appLine.contains("TRACE"));
        checkJsonMessage(appLine, appAttributeMap);
    }

    /**
     * Checks for populated span and trace ID for application logs
     */
    @Test
    public void testTelemetryTraceID() throws Exception {
        TestUtils.runApp(server, "logServlet");
        String line = server.waitForStringInLog("finest trace", server.getConsoleLogFile());

        Map<String, String> appAttributeMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.type", "liberty_trace");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        assertNotNull("App Trace message could not be found.", line);
        assertTrue("MPTelemetry did not log the correct message", line.contains("finest trace"));
        assertTrue("MPTelemetry did not log the correct log level", line.contains("TRACE"));
        assertFalse("MPTelemetry did not populate the trace ID.", line.contains("00000000000000000000000000000000"));
        assertFalse("MPTelemetry did not populate the span ID.", line.contains("0000000000000000"));

        checkJsonMessage(line, appAttributeMap);
    }

    /*
     * Compares telemetry logs to the provided map to verify the bridged attributes and values match.
     */
    private void checkJsonMessage(String line, Map<String, String> attributeMap) {
        final String method = "checkJsonMessage";

        String delimeter = "scopeInfo: io.openliberty.microprofile.telemetry:]";
        int index = line.indexOf(delimeter);

        line = line.substring(index + delimeter.length()).strip();
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

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}