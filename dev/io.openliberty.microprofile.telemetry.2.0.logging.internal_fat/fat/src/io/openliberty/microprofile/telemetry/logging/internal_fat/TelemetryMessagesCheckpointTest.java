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

import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryMessagesTest.APP_NAME;
import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryMessagesTest.SERVER_NAME;
import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryMessagesTest.testTelemetryMessages;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class TelemetryMessagesCheckpointTest extends FATServletClient {

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void testSetup() throws Exception {
        setupServerApp(LibertyServerFactory.getLibertyServer(SERVER_NAME));
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
        server.startServer();
    }

    static LibertyServer setupServerApp(LibertyServer s) throws Exception {
        ShrinkHelper.defaultApp(s, APP_NAME, "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");
        return s;
    }

    /**
     * Ensures Liberty messages are correctly bridged and all attributes are present.
     */
    @Test
    public void testTelemetryMessagesCheckpoint() throws Exception {
        testTelemetryMessages(server, (linesConsoleLog) -> {
            // for checkpoint we expect to NOT see the message:
            // CWWKC0451I: A server checkpoint "beforeAppStart" was requested.
            assertNull("Should not contain early messages from checkpoint", linesConsoleLog.stream().filter((l) -> l.contains("CWWKC0451I")).findFirst().orElse(null));
        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}