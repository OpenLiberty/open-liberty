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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class TelemetryMessagesCheckpointTest extends FATServletClient {

    public static final String APP_NAME = "TelemetryServletTestApp";
    public static final String SERVER_NAME = "TelemetryMessageNoApp";

    public static LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String CONSOLE_LOG = "logs/console.log";

    @BeforeClass
    public static void initialSetup() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
        server.startServer();
    }

    /**
     * Ensures Liberty messages are correctly bridged and all attributes are present.
     */
    @Test
    public void testTelemetryMessages() throws Exception {
        String line = server.waitForStringInLog("CWWKF0011I", server.getConsoleLogFile());
        List<String> linesMessagesLog = server.findStringsInFileInLibertyServerRoot("^(?!.*scopeInfo).*\\[.*$", MESSAGE_LOG);
        List<String> linesConsoleLog = server.findStringsInFileInLibertyServerRoot(".*scopeInfo.*", CONSOLE_LOG);

        // for checkpoint we expect to NOT see the message:
        // CWWKC0451I: A server checkpoint "beforeAppStart" was requested.
        assertNull("Should not container early message from checkpoint", linesConsoleLog.stream().filter((l) -> l.contains("CWWKC0451I")).findFirst().orElse(null));

        assertEquals("Messages.log and Telemetry console logs don't match.", linesMessagesLog.size(), linesConsoleLog.size());

        assertNotNull("CWWKF0011I log could not be found.", line);
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