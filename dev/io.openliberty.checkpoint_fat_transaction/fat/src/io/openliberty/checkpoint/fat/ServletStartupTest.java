/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static io.openliberty.checkpoint.fat.FATSuite.stopServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Verify checkpoint fails when a servlet begins a UserTransaction during application
 * startup.
 */

@RunWith(FATRunner.class)
@CheckpointTest
public class ServletStartupTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransactionServletStartup";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EE8FeatureReplacementAction().forServers(SERVER_NAME))
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "transactionservletstartup";
    static final String SERVLET_NAME = APP_NAME + "/StartupServlet";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    TestMethod testMethod;

    @BeforeClass
    public static void setUpClass() throws Exception {
        server.saveServerConfiguration();
    }

    @Before
    public void setUp() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();

        testMethod = getTestMethod(TestMethod.class, testName);
        switch (testMethod) {
            case testServletInitUserTranAtDeployment:
                server.restoreServerConfiguration();
                ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.startup.*");

                server.setCheckpoint(CheckpointPhase.BEFORE_APP_START, false, null);
                break;
            case testServletInitUserTranAtApplications:
                server.restoreServerConfiguration();
                ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.startup.*");

                // Expect server checkpoint and restore to fail
                server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, true, true, null));
                break;
            default:
                break;
        }
    }

    @After
    public void tearDown() throws Exception {
        stopServer(server);
    }

    @Test
    public void testServletInitUserTranAtDeployment() throws Exception {
        // Request a server checkpoint
        server.startServer(getTestMethodNameOnly(testName) + ".log");

        assertNull("The StartupServlet.init() method should not execute checkpoint at=deployment, but did.",
                   server.waitForStringInLogUsingMark("StartupServlet init starting", 1000));

        server.checkpointRestore();

        assertNotNull("The StartupServlet.init() method should complete a user transaction during restore, but did not.",
                      server.waitForStringInLogUsingMark("StartupServlet init completed without exception"));
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testServletInitUserTranAtApplications() throws Exception {
        // Request a server checkpoint
        ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int returnCode = output.getReturnCode();
        assertEquals("The server checkpoint request should return failure code 72, but did not.", 72, returnCode);

        assertNotNull("The transaction manager should log the stack trace of thread that begins a transaction during checkpoint, but did not.",
                      server.waitForStringInLogUsingMark("WTRN0155"));

        assertNotNull("The transaction manager should log it is unable to begin a transaction during checkpoint, but did not.",
                      server.waitForStringInLogUsingMark("WTRN0154"));
    }

    static enum TestMethod {
        testServletInitUserTranAtApplications,
        testServletInitUserTranAtDeployment,
        unknown;
    }
}
