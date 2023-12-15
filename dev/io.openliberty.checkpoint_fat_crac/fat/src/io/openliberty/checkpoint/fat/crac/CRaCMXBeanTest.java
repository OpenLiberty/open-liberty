/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat.crac;

import static io.openliberty.checkpoint.fat.crac.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.fat.crac.app.mxbean.TestCRaCMXBeanServlet;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class CRaCMXBeanTest extends FATServletClient {

    public static final String APP_NAME = "testApp";
    public static final String APP_PACKAGE = TestCRaCMXBeanServlet.class.getPackage().getName();

    @Server("cracFATServer")
    @TestServlet(servlet = TestCRaCMXBeanServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = MicroProfileActions.repeat("cracFATServer", TestMode.FULL, //
                                                                      MicroProfileActions.MP41, // first test in LITE mode
                                                                      // rest are FULL mode
                                                                      MicroProfileActions.MP50, MicroProfileActions.MP60);

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_PACKAGE);
    }

    @Before
    public void setUp() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: ' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: " + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + APP_NAME + " started", 0));
                                 assertNotNull("beforeCheckpoint not called",
                                               server.waitForStringInLogUsingMark("TESTING - beforeCheckpoint - testRestoreTime:", 0));
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        assertNotNull("afterRestore not called",
                      server.waitForStringInLogUsingMark("TESTING - afterRestore - testRestoreTime: ", 0));
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
