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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Verify checkpoint fails when a servlet begins a transaction during application
 * startup.
 */

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class ServletStartupTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransactionServletStartup";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "transactionservletstartup";
    static final String SERVLET_NAME = APP_NAME + "/StartupServlet";

    @Server(SERVER_NAME)
//    @TestServlet(servlet = StartupServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "servlets.startup.*");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

    TestMethod testMethod;

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        switch (testMethod) {
            case testServletInitUserTranAtDeployment:
                server.setCheckpoint(CheckpointPhase.DEPLOYMENT, false, null);
                break;
            case testServletInitUserTranAtApplications:
                server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
                break;
            default:
                break;
        }
        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server.startServer(getTestMethod(TestMethod.class, testName) + ".log");
    }

    @After
    public void tearDown() throws Exception {
        stopServer();
    }

    static void stopServer() {
        if (server.isStarted()) {
            try {
                server.stopServer();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testServletInitUserTranAtDeployment() throws Exception {
        assertNull("The StartupServlet.init() method should not execute checkpoint at=deployment, but did.",
                   server.waitForStringInLogUsingMark("StartupServlet init starting", 1000));

        server.checkpointRestore();

        assertNotNull("The StartupServlet.init() method should complete a user transaction during restore, but did not.",
                      server.waitForStringInLogUsingMark("StartupServlet init completed without exception"));
    }

    // TODO: FEATURE SUPPORT NOT YET IMPLEMENTED
    // Modify this test either to verify checkpoint fails when servlet.init() begins
    // a user tx or verify Servlet.init() executes during restore.
    @Test
    public void testServletInitUserTranAtApplications() throws Exception {
        assertNotNull("The StartupServlet.init() method should complete a user transaction during checkpoint at=applications, but did not.",
                      server.waitForStringInLogUsingMark("StartupServlet init completed without exception"));

        server.checkpointRestore();
    }

    static enum TestMethod {
        testServletInitUserTranAtApplications,
        testServletInitUserTranAtDeployment,
        unknown;
    }
}
