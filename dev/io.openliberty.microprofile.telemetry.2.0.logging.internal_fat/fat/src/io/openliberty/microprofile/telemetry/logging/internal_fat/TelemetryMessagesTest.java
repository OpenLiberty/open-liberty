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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

/**
 * HTTP request tracing tests
 */
@RunWith(FATRunner.class)
public class TelemetryMessagesTest extends FATServletClient {

    private static Class<?> c = TelemetryMessagesTest.class;

    public static final String APP_NAME = "TelemetryServletTestApp";
    public static final String SERVER_NAME = "TelemetryMessageNoApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String CONSOLE_LOG = "logs/console.log";

    @BeforeClass
    public static void initialSetup() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.startServer();
    }

    @Test
    public void testTelemetryMessages() throws Exception {
        String line = server.waitForStringInLog("CWWKF0011I", server.getConsoleLogFile());
        List<String> linesMessagesLog = server.findStringsInFileInLibertyServerRoot("^(?!.*scopeInfo).*\\[.*$", MESSAGE_LOG);
        List<String> linesConsoleLog = server.findStringsInFileInLibertyServerRoot(".*scopeInfo.*", CONSOLE_LOG);

        assertEquals("Messages.log and Telemetry console logs don't match.", linesMessagesLog.size(), linesConsoleLog.size());

        assertTrue("MPTelemetry did not log the correct message", line.contains("The TelemetryMessageNoApp server is ready to run a smarter planet."));
        assertTrue("MPTelemetry did not log server messageID field", line.contains("io.openliberty.message_id=\"CWWKF0011I\""));
        assertTrue("MPTelemetry did not log server module field", line.contains("io.openliberty.module=\"com.ibm.ws.kernel.feature.internal.FeatureManager\""));
        assertTrue("MPTelemetry did not log server sequence field", line.contains("io.openliberty.sequence=\""));
        assertTrue("MPTelemetry did not log server type field", line.contains("io.openliberty.type=\"liberty_message\""));
        assertTrue("MPTelemetry did not log server threadID field", line.contains("thread.id"));
        assertTrue("MPTelemetry did not log server thread name field", line.contains("thread.name"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}