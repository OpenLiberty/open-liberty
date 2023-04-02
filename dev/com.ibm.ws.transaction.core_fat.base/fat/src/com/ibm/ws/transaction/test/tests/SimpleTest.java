/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.transaction.test.tests;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import servlets.SimpleServlet;

@RunWith(FATRunner.class)
public class SimpleTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    @Server("com.ibm.ws.transaction")
    @TestServlet(servlet = SimpleServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "servlets.*");

        try {
            server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
            server.startServer();
        } catch (Exception e) {
            Log.error(SimpleTest.class, "setUp", e);
            // Try again
            server.startServer();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                server.stopServer("WTRN0017W");
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

    @Test
    public void testShowPort() throws Exception {
        // Just testing the debug output really
        @SuppressWarnings("unused")
        ProgramOutput startServerExpectFailure = server.startServerExpectFailure("blabla.log", false, false);
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
