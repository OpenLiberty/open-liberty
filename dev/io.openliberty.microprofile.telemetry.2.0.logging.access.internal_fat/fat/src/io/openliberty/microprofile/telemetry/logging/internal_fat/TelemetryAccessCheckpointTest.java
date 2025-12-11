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
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.rules.repeater.CheckpointRule.ServerMode;
import org.junit.rules.RuleChain;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
@SkipForRepeat(TelemetryActions.MP14_MPTEL21_ID)
public class TelemetryAccessCheckpointTest extends FATServletClient {

    public static final String SERVER_NAME = "TelemetryAccessCheckpoint";
    public static final String APP_NAME = "MpTelemetryLogApp";

    @ClassRule
    public static CheckpointRule checkpointRule = new CheckpointRule()
                    .setConsoleLogName(TelemetryAccessCheckpointTest.class.getSimpleName() + ".log")
                    .setRunNormalTests(false)
                    .setServerSetup(TelemetryAccessCheckpointTest::initialSetup)
                    .setServerStart(TelemetryAccessCheckpointTest::testSetup)
                    .setServerTearDown(TelemetryAccessCheckpointTest::testTearDown);

    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry21andLatest20Repeats();

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static LibertyServer initialSetup(ServerMode mode) throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.SERVER_ONLY },
                                "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");

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
     * Ensures Access messages are correctly bridged, then verify no access logs are found after a checkpoint is restored.
     */
    @Test
    public void testTelemetryAccessLogs() throws Exception {
        // on restore we expect no access messages that happened on the checkpoint side
        RemoteFile consoleLog = server.getConsoleLogFile();

        // Trigger an access log event
        TestUtils.runApp(server, "access");

        List<String> linesConsoleLog = server.findStringsInLogsUsingMark(".*scopeInfo.*", consoleLog);

        Log.info(getClass(), "testRestoreTelemetryAccessLogs", "Number of logs: " + linesConsoleLog.size());
        for (String line : linesConsoleLog) {
            Log.info(getClass(), "testRestoreTelemetryAccessLogs", line);
        }

        linesConsoleLog = linesConsoleLog.stream().filter((l) -> !l.contains("http.request.method")).collect(Collectors.toList());

        assertEquals("Expected no access messages on restore", 0, linesConsoleLog.size());

        // Trigger an access log event
        TestUtils.runApp(server, "access");

        String line = server.waitForStringInLog("INFO2 'GET /MpTelemetryLogApp/AccessURL HTTP/1.1", server.getConsoleLogFile());
        assertNotNull("The access log message was not found.", line);
    }

}
