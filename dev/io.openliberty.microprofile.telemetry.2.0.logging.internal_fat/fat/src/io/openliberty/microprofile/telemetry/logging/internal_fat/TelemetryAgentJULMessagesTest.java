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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class TelemetryAgentJULMessagesTest extends FATServletClient {

    private static Class<?> c = TelemetryAgentJULMessagesTest.class;

    public static final String SERVER_NAME = "TelemetryAgentJULMessages";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void initialSetup() throws Exception {
        // Copy the OTel Java agent to the Liberty server root directory.
        server.copyFileToLibertyServerRoot("agent-260/opentelemetry-javaagent.jar");
        ShrinkHelper.defaultDropinApp(server, "ffdc-servlet", "io.openliberty.microprofile.telemetry.logging.internal.fat.ffdc.servlet");
    }

    @Before
    public void setUp() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Ensure JUL message logs are not duplicated when the OpenTelemetry Java agent is active with mpTelemetry-2.0.
     */
    //@Test
    public void testNoDuplicateJULMessageLogsWithOpenTelemetryAgent() throws Exception {
        // Wait for the JUL message (SRVE0250I) to appear in the logs.
        server.waitForStringInLog("SRVE0250I");

        // Wait for the second message (SESN0176I) to arrive, to ensure all the occurrences of the first message (SRVE0250I) has arrived.
        server.waitForStringInLog("SESN0176I");

        //Get all occurrences of the first message (SRVE0250I)
        List<String> agentMappedJulMsg = server.findStringsInLogs("SRVE0250I", server.getConsoleLogFile());
        Log.info(c, "testNoDuplicateJULMessageLogsWithOpenTelemetryAgent()", "Found JUL messages: " + agentMappedJulMsg);

        // There should only be one instance of the JUL message routed to OpenTelemetry.
        assertTrue("There are duplicate JUL messages.", agentMappedJulMsg.size() == 1);

        // Get the JUL message
        String julMsgLine = agentMappedJulMsg.get(0);
        Log.info(c, "testNoDuplicateJULMessageLogsWithOpenTelemetryAgent()", "JUL Msg Line: " + julMsgLine);

        // Verify if the Java agent bridged the JUL message log correctly.
        assertTrue("The OpenTelemetry Agent did not log the correct message.", julMsgLine.contains("SRVE0250I: Web Module ffdc-servlet has been bound to default_host."));
        assertTrue("The OpenTelemetry Agent did not log the instrumentation name correctly, in the scopeInfo.", julMsgLine.contains("scopeInfo: com.ibm.ws.webcontainer:"));
        assertTrue("The OpenTelemetry Agent contains attributes.", julMsgLine.contains("{}"));

        // Wait for the server started message
        server.waitForStringInLog("CWWKF0011I");
        List<String> linesMessagesLog = server.findStringsInLogs("^(?!.*scopeInfo).*\\[.*$", server.getDefaultLogFile());
        List<String> linesConsoleLog = server.findStringsInLogs(".*scopeInfo.*", server.getConsoleLogFile());

        // Verify all the messages got routed over to OTel.
        assertEquals("Messages.log and Telemetry console logs don't match.", linesMessagesLog.size(), linesConsoleLog.size());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}