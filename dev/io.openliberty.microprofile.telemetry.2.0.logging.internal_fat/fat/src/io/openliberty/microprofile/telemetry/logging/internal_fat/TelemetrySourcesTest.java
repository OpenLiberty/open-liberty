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

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class TelemetrySourcesTest extends FATServletClient {

    private static Class<?> c = TelemetrySourcesTest.class;

    public static final String SERVER_NAME = "TelemetrySources";
    public static final String APP_NAME = "MpTelemetryLogApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests rt = TelemetryActions.latestTelemetry20Repeats();

    public static final String SERVER_XML_ALL_SOURCES = "allSources.xml";
    public static final String SERVER_XML_EMPTY_SOURCE = "emptySource.xml";
    public static final String SERVER_XML_NO_SOURCE = "noSource.xml";
    public static final String SERVER_XML_NO_TELEMETRY_ATTRIBUTE = "noTelemetryAttribute.xml";
    public static final String SERVER_XML_INVALID_SOURCE = "invalidSource.xml";
    public static final String SERVER_XML_INVALID_AND_VALID_SOURCES = "invalidAndValidSources.xml";
    public static final String SERVER_XML_MESSAGE_SOURCE = "messageSource.xml";

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E" };

    @BeforeClass
    public static void initialSetup() throws Exception {
        server.saveServerConfiguration();
    }

    @Before
    public void testSetup() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.SERVER_ONLY },
                                "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");
        server.startServer();

    }

    @After
    public void testTearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }

        server.restoreServerConfiguration();
    }

    /*
     * Test a server with all MPTelemetry sources enabled and ensure message, trace and ffdc logs are bridged.
     * MPTelemetry configuration is as follows: <mpTelemetry source="message, trace, ffdc"/>
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
        TestUtils.checkJsonMessage(messageLine, attributeMap);

        //Ensure trace, message and runtime logs are bridged.
        assertNotNull("Trace message could not be found.", traceLine);
        assertNotNull("FFDC message could not be found.", ffdcLine);

    }

    /*
     * Test a server with no MPTelemetry source attribute and ensure message logs are bridged by default.
     * MPTelemetry configuration is as follows: <mpTelemetry/>
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
        TestUtils.checkJsonMessage(messageLine, attributeMap);

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
        TestUtils.checkJsonMessage(messageLine, attributeMap);

        assertNull("Trace message was found and should not have been bridged.", traceLine);
        assertNull("FFDC message was found and should not have been bridged.", ffdcLine);

    }

    /*
     * Test a server with an empty MPTelemetry sources attribute and ensure no logs are bridged.
     * Source configuraton is as follows: <mpTelemetry source=""/>
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

        assertNull("Info message was found and should not have been bridged.", messageLine);
        assertNull("Trace message was found and should not have been bridged.", traceLine);
        assertNull("FFDC message was found and should not have been bridged.", ffdcLine);

    }

    /*
     * Test a server with an invalid MPTelemetry source attribute and a warning is logged.
     * Source configuraton is as follows: <mpTelemetry source="msg"/>
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryInvalidSource() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        setConfig(SERVER_XML_INVALID_SOURCE, consoleLogFile, server);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc1");

        String messageLine = server.waitForStringInLog("info message", 5000, server.getConsoleLogFile());
        String traceLine = server.waitForStringInLog("finest trace", 5000, server.getConsoleLogFile());
        String ffdcLine = server.waitForStringInLog("liberty_ffdc", 5000, server.getConsoleLogFile());

        assertNull("Info message was found and should not have been bridged.", messageLine);
        assertNull("Trace message was found and should not have been bridged.", traceLine);
        assertNull("FFDC message was found and should not have been bridged.", ffdcLine);

        String warningLine = server.waitForStringInLog("CWMOT5005W", server.getDefaultLogFile());

        assertNotNull("Unknown log source warning was not found.", warningLine);
    }

    /*
     * Test a server with both invalid and valid MPTelemetry source attributes and ensure a warning is logged along with the messages being bridged.
     * Source configuraton is as follows: <mpTelemetry source="message, fdc"/>
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryInvalidAndValidSource() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        setConfig(SERVER_XML_INVALID_AND_VALID_SOURCES, consoleLogFile, server);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc1");

        String warningLine = server.waitForStringInLog("CWMOT5005W", server.getDefaultLogFile());
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

        assertNotNull("Unknown log source warning was not found.", warningLine);
        assertNotNull("Info message log could not be found.", messageLine);
        TestUtils.checkJsonMessage(messageLine, attributeMap);

        assertNull("Trace message was found and should not have been bridged.", traceLine);
        assertNull("FFDC message was found and should not have been bridged.", ffdcLine);
    }

    /*
     * Test a server with only the message sources enabled and only ensure message logs are bridged.
     * MPTelemetry configuration is as follows: <mpTelemetry source="message"/>
     * Dynamically update the configuration to include all log sources and ensure both message and ffdcs are bridged.
     * MPTelemetry configuration is as follows: <mpTelemetry source="message, trace, ffdc"/>
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException", "java.lang.ArithmeticException" })
    public void testTelemetryDynamicUpdateAllSources() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        RemoteFile messageLogFile = server.getDefaultLogFile();

        setConfig(SERVER_XML_MESSAGE_SOURCE, messageLogFile, server);
        server.setMarkToEndOfLog(consoleLogFile);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc1");

        String messageLine = server.waitForStringInLogUsingMark("info message", consoleLogFile);
        String ffdcLine = server.waitForStringInLogUsingMark("liberty_ffdc", consoleLogFile);

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
        TestUtils.checkJsonMessage(messageLine, attributeMap);

        assertNull("FFDC message was found and should not have been bridged.", ffdcLine);

        //Update MPTelemetry sources to include messages, trace and ffdc.
        setConfig(SERVER_XML_ALL_SOURCES, messageLogFile, server);
        server.setMarkToEndOfLog(consoleLogFile);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc2");

        messageLine = server.waitForStringInLogUsingMark("info message", consoleLogFile);
        ffdcLine = server.waitForStringInLogUsingMark("liberty_ffdc", consoleLogFile);

        attributeMap = new HashMap<String, String>() {
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
        TestUtils.checkJsonMessage(messageLine, attributeMap);

        assertNotNull("FFDC message could not be found.", ffdcLine);

    }

    /*
     * Test a server with only the message sources enabled and only ensure message logs are bridged.
     * MPTelemetry configuration is as follows: <mpTelemetry source="message"/>
     * Dynamically update the configuration to exclude all log sources and ensure no messages are bridged.
     * MPTelemetry configuration is as follows: <mpTelemetry source=""/>
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryDynamicUpdateEmptySource() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        RemoteFile messageLogFile = server.getDefaultLogFile();

        setConfig(SERVER_XML_MESSAGE_SOURCE, messageLogFile, server);
        server.setMarkToEndOfLog(consoleLogFile);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc1");

        String messageLine = server.waitForStringInLogUsingMark("info message", consoleLogFile);
        String ffdcLine = server.waitForStringInLogUsingMark("liberty_ffdc", consoleLogFile);

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
        TestUtils.checkJsonMessage(messageLine, attributeMap);

        assertNull("FFDC message was found and should not have been bridged.", ffdcLine);

        //Update MPTelemetry sources to exclude all sources
        setConfig(SERVER_XML_EMPTY_SOURCE, messageLogFile, server);

        server.setMarkToEndOfLog(consoleLogFile);

        TestUtils.runApp(server, "logServlet");
        TestUtils.runApp(server, "ffdc1");

        messageLine = null;
        messageLine = server.waitForStringInLogUsingMark("info message", 5000, consoleLogFile);
        ffdcLine = server.waitForStringInLogUsingMark("liberty_ffdc", 5000, consoleLogFile);

        assertNull("App message was found and should not have been bridged.", messageLine);
        assertNull("FFDC message was found and should not have been bridged.", ffdcLine);

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