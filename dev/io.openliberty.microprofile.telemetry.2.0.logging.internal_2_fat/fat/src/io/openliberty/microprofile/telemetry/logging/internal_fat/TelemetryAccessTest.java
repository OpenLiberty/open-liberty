/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class TelemetryAccessTest extends FATServletClient {

    private static Class<?> c = TelemetryAccessTest.class;

    public static final String APP_NAME = "MpTelemetryLogApp";

    public static final String SERVER_NAME = "TelemetryAccess";

    private static final String ZERO_SPAN_TRACE_ID = "00000000000000000000000000000000 0000000000000000";

    //This test will run on all mp 2.0 repeats to ensure we have some test coverage on all versions.
    //I chose this one because TelemetryMessages is core to this bucket
    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry20Repeats();

    @Server(SERVER_NAME)
    public static LibertyServer server;

    // Test server configurations
    public static final String SERVER_XML_ACCESS_SOURCE_DEFAULT = "accessServer.xml";
    public static final String SERVER_XML_ACCESS_SOURCE_CUSTOM = "accessServerCustom.xml";
    public static final String SERVER_XML_ALL_SOURCES_WITH_ACCESS = "allSourcesWithAccess.xml";
    public static final String SERVER_XML_ONLY_ACCESS_FEATURE = "onlyAccessConfiguration.xml";
    public static final String SERVER_XML_ONLY_ACCESS_SOURCE = "onlyAccessSource.xml";
    public static final String SERVER_XML_INVALID_ACCESS_SOURCE = "invalidAccessSource.xml";
    public static final String SERVER_XML_INVALID_ACCESS_FORMAT = "invalidAccessFormat.xml";

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E", "CWWKG0032W" };

    // Explicitly set the log search time out to 15 secs, instead of the default 4 mins set by the fattest.simplicity.LibertyServer.LOG_SEARCH_TIMEOUT,
    // which causes the tests to wait 4 mins each run, where with repeated tests, it adds up to 2+ hours of waiting.
    private static final int LOG_SEARCH_TIMEOUT = 15 * 1000; // in milliseconds.

    @BeforeClass
    public static void initialSetup() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.SERVER_ONLY },
                                "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");

        server.saveServerConfiguration();
    }

    @Before
    public void testSetup() throws Exception {
        if (!server.isStarted())
            server.startServer();
    }

    @After
    public void testCleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);

            // Restore the server configuration, after each test case.
            server.restoreServerConfiguration();
        }
    }

    /**
     * Tests whether access messages are correctly bridged and several default attributes are present.
     */
    @Test
    public void testTelemetryAccessLogs() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure access feature and access source
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_DEFAULT);

        // Trigger an access log event
        TestUtils.runApp(server, "access");

        // Wait for the access log message to be bridged over
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/AccessURL HTTP/1.1'", consoleLogFile);

        // Check if the expected key-value pair is correctly formatted and mapped to OTel.
        Map<String, String> expectedAccessFieldsMap = new HashMap<String, String>() {
            {
                put("http.request.method", "GET");
                put("http.response.status_code", "200");
                put("io.openliberty.access_log.url.path", "/MpTelemetryLogApp/AccessURL");
                put("network.local.port", Integer.toString(server.getHttpDefaultPort()));
                put("io.openliberty.type", "liberty_accesslog");
                put("network.protocol.name", "HTTP");
                put("network.protocol.version", "1.1");
                put("io.openliberty.sequence", ""); // since, the sequence can be random, have to make sure the sequence field is still present.
            }
        };
        TestUtils.checkJsonMessage(accessLine, expectedAccessFieldsMap);
    }

    /**
     * Tests whether access messages are correctly bridged while using a custom set of attributes.
     */
    @Test
    public void testTelemetryCustomAccessLogs() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure access feature and access source
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_CUSTOM);

        // Trigger an access log event
        TestUtils.runApp(server, "access");

        // Wait for the access log message to be bridged over
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/AccessURL HTTP/1.1'", consoleLogFile);

        // Check if the expected key-value pair is correctly formatted and mapped to OTel.
        Map<String, String> expectedAccessFieldsMap = new HashMap<String, String>() {
            {
                put("http.request.method", "GET");
                put("http.response.status_code", "200");
                put("io.openliberty.access_log.url.path", "/MpTelemetryLogApp/AccessURL");
                put("network.local.port", Integer.toString(server.getHttpDefaultPort()));
                put("io.openliberty.type", "liberty_accesslog");
                put("network.protocol.name", "HTTP");
                put("network.protocol.version", "1.1");
                put("io.openliberty.sequence", ""); // since, the sequence can be random, have to make sure the sequence field is still present.
                put("io.openliberty.access_log.bytes_received", ""); // Keeping blank in case byte size changes in the future.
                put("io.openliberty.access_log.request_elapsed_time", ""); // Elapsed time ranges.
                put("io.openliberty.access_log.request_start_time", ""); //Time ranges
                put("io.openliberty.access_log.request_elapsed_time", "");
                put("io.openliberty.access_log.request_first_line", "GET /MpTelemetryLogApp/AccessURL HTTP/1.1");
            }
        };

        TestUtils.checkJsonMessage(accessLine, expectedAccessFieldsMap);
    }

    /*
     * Test a server with all MPTelemetry sources enabled with access and ensure all message, trace, ffdc, and access logs are bridged.
     * MPTelemetry configuration is as follows: <mpTelemetry source="message, trace, ffdc, accessLog"/>
     */
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void testTelemetryAccessLogsWithAllSourcesEnabled() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure all sources
        setConfig(server, messageLogFile, SERVER_XML_ALL_SOURCES_WITH_ACCESS);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an access event
        TestUtils.runApp(server, "logServlet");

        // Wait for the access log message to be bridged over
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/LogURL HTTP/1.1'", consoleLogFile);

        assertNotNull("The access log event was not found.", accessLine);
        checkAccessOTelAttributeMapping(accessLine);

        //Ensure audit log is bridged over, that is generated from an app.
        String auditLine = server.waitForStringInLog("SECURITY_AUTHN", consoleLogFile);
        assertNotNull("The Security Authentication audit event was not found.", auditLine);
        checkAuditOTelAttributeMapping(auditLine);

        //Ensure the other sources - message, trace, and ffdc logs are bridged, as well.
        String messageLine = server.waitForStringInLog("info message", consoleLogFile);
        assertNotNull("Info message could not be found.", messageLine);

        String traceLine = server.waitForStringInLog("finest trace", consoleLogFile);
        assertNotNull("Trace message could not be found.", traceLine);

        TestUtils.runApp(server, "ffdc1");
        String ffdcLine = server.waitForStringInLog("liberty_ffdc", consoleLogFile);
        assertNotNull("FFDC message could not be found.", ffdcLine);

    }

    /*
     * Tests when the access source is dynamically added to the server.xml, with the accessLog configuration already present.
     */
    @Test
    public void testDynamicAccessSourceAddition() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure access feature only
        setConfig(server, messageLogFile, SERVER_XML_ONLY_ACCESS_FEATURE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an access event
        TestUtils.runApp(server, "logServlet");

        // Ensure access log is NOT bridged over, that is generated from an app.
        String accessLine = server.waitForStringInLog("liberty_access", LOG_SEARCH_TIMEOUT, consoleLogFile);
        assertNull("Access logs could be found.", accessLine);

        // Configure <mpTelemetry source="accessLog"/>
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_DEFAULT);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an access event.
        TestUtils.runApp(server, "logServlet");

        //Ensure access log is bridged over, that is generated from an app.
        accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/LogURL HTTP/1.1'", consoleLogFile);
        assertNotNull("Access logs could NOT be found.", accessLine);
        checkAccessOTelAttributeMapping(accessLine);
    }

    /*
     * Tests when the access source is dynamically removed to the server.xml, with the access configuration already present.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDynamicAccessSourceRemoval() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure access configuration and access source
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_DEFAULT);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an access event
        TestUtils.runApp(server, "logServlet");

        //Ensure access log is bridged over, that is generated from an app.
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/LogURL HTTP/1.1'", consoleLogFile);

        assertNotNull("Access logs could NOT be found.", accessLine);
        checkAccessOTelAttributeMapping(accessLine);

        // Remove only access source
        setConfig(server, messageLogFile, SERVER_XML_ONLY_ACCESS_FEATURE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an access event.
        TestUtils.runApp(server, "logServlet");

        // Ensure access log is NOT bridged over, that is generated from an app.
        accessLine = server.waitForStringInLog("liberty_access", LOG_SEARCH_TIMEOUT, consoleLogFile);
        assertNull("Access logs could be found.", accessLine);
    }

    /*
     * Tests when the access configuration is dynamically added to the server.xml, with the access source already present.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDynamicAccessConfigurationAddition() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure access source only
        setConfig(server, messageLogFile, SERVER_XML_ONLY_ACCESS_SOURCE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an access event
        TestUtils.runApp(server, "logServlet");

        // Ensure access log is NOT bridged over, that is generated from an app.
        String accessLine = server.waitForStringInLog("liberty_access", LOG_SEARCH_TIMEOUT, consoleLogFile);
        assertNull("Access logs could be found.", accessLine);

        // Configure access feature
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_DEFAULT);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an access event.
        TestUtils.runApp(server, "logServlet");

        //Ensure access log is bridged over, that is generated from an app.
        accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/LogURL HTTP/1.1'", consoleLogFile);
        assertNotNull("Access logs could NOT be found.", accessLine);
        checkAccessOTelAttributeMapping(accessLine);
    }

    /*
     * Tests when the access configuration is dynamically removed in the server.xml, with the access source already present.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDynamicAccessConfigurationRemoval() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure access feature and access source
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_DEFAULT);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an access event
        TestUtils.runApp(server, "logServlet");

        //Ensure access log is bridged over, that is generated from an app.
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/LogURL HTTP/1.1'", consoleLogFile);
        assertNotNull("Access logs could NOT be found.", accessLine);
        checkAccessOTelAttributeMapping(accessLine);

        // Remove only access feature
        setConfig(server, messageLogFile, SERVER_XML_ONLY_ACCESS_SOURCE);
        server.setMarkToEndOfLog(consoleLogFile);

        // Trigger an access event.
        TestUtils.runApp(server, "logServlet");

        // Ensure access log is NOT bridged over, that is generated from an app.
        accessLine = server.waitForStringInLog("liberty_access", LOG_SEARCH_TIMEOUT, consoleLogFile);
        assertNull("Access logs could be found.", accessLine);
    }

    /*
     * Tests when an invalid access source attribute is configured, a warning is logged.
     * Source configuraton is as follows: <mpTelemetry source="accessLoog"/>
     */
    @Test
    public void testTelemetryInvalidAccessSource() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure invalid access source
        setConfig(server, messageLogFile, SERVER_XML_INVALID_ACCESS_SOURCE);

        // Access events should NOT be bridged over to OpenTelemetry
        String accessLine = server.waitForStringInLog("liberty_access", LOG_SEARCH_TIMEOUT, consoleLogFile);
        assertNull("Access events were bridged to OpenTelemetry.", accessLine);

        // Check if the warning message is logged
        String warningLine = server.waitForStringInLog("CWMOT5005W", messageLogFile);
        assertNotNull("Unknown log source warning was NOT found.", warningLine);
    }

    @Test
    public void testTelemetryInvalidAccessConfiguration() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure invalid access source
        setConfig(server, messageLogFile, SERVER_XML_INVALID_ACCESS_FORMAT);
        server.setMarkToEndOfLog(consoleLogFile);

        TestUtils.runApp(server, "logServlet");

        // Access events should be bridged over to OpenTelemetry
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/LogURL HTTP/1.1'", consoleLogFile);
        assertNotNull("Access events were not bridged to OpenTelemetry.", accessLine);
        checkAccessOTelAttributeMapping(accessLine);

        // Check if the warning message is logged
        String warningLine = server.waitForStringInLog("CWWKG0032W", messageLogFile);
        assertNotNull("Unknown log source warning was NOT found.", warningLine);
    }

    /*
     * Verify that the W3C propagator is properly associating traces and spans with the access log
     */
    @Test
    public void testTelemetryAccessW3CTraceLogs() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure access feature and access source
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_DEFAULT);

        // Trigger an access log event
        TestUtils.runAccessApp(server, "runAccessApp", "w3c");

        // Wait for the access log message to be bridged over
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/AccessURL HTTP/1.1'", consoleLogFile);
        assertFalse("The access log event does NOT contain a valid trace and span id", accessLine.contains(ZERO_SPAN_TRACE_ID));

        // Check if the expected key-value pair is correctly formatted and mapped to OTel.
        Map<String, String> expectedAccessFieldsMap = new HashMap<String, String>() {
            {
                put("http.request.method", "GET");
                put("http.response.status_code", "200");
                put("io.openliberty.access_log.url.path", "/MpTelemetryLogApp/AccessURL");
                put("network.local.port", Integer.toString(server.getHttpDefaultPort()));
                put("io.openliberty.type", "liberty_accesslog");
                put("network.protocol.name", "HTTP");
                put("network.protocol.version", "1.1");
                put("io.openliberty.sequence", ""); // since, the sequence can be random, have to make sure the sequence field is still present.
            }
        };
        TestUtils.checkJsonMessage(accessLine, expectedAccessFieldsMap);
    }

    /*
     * Verify that the b3 propagator is properly associating traces and spans with the access log
     */
    @Test
    public void testTelemetryAccessB3TraceLogs() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure access feature and access source
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_DEFAULT);

        // Trigger an access log event
        TestUtils.runAccessApp(server, "runAccessApp", "b3");

        // Wait for the access log message to be bridged over
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/AccessURL HTTP/1.1'", consoleLogFile);
        assertFalse("The access log event does NOT contain a valid trace and span id", accessLine.contains(ZERO_SPAN_TRACE_ID));

        // Check if the expected key-value pair is correctly formatted and mapped to OTel.
        Map<String, String> expectedAccessFieldsMap = new HashMap<String, String>() {
            {
                put("http.request.method", "GET");
                put("http.response.status_code", "200");
                put("io.openliberty.access_log.url.path", "/MpTelemetryLogApp/AccessURL");
                put("network.local.port", Integer.toString(server.getHttpDefaultPort()));
                put("io.openliberty.type", "liberty_accesslog");
                put("network.protocol.name", "HTTP");
                put("network.protocol.version", "1.1");
                put("io.openliberty.sequence", ""); // since, the sequence can be random, have to make sure the sequence field is still present.
            }
        };
        TestUtils.checkJsonMessage(accessLine, expectedAccessFieldsMap);
    }

    /*
     * Verify that the Jaeger propagator is properly associating traces and spans with the access log
     */
    @Test
    public void testTelemetryAccessJaegerTraceLogs() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();

        // Configure access feature and access source
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_DEFAULT);

        // Trigger an access log event
        TestUtils.runAccessApp(server, "runAccessApp", "jaeger");

        // Wait for the access log message to be bridged over
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/AccessURL HTTP/1.1'", consoleLogFile);
        assertFalse("The access log event does NOT contain a valid trace and span id", accessLine.contains(ZERO_SPAN_TRACE_ID));

        // Check if the expected key-value pair is correctly formatted and mapped to OTel.
        Map<String, String> expectedAccessFieldsMap = new HashMap<String, String>() {
            {
                put("http.request.method", "GET");
                put("http.response.status_code", "200");
                put("io.openliberty.access_log.url.path", "/MpTelemetryLogApp/AccessURL");
                put("network.local.port", Integer.toString(server.getHttpDefaultPort()));
                put("io.openliberty.type", "liberty_accesslog");
                put("network.protocol.name", "HTTP");
                put("network.protocol.version", "1.1");
                put("io.openliberty.sequence", ""); // since, the sequence can be random, have to make sure the sequence field is still present.
            }
        };
        TestUtils.checkJsonMessage(accessLine, expectedAccessFieldsMap);
    }

    /*
     * Verify that invalid trace headers are properly handled and a debug message is logged.
     */
    @Test
    public void testTelemetryAccessInvalidTraceLogs() throws Exception {
        RemoteFile messageLogFile = server.getDefaultLogFile();
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        RemoteFile traceLogFile = server.getDefaultTraceFile();

        // Configure access feature and access source
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_DEFAULT);

        // Trigger an access log event
        TestUtils.runAccessApp(server, "runAccessApp", "invalidHeaderValue");

        // Wait for the access log message to be bridged over
        String accessLine = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/AccessURL HTTP/1.1'", consoleLogFile);
        assertTrue("The access log contains a valid trace and span id", accessLine.contains(ZERO_SPAN_TRACE_ID));

        String traceDebugLine = server.waitForStringInLog("An invalid header value was found for header", traceLogFile);
        assertNotNull("Invalid header debug message was NOT found.", traceDebugLine);

        // Check if the expected key-value pair is correctly formatted and mapped to OTel.
        Map<String, String> expectedAccessFieldsMap = new HashMap<String, String>() {
            {
                put("http.request.method", "GET");
                put("http.response.status_code", "200");
                put("io.openliberty.access_log.url.path", "/MpTelemetryLogApp/AccessURL");
                put("network.local.port", Integer.toString(server.getHttpDefaultPort()));
                put("io.openliberty.type", "liberty_accesslog");
                put("network.protocol.name", "HTTP");
                put("network.protocol.version", "1.1");
                put("io.openliberty.sequence", ""); // since, the sequence can be random, have to make sure the sequence field is still present.
            }
        };
        TestUtils.checkJsonMessage(accessLine, expectedAccessFieldsMap);
    }

    private static void checkAccessOTelAttributeMapping(String accessLine) {
        // Ensures the triggered application access event is mapped correctly.
        Map<String, String> expectedAccessFieldsMap = new HashMap<String, String>() {
            {
                put("http.request.method", "GET");
                put("http.response.status_code", "200");
                put("io.openliberty.access_log.url.path", "/MpTelemetryLogApp/LogURL");
                put("network.local.port", Integer.toString(server.getHttpDefaultPort()));
                put("io.openliberty.type", "liberty_accesslog");
                put("network.protocol.name", "HTTP");
                put("network.protocol.version", "1.1");
                put("io.openliberty.sequence", ""); // since, the sequence can be random, have to make sure the sequence field is still present.
            }
        };
        TestUtils.checkJsonMessage(accessLine, expectedAccessFieldsMap);
    }

    private static void checkAuditOTelAttributeMapping(String auditLine) {
        // Ensures the triggered application audit security event is mapped correctly.
        Map<String, String> expectedAuditFieldsMap = new HashMap<String, String>() {
            {
                put("io.openliberty.type", "liberty_audit");

                put("io.openliberty.audit.event_name", "SECURITY_AUTHN");

                put("io.openliberty.audit.observer.name", "SecurityService");
                put("io.openliberty.audit.observer.type_uri", "service/server");

                put("io.openliberty.audit.outcome", "success");

                put("io.openliberty.audit.reason.reason_code", "200");
                put("io.openliberty.audit.reason.reason_type", "HTTP");

                put("io.openliberty.audit.target.appname", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.LogServlet");
                put("io.openliberty.audit.target.method", "GET");
                put("io.openliberty.audit.target.name", "/MpTelemetryLogApp/LogURL");
                put("io.openliberty.audit.target.realm", "defaultRealm");
                put("io.openliberty.audit.target.type_uri", "service/application/web");
            }
        };
        TestUtils.checkJsonMessage(auditLine, expectedAuditFieldsMap);
    }

    private static void setConfig(LibertyServer server, RemoteFile logFile, String fileName) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), new String[] {});
    }

}