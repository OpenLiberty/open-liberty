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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class TelemetryFFDCTest extends FATServletClient {

    private static Class<?> c = TelemetryFFDCTest.class;

    public static final String APP_NAME = "MpTelemetryLogApp";
    public static final String SERVER_NAME = "TelemetryFFDC";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E" };

    @BeforeClass
    public static void initialSetup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");

    }

    @Before
    public void setUp() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    /**
     * Triggers an FFDC and ensures exception messages are present.
     */
    @Test
    @ExpectedFFDC("java.lang.NullPointerException")
    public void testTelemetryFFDCMessages() throws Exception {
        //hitWebPage("ffdc-servlet", "FFDCServlet", true, "?generateFFDC=true");
        TestUtils.runApp(server, "ffdc1");

        String logLevelLine = server.waitForStringInLog(".*scopeInfo.*", server.getConsoleLogFile());
        String exceptionMessageLine = server.waitForStringInLog("exception.message=", server.getConsoleLogFile());
        String exceptionTraceLine = server.waitForStringInLog("exception.stacktrace=\"java.lang.NullPointerException", server.getConsoleLogFile());
        String exceptionTypeLine = server.waitForStringInLog("exception.type=\"java.lang.NullPointerException\"", server.getConsoleLogFile());

        assertNotNull("FFDC log could not be found.", logLevelLine);
        assertTrue("FFDC Log level was not logged by MPTelemetry", logLevelLine.contains("WARN "));
        assertNotNull("FFDC Exception.message was not logged by MPTelemetry", exceptionMessageLine);
        assertNotNull("FFDC Exception.stacktrace was not logged by MPTelemetry", exceptionTraceLine);
        assertTrue("FFDC Exception.stacktrace did not contain error message", exceptionTraceLine.contains("java.lang.String.toString()"));
        assertNotNull("FFDC Exception.type was not logged by MPTelemetry", exceptionTypeLine);
    }

    @After
    public void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}