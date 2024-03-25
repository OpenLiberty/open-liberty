/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class CheckpointPhaseTest {
    @Rule
    public TestName testName = new TestName();
    public static final String APP_NAME = "app2";

    @Server("checkpointFATServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat("checkpointFATServer");

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "app2");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Test
    public void testAfterAppStartMultRestore() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, (s) -> {
            assertNotNull("App code should have run.", server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
        }));
        server.startServer();
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
        assertEquals("Unexpected app code ran.", null, server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));

        server.stopServer(false, "");
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    public void testBeforeAppStart() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.BEFORE_APP_START, true, (s) -> {
            assertEquals("Unexpected app code ran.", null, s.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
        }));
        server.startServer();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
        assertNotNull("App code should have run.", server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
    }

    @Test
    public void testMultCheckpointNoClean() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");
        server.startServerAndValidate(LibertyServer.DEFAULT_PRE_CLEAN, false /* clean start */,
                                      LibertyServer.DEFAULT_VALIDATE_APPS, false /* expectStartFailure */ );
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    private void restoreServerCheckConsoleLogHeader(LibertyServer server) throws Exception {
        server.checkpointRestore();
        server.waitForStringInLog("Launching checkpointFATServer", 100);
    }

    @Before
    public void setConsoleLogName() {
        server.setConsoleLogName(getTestMethodNameOnly(testName));
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
