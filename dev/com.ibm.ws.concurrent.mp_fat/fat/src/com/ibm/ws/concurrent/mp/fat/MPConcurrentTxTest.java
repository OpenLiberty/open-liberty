/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.mp.fat.tx.web.MPConcurrentTxTestServlet;

@RunWith(FATRunner.class)
public class MPConcurrentTxTest extends FATServletClient {

    private static final String APP_NAME = "MPConcurrentTxApp";

    @Server("MPConcurrentTxTestServer")
    @TestServlet(servlet = MPConcurrentTxTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "concurrent.mp.fat.tx.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          // From expected timeout of unresolved transaction with setRollbackOnly:
                          "DSRA0302E.*XA_RBROLLBACK", // XAException occurred.  Error code is: XA_RBROLLBACK (100).  Exception is...
                          "DSRA0304E" // XAException occurred. XAException contents and details are...
        );
    }

    @AllowedFFDC({
                   "java.lang.IllegalStateException", // attempt to use same transaction on 2 threads at once
                   "javax.transaction.xa.XAException" // transaction marked rollback-only due to intentionally caused error
    })
    @Test
    public void testTransactionTimesOutAndReleasesLocks() throws Exception {
        server.setMarkToEndOfLog();

        runTest(server, APP_NAME + "/MPConcurrentTestServlet", testName.getMethodName());

        // This test involves an asynchronous transaction timeout, which can continue logging FFDC and error messages on another
        // thread after the test's servlet method completes. Wait for the FFDC and error messages to appear in the logs
        // in order to prevent it from overlapping subsequent tests where it would be considered a test failure.
        server.waitForStringInLogUsingMark("FFDC1015I.*IllegalStateException");
        server.waitForStringInLogUsingMark("DSRA0302E.*XA_RBROLLBACK");
    }
}
