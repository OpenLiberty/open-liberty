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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class WebAppMessageTest extends FATServletClient {

    public static final String APP_NAME = "app2";

    @Server("checkpointFATServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat("checkpointFATServer");

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Test
    public void testAfterAppStartWebAppMessage() throws Exception {
        doTest(CheckpointPhase.AFTER_APP_START);
    }

    @Test
    public void testBeforeAppStartWebAppMessage() throws Exception {
        doTest(CheckpointPhase.BEFORE_APP_START);
    }

    private void doTest(CheckpointPhase phase) throws Exception {
        server.setCheckpoint(phase, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: " + APP_NAME, 0));
                                 if (phase == CheckpointPhase.AFTER_APP_START) {
                                     assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                                   server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + APP_NAME + " started", 0));
                                 }
                                 // make sure the web app URL is not logged on checkpoint side
                                 assertNull("'CWWKT0016I: Web application available' found in log.",
                                            server.waitForStringInLogUsingMark("CWWKT0016I: .*" + APP_NAME, 0));
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        if (phase == CheckpointPhase.BEFORE_APP_START) {
            assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                          server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + APP_NAME + " started", 0));
        }
        // make sure the web app URL is logged on restore side
        assertNotNull("'CWWKT0016I: Web application available' not found in log.",
                      server.waitForStringInLogUsingMark("CWWKT0016I: .*" + APP_NAME));
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
