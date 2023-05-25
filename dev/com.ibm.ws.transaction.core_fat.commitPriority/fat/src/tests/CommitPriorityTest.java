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

import static org.junit.Assert.assertTrue;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class CommitPriorityTest extends FATServletClient {

    public static final String APP_NAME = "commitPriority";
    public static final String SERVLET_NAME = APP_NAME + "/commitPriority";

    @Server("com.ibm.ws.transaction_commitPriority")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "commitPriority.*");

        final int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < 120000) {
            server.setAppStartTimeout(120000);
        }

        final int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < 120000) {
            server.setConfigUpdateTimeout(120000);
        }

        server.setServerStartTimeout(300000);
        LibertyServer.setValidateApps(true);
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

    private final int TIMEOUT = 5000;

    String committingMsg = "Committing resource with priority ";

    @Test
    public void basicEJB() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "basicEJB"), FATServletClient.SUCCESS);

        // Resources should go 3,2,1,12,11,10
        String s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed first: " + s, 0 <= s.indexOf(committingMsg + 3));
        s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed second: " + s, 0 <= s.indexOf(committingMsg + 2));
        s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed third: " + s, 0 <= s.indexOf(committingMsg + 1));
        s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed fourth: " + s, 0 <= s.indexOf(committingMsg + 12));
        s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed fifth: " + s, 0 <= s.indexOf(committingMsg + 11));
        s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed sixth: " + s, 0 <= s.indexOf(committingMsg + 10));
    }

    @Test
    public void basicCDI() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "basicCDI"), FATServletClient.SUCCESS);

        // Resources should go 6,5,4
        String s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed first: " + s, 0 <= s.indexOf(committingMsg + 6));
        s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed second: " + s, 0 <= s.indexOf(committingMsg + 5));
        s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed third: " + s, 0 <= s.indexOf(committingMsg + 4));
    }

    @Test
    public void basicServlet() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "basicServlet"), FATServletClient.SUCCESS);

        // Resources should go 9,8,7
        String s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed first: " + s, 0 <= s.indexOf(committingMsg + 9));
        s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed second: " + s, 0 <= s.indexOf(committingMsg + 8));
        s = server.waitForStringInTraceUsingLastOffset(committingMsg, TIMEOUT);
        assertTrue("wrong resource committed third: " + s, 0 <= s.indexOf(committingMsg + 7));
    }
}
