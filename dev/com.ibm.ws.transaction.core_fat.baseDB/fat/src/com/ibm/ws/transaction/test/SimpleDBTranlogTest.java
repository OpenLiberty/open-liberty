/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.transaction.test;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.TxShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import servlets.SimpleServlet;

@RunWith(FATRunner.class)
public class SimpleDBTranlogTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";
    private static final String APP_PATH = "../com.ibm.ws.transaction.core_fat.base/";

    @Server("com.ibm.ws.transaction.dblog")
    @TestServlet(servlet = SimpleServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        TxShrinkHelper.buildDefaultApp(server, APP_NAME, APP_PATH, "servlets.*");

        server.setServerStartTimeout(300000);
        server.startServer();
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
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES })
    public void testAsyncFallbackDBLog() throws Exception {
        runTest("testAsyncFallback");
    }

    @Test
    public void testUserTranLookupDBLog() throws Exception {
        runTest("testUserTranLookup");
    }

    @Test
    public void testUserTranFactoryDBLog() throws Exception {
        runTest("testUserTranFactory");
    }

    @Test
    public void testTranSyncRegistryLookupDBLog() throws Exception {
        runTest("testTranSyncRegistryLookup");
    }

    /**
     * Test of basic database connectivity
     */
    @Test
    public void testBasicConnectionDBLog() throws Exception {
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
    public void testTransactionEnlistmentDBLog() throws Exception {
        runTest("testTransactionEnlistment");
    }

    /**
     * Test that rolling back a newly started UserTransaction doesn't affect the previously implicitly committed
     * LTC transaction.
     */
    @Test
    public void testImplicitLTCCommitDBLog() throws Exception {
        runTest("testImplicitLTCCommit");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.NotSupportedException" })
    public void testNEWDBLog() throws Exception {
        runTest("testNEW");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.NotSupportedException" })
    public void testNEW2DBLog() throws Exception {
        runTest("testNEW2");
    }

    /**
     * Test that rolling back a newly started UserTransaction doesn't affect the previously explicitly committed
     * LTC transaction.
     */
    @Test
    public void testExplicitLTCCommitDBLog() throws Exception {
        runTest("testExplicitLTCCommit");
    }

    @Test
    public void testLTCAfterGlobalTranDBLog() throws Exception {
        runTest("testLTCAfterGlobalTran");
    }

    @Test
    public void testUOWManagerLookupDBLog() throws Exception {
        runTest("testUOWManagerLookup");
    }

    @Test
    public void testUserTranRestrictionDBLog() throws Exception {
        runTest("testUserTranRestriction");
    }

    @Test
    public void testSetTransactionTimeoutDBLog() throws Exception {
        runTest("testSetTransactionTimeout");
    }

    @Test
    public void testSingleThreadingDBLog() throws Exception {
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
