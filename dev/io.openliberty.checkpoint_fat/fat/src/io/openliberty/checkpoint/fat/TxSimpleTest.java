/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertNotNull;

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
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import servlets.SimpleServlet;

@RunWith(FATRunner.class)
public class TxSimpleTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransaction";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() //
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly()) //
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "transaction";
    static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    @Server(SERVER_NAME)
    @TestServlet(servlet = SimpleServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "servlets.*");
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

    @Before
    public void setUp() throws Exception {
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                             });
        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer("WTRN0017W");
    }

    @Test
    public void testAsyncFallback() throws Exception {
        runTest("testAsyncFallback");
    }

    @Test
    public void testUserTranLookup() throws Exception {
        runTest("testUserTranLookup");
    }

    @Test
    public void testUserTranFactory() throws Exception {
        runTest("testUserTranFactory");
    }

    @Test
    public void testTranSyncRegistryLookup() throws Exception {
        runTest("testTranSyncRegistryLookup");
    }

    /**
     * Test of basic database connectivity
     */
    @Test
    public void testBasicConnection() throws Exception {
        runTest("testBasicConnection");
    }

    /**
     * Test enlistment in transactions.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    @Test
    public void testTransactionEnlistment() throws Exception {
        runTest("testTransactionEnlistment");
    }

    /**
     * Test that rolling back a newly started UserTransaction doesn't affect the previously implicitly committed
     * LTC transaction.
     */
    @Test
    public void testImplicitLTCCommit() throws Exception {
        runTest("testImplicitLTCCommit");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.NotSupportedException" })
    public void testNEW() throws Exception {
        runTest("testNEW");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.NotSupportedException" })
    public void testNEW2() throws Exception {
        runTest("testNEW2");
    }

    /**
     * Test that rolling back a newly started UserTransaction doesn't affect the previously explicitly committed
     * LTC transaction.
     */
    @Test
    public void testExplicitLTCCommit() throws Exception {
        runTest("testExplicitLTCCommit");
    }

    @Test
    public void testLTCAfterGlobalTran() throws Exception {
        runTest("testLTCAfterGlobalTran");
    }

    @Test
    public void testUOWManagerLookup() throws Exception {
        runTest("testUOWManagerLookup");
    }

    @Test
    public void testUserTranRestriction() throws Exception {
        runTest("testUserTranRestriction");
    }

    @Test
    public void testSetTransactionTimeout() throws Exception {
        runTest("testSetTransactionTimeout");
    }

    @Test
    public void testSingleThreading() throws Exception {
        runTest("testSingleThreading");
    }

    /**
     * Runs the test
     */
    private void runTest(String testName) throws Exception {
        StringBuilder sb = null;
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, testName);

        } catch (Throwable e) {
        }
        Log.info(this.getClass(), testName, testName + " returned: " + sb);

    }
}
