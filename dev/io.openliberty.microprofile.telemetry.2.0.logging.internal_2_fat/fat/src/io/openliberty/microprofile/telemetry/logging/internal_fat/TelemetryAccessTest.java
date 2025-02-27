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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class TelemetryAccessTest extends FATServletClient {

    private static Class<?> c = TelemetryAccessTest.class;

    public static final String APP_NAME = "MpTelemetryLogApp";

    public static final String SERVER_NAME = "TelemetryAccess";

    //This test will run on all mp 2.0 repeats to ensure we have some test coverage on all versions.
    //I chose this one because TelemetryMessages is core to this bucket
    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry20Repeats();

    @Server(SERVER_NAME)
    public static LibertyServer server;

    // Test server configurations
    public static final String SERVER_XML_ACCESS_SOURCE_FEATURE = "accessServer.xml";

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E" };

    private static final String ZERO_SPAN_TRACE_ID = "00000000000000000000000000000000 0000000000000000";

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
        setConfig(server, messageLogFile, SERVER_XML_ACCESS_SOURCE_FEATURE);

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

    private static void setConfig(LibertyServer server, RemoteFile logFile, String fileName) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), new String[] {});
    }

}