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

import static componenttest.topology.impl.LibertyServerFactory.getLibertyServer;
import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryFFDCTest.SERVER_NAME;
import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryFFDCTest.installUserFeatureAndApp;
import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryFFDCTest.removeUserFeaturesAndStopServer;
import static io.openliberty.microprofile.telemetry.logging.internal_fat.TelemetryFFDCTest.testTelemetryFFDCMessages;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
@CheckpointTest
public class TelemetryFFDCCheckpointTest extends FATServletClient {

    @Server(SERVER_NAME)
    public static LibertyServer server;

    //This test will run on all mp 2.0 repeats to ensure we have some test coverage on all versions.
    //I picked it for this because checkpoint is strategic so I picked one of the checkpoint tests
    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry20Repeats();

    @BeforeClass
    public static void initialSetup() throws Exception {
        if (!RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP14_MPTEL20_ID)) {
            server = installUserFeatureAndApp(getLibertyServer(SERVER_NAME));

            server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
            server.addCheckpointRegexIgnoreMessages("com.ibm.ws.logging.fat.ffdc.servlet", "RuntimeException", "SRVE0207E");
            server.addBootstrapProperties(Collections.singletonMap("io.openliberty.microprofile.telemetry.ffdc.early", "true"));

            server.startServer();
        }
    }

    /**
     * Triggers an FFDC and ensures exception messages are present.
     */
    @Test
    @ExpectedFFDC({ "java.lang.ArithmeticException" })
    @SkipForRepeat({ TelemetryActions.MP14_MPTEL20_ID }) //Checkpoint only supports MP4.1 and higher.
    public void testTelemetryFFDCMessagesCheckpoint() throws Exception {
        testTelemetryFFDCMessages(server, (linesConsoleLog) -> {
            // For checkpoint we expect to NOT see the early ffdc message:
            assertNull("Should not contain early FFDC message FFDC_TEST_BUNDLE_START",
                       linesConsoleLog.stream().filter((l) -> l.contains("FFDC_TEST_BUNDLE_START")).findFirst().orElse(null));

            assertNull("Should not contain early FFDC message FFDC_TEST_INIT",
                       linesConsoleLog.stream().filter((l) -> l.contains("FFDC_TEST_INIT")).findFirst().orElse(null));
        });
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            removeUserFeaturesAndStopServer(server);
        }
    }

}