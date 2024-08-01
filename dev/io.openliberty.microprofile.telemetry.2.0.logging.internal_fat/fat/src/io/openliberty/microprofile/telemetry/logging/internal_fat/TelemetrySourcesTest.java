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

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class TelemetrySourcesTest extends FATServletClient {

    private static Class<?> c = TelemetrySourcesTest.class;

    public static final String SERVER_NAME = "TelemetrySources";
    public static final String APP_NAME = "MpTelemetryLogApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static final String SERVER_XML_ALL_SOURCES = "allSources.xml";
    public static final String SERVER_XML_EMPTY_SOURCE = "emptySource.xml";
    public static final String SERVER_XML_NO_SOURCE = "noSource.xml";
    public static final String SERVER_XML_NO_TELEMETRY_ATTRIBUTE = "noTelemetryAttribute.xml";
    public static final String SERVER_XML_INVALID_SOURCE = "invalidSource.xml";

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E" };

    @Before
    public void testSetup() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");
        server.startServer();

    }

    @After
    public void testTearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }

    /*
     * Test a server with all MPTelemetry sources enabled and ensure message, trace and ffdc logs are bridged.
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryAllSources() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        setConfig(SERVER_XML_ALL_SOURCES, consoleLogFile, server);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc1");

        String messageLine = server.waitForStringInLog("info message", server.getConsoleLogFile());
        String traceLine = server.waitForStringInLog("finest trace", server.getConsoleLogFile());
        String ffdcLine = server.waitForStringInLog("liberty_ffdc", server.getConsoleLogFile());

        Map<String, String> attributeMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.type", "liberty_message");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        assertNotNull("Info message could not be found.", messageLine);
        assertTrue("MPTelemetry did not log the correct message", messageLine.contains("info message"));
        assertTrue("MPTelemetry did not log the correct message", messageLine.contains("INFO"));
        checkJsonMessage(messageLine, attributeMap);

        assertNotNull("Trace message could not be found.", traceLine);
        assertNotNull("FFDC message could not be found.", ffdcLine);
    }

    /*
     * Test a server with no MPTelemetry source attribute and ensure message logs are bridged by default.
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryNoSource() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        setConfig(SERVER_XML_NO_SOURCE, consoleLogFile, server);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc1");

        String messageLine = server.waitForStringInLog("info message", server.getConsoleLogFile());
        String traceLine = server.waitForStringInLog("finest trace", 5000, server.getConsoleLogFile());
        String ffdcLine = server.waitForStringInLog("liberty_ffdc", 5000, server.getConsoleLogFile());

        Map<String, String> attributeMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.type", "liberty_message");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        assertNotNull("Info message could not be found.", messageLine);
        assertTrue("MPTelemetry did not log the correct message", messageLine.contains("info message"));
        assertTrue("MPTelemetry did not log the correct message", messageLine.contains("INFO"));
        checkJsonMessage(messageLine, attributeMap);

        assertNull("Trace message was found and should not have been bridged.", traceLine);
        assertNull("FFDC message was found and should not have been bridged.", ffdcLine);
    }

    /*
     * Test a server with no MPTelemetry attribute and ensure message logs are bridged by default.
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryNoTelemetryAttribute() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        setConfig(SERVER_XML_NO_TELEMETRY_ATTRIBUTE, consoleLogFile, server);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc1");

        String messageLine = server.waitForStringInLog("info message", server.getConsoleLogFile());
        String traceLine = server.waitForStringInLog("finest trace", 5000, server.getConsoleLogFile());
        String ffdcLine = server.waitForStringInLog("liberty_ffdc", 5000, server.getConsoleLogFile());

        Map<String, String> attributeMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.type", "liberty_message");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        assertNotNull("Info message log could not be found.", messageLine);
        assertTrue("MPTelemetry did not log the correct message", messageLine.contains("info message"));
        assertTrue("MPTelemetry did not log the correct message", messageLine.contains("INFO"));
        checkJsonMessage(messageLine, attributeMap);

        assertNull("Trace message was found and should not have been bridged.", traceLine);
        assertNull("FFDC message was found and should not have been bridged.", traceLine);

    }

    /*
     * Test a server with an empty MPTelemetry sources attribute and ensure no logs are bridged.
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryEmptySource() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        setConfig(SERVER_XML_EMPTY_SOURCE, consoleLogFile, server);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc1");

        String messageLine = server.waitForStringInLog("info message", 5000, server.getConsoleLogFile());
        String traceLine = server.waitForStringInLog("finest trace", 5000, server.getConsoleLogFile());
        String ffdcLine = server.waitForStringInLog("liberty_ffdc", 5000, server.getConsoleLogFile());

        // checkJsonMessage(line, messageKeyList, messageValueList);

        assertNull("Info message was found and should not have been bridged.", messageLine);
        assertNull("Trace message was found and should not have been bridged.", traceLine);
        assertNull("FFDC message was found and should not have been bridged.", traceLine);

    }

    /*
     * Test a server with an invalid MPTelemetry source attribute and a warning is logged.
     */
    @Test
    public void testTelemetryInvalidSource() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        setConfig(SERVER_XML_INVALID_SOURCE, consoleLogFile, server);

        TestUtils.runApp(server, "logServlet");

        String messageLine = server.waitForStringInLog("CWMOT5005W", server.getDefaultLogFile());

        assertNotNull("Unknown log source warning was not found.", messageLine);
    }

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

    private static String setConfig(String fileName, RemoteFile logFile, LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }

}