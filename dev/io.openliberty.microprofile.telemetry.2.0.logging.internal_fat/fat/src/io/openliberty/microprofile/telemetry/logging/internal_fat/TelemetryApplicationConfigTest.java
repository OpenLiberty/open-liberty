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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class TelemetryApplicationConfigTest extends FATServletClient {

    private static Class<?> c = TelemetryApplicationConfigTest.class;

    public static final String APP_NAME = "MpTelemetryLogApp";
    public static final String SERVER_NAME = "TelemetryAppConfig";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static final String SERVER_XML_ALL_SOURCES = "allSources.xml";
    public static final String SERVER_XML_NO_TELEMETRY_ATTRIBUTE = "noTelemetryAttribute.xml";

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

    /*
     * Ensure that application logs are configured correctly with an application SDK
     */
    @Test
    public void testTelemetryApplicationMessages() throws Exception {
        TestUtils.runApp(server, "logServlet");

        String runtimeLine = server.waitForStringInLog("CWWKF0011I", 5000, server.getConsoleLogFile());
        String appLine = server.waitForStringInLog("info message", server.getConsoleLogFile());

        Map<String, String> myMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.type", "liberty_message");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        assertNotNull("info message log could not be found.", appLine);
        assertTrue("MPTelemetry did not log the correct message", appLine.contains("info message"));
        assertTrue("MPTelemetry did not log the correct log level", appLine.contains("INFO"));
        checkJsonMessage(appLine, myMap);

        assertNull("MPTelemetry incorrectly bridged runtime logs.", runtimeLine);
    }

    /*
     * Ensure that the LogRecordContext extensions configured by an application are correctly mapped.
     */
    @Test
    public void testTelemetryLogRecordContext() throws Exception {
        TestUtils.runApp(server, "extension");

        String line = server.waitForStringInLog("Test Extension Message", server.getConsoleLogFile());

        Map<String, String> myMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.ext.correctbooleanextension", "true");
                put("io.openliberty.ext.correctbooleanextension2", "false");
                put("io.openliberty.ext.correctfloatextension", "100.12300109863281");
                put("io.openliberty.ext.correctfloatextension2", "-100.12300109863281");
                put("io.openliberty.ext.correctintextension", "12345");
                put("io.openliberty.ext.correctintextension2", "-12345");
                put("io.openliberty.ext.correctstringextension", "Testing string 1234");
                put("io.openliberty.method_name", "Method.Info");

                put("io.openliberty.type", "liberty_message");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        assertNotNull("info message log could not be found.", line);
        assertTrue("MPTelemetry did not log the correct message", line.contains("Test Extension Message"));
        assertTrue("MPTelemetry did not log the correct log level", line.contains("INFO"));
        checkJsonMessage(line, myMap);
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

        Log.info(c, method, "My line: " + line);

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

    private static String setConfig(String fileName, RemoteFile logFile, LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}