/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package tests;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SimpleTestNoRecOnStart extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    @Server("transaction_noRecoveryOnStartup")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "transaction.servlets.*");

        try {
            server.setServerStartTimeout(300000);
            server.startServer();
        } catch (Exception e) {
            Log.error(SimpleTestNoRecOnStart.class, "setUp", e);
            // Try again
            server.startServer();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                server.stopServer("WTRN0017W", "CWWKE0701E");
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

    /**
     * Test enlistment in transactions.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    @Test
    public void testTransactionEnlistmentNoRecOnStart() throws Exception {
        runTest(server, SERVLET_NAME, "testTransactionEnlistment");
    }
}
