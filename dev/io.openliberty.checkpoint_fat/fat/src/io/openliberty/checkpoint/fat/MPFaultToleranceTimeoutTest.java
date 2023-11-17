/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class MPFaultToleranceTimeoutTest {

    public static final String APP_NAME = "timeoutTest";

    @Server("timeoutServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat("timeoutServer");

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "timeoutTest");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Test
    public void testCheckpointTimeout() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();

        config.getVariables().getById("timeout").setValue("true");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, server -> {
            String result = server.waitForStringInLogUsingMark("TIMEOUT CALLED", 20000);
            assertNotNull("Timeout call not found at checkpoint", result);
        });

        server.startServer();
        Thread.sleep(22000);
        server.checkpointRestore();
        String result = server.waitForStringInLogUsingMark("TIMEOUT OCCURED", 20000);
        assertNotNull("Timeout not found in log", result);
    }

    @Test
    public void testCheckpointNoTimeout() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();

        config.getVariables().getById("timeout").setValue("false");
        server.updateServerConfiguration(config);
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, server -> {
            String result = server.waitForStringInLogUsingMark("TIMEOUT CALLED", 20000);
            assertNotNull("Timeout call not found at checkpoint", result);
        });
        server.startServer();
        Thread.sleep(22000);
        server.checkpointRestore();
        assertNotNull("Timeout occured", server.waitForStringInLog("timeout did not occur", 20000));
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

    @AfterClass
    public static void removeWebApp() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();
    }

}
