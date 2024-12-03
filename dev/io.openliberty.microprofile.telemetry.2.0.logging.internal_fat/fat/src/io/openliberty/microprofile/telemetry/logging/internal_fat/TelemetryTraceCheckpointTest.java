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

import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryTraceTest.SERVER_NAME;
import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryTraceTest.setupApp;
import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryTraceTest.testTelemetryTrace;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

/**
 * HTTP request tracing tests
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class TelemetryTraceCheckpointTest extends FATServletClient {

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests rt = TelemetryActions.latestTelemetry20Repeats();

    @BeforeClass
    public static void initialSetup() throws Exception {
        if (!RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP14_MPTEL20_ID)) {
            setupApp(server);
            server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
            server.startServer();
        }
    }

    /**
     * Ensures trace logs are bridged and all attributes are present.
     */
    @Test
    @SkipForRepeat({ TelemetryActions.MP14_MPTEL20_ID }) //Checkpoint only supports MP4.1 and higher.
    public void testTelemetryTraceCheckpoint() throws Exception {
        testTelemetryTrace(server, (linesConsoleLog) -> {
            assertNull("Should not contain early traces from checkpoint",
                       linesConsoleLog.stream().filter((l) -> l.contains("Calling prepare hooks on this list")).findFirst().orElse(null));
        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}