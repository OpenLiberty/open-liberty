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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.rules.repeater.CheckpointRule.ServerMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
public class TelemetryAuditCheckpointTest extends FATServletClient {

    public static final String SERVER_NAME = "TelemetryAuditCheckpoint";

    @ClassRule
    public static CheckpointRule checkpointRule = new CheckpointRule()
                    .setConsoleLogName(TelemetryAuditCheckpointTest.class.getSimpleName() + ".log")
                    .setRunNormalTests(false)
                    .setServerSetup(TelemetryAuditCheckpointTest::initialSetup)
                    .setServerStart(TelemetryAuditCheckpointTest::testSetup)
                    .setServerTearDown(TelemetryAuditCheckpointTest::testTearDown);

    //This test will run on all mp 2.0 repeats to ensure we have some test coverage on all versions.
    //I chose this one because TelemetryMessages is core to this bucket
    // Will re-enable in follow-on issue.
    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry20Repeats();

    private static LibertyServer server;

    public static LibertyServer initialSetup(ServerMode mode) throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME, null, true);
        server.saveServerConfiguration();
        return server;
    }

    public static void testSetup(ServerMode mode, LibertyServer server) throws Exception {
        server.startServer();
    }

    public static void testTearDown(ServerMode mode, LibertyServer server) throws Exception {
        server.stopServer();

        // Restore the server configuration, after each test case.
        server.restoreServerConfiguration();
    }

    /**
     * Ensures Audit messages are correctly bridged and all attributes are present.
     */
    @Test
    public void testTelemetryAuditLogs() throws Exception {
        // on restore we expect no audit messages that happened on the checkpoint side
        RemoteFile consoleLog = server.getConsoleLogFile();
        List<String> linesConsoleLog = server.findStringsInLogsUsingMark(".*scopeInfo.*", consoleLog);

        Log.info(getClass(), "testRestoreTelemetryAuditLogs", "Number of logs: " + linesConsoleLog.size());
        for (String line : linesConsoleLog) {
            Log.info(getClass(), "testRestoreTelemetryAuditLogs", line);
        }

        linesConsoleLog = linesConsoleLog.stream().filter((l) -> !l.contains("WebSphere:type=ThreadPoolStats")).collect(Collectors.toList());

        assertEquals("Expected no audit messages on restore", 0, linesConsoleLog.size());
        // generate JMX_MBEAN_REGISTER audit message by hitting the root of the server
        TestUtils.runGetMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort());
        String line = server.waitForStringInLog("JMXService", server.getConsoleLogFile());
        assertNotNull("The JMXService audit event was not not found.", line);
    }

}
