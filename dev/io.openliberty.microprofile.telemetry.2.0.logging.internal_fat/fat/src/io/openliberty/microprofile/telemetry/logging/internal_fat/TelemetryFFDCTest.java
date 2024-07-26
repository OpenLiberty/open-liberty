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

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class TelemetryFFDCTest extends FATServletClient {

    private static Class<?> c = TelemetryFFDCTest.class;

    public static final String APP_NAME = "TelemetryServletTestApp";
    public static final String SERVER_NAME = "TelemetryFFDC";

    private static final String SERVER_NAME_XML = "TelemetryFFDC";
    private static final int CONN_TIMEOUT = 10;
    private static LibertyServer server;

    @BeforeClass
    public static void initialSetup() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME_XML);
        server.saveServerConfiguration();
        ShrinkHelper.defaultDropinApp(server, "ffdc-servlet", "io.openliberty.microprofile.telemetry.logging.internal.fat.ffdc.servlet");

    }

    @Before
    public void setUp() throws Exception {
        if (server != null && !server.isStarted()) {
            // Restore the original server configuration, before starting the server for each test case.
            server.restoreServerConfiguration();
            server.startServer();
        }
    }

    /**
     * Triggers an FFDC and ensures exception messages are present.
     */
    @Test
    @ExpectedFFDC("java.lang.ArithmeticException")
    public void testTelemetryFFDCMessages() throws Exception {
        hitWebPage("ffdc-servlet", "FFDCServlet", true, "?generateFFDC=true");

        String logLevelLine = server.waitForStringInLog(".*scopeInfo.*", server.getConsoleLogFile());
        String exceptionMessageLine = server.waitForStringInLog("exception.message=", server.getConsoleLogFile());
        String exceptionTraceLine = server.waitForStringInLog("exception.stacktrace=\"java.lang.ArithmeticException", server.getConsoleLogFile());
        String exceptionTypeLine = server.waitForStringInLog("exception.type=\"java.lang.ArithmeticException\"", server.getConsoleLogFile());

        assertNotNull("FFDC log could not be found.", logLevelLine);
        assertTrue("FFDC Log level was not logged by MPTelemetry", logLevelLine.contains("WARN "));
        assertNotNull("FFDC Exception.message was not logged by MPTelemetry", exceptionMessageLine);
        assertNotNull("FFDC Exception.stacktrace was not logged by MPTelemetry", exceptionTraceLine);
        assertTrue("FFDC Exception.stacktrace did not contain error message", exceptionTraceLine.contains("by zero"));
        assertNotNull("FFDC Exception.type was not logged by MPTelemetry", exceptionTypeLine);

    }

    @After
    public void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("com.ibm.ws.logging.fat.ffdc.servlet", "ArithmeticException", "SRVE0777E");
        }
    }

    private static void hitWebPage(String contextRoot, String servletName, boolean failureAllowed,
                                   String params) throws MalformedURLException, IOException, ProtocolException, InterruptedException {
        try {
            String urlStr = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + "/" + servletName;
            urlStr = params != null ? urlStr + params : urlStr;
            URL url = new URL(urlStr);
            int expectedResponseCode = failureAllowed ? HttpURLConnection.HTTP_INTERNAL_ERROR : HttpURLConnection.HTTP_OK;
            HttpURLConnection con = HttpUtils.getHttpConnection(url, expectedResponseCode, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            // Make sure the server gave us something back
            assertNotNull(line);
            con.disconnect();
        } catch (IOException e) {
            // A message about a 500 code may be fine
            if (!failureAllowed) {
                throw e;
            }
        }
    }

}