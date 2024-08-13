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

import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryTraceTest.testTelemetryTrace;
import static org.junit.Assert.assertNull;

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

/**
 * HTTP request tracing tests
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class TelemetryTraceCheckpointTest extends FATServletClient {

    public static final String SERVER_NAME = "TelemetryTrace";

    public static LibertyServer server;

    @BeforeClass
    public static void initialSetup() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
        server.startServer();
    }

    /**
     * Ensures trace logs are bridged and all attributes are present.
     */
    @Test
    public void testTelemetryTraceCheckpoint() throws Exception {
        testTelemetryTrace(server, (linesConsoleLog) -> {
            assertNull("Should not contain early traces from checkpoint",
                       linesConsoleLog.stream().filter((l) -> l.contains("Calling prepare hooks on this list")).findFirst().orElse(null));
        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}