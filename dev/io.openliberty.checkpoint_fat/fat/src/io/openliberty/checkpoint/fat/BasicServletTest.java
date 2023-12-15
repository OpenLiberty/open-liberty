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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import app1.TestServletA;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class BasicServletTest extends FATServletClient {

    public static final String APP_NAME = "app1";

    @Server("checkpointFATServer")
    @TestServlet(servlet = TestServletA.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat("checkpointFATServer");

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "app1");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: app1' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: app1", 0));
                                 assertNotNull("'CWWKZ0001I: Application app1 started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: Application app1 started", 0));
                                 // make sure the web app URL is not logged on checkpoint side
                                 assertNull("'CWWKT0016I: Web application available' found in log.",
                                            server.waitForStringInLogUsingMark("CWWKT0016I: .*app1", 0));
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        // make sure the web app URL is logged on restore side
        assertNotNull("'CWWKT0016I: Web application available' not found in log.",
                      server.waitForStringInLogUsingMark("CWWKT0016I: .*app1"));
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
