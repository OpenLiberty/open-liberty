/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.Simple2PCCloudServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class PostgreSQLTest extends FATServletClient {
    private static final Class<?> c = PostgreSQLTest.class;

    private static final int LOG_SEARCH_TIMEOUT = 300000;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/Simple2PCCloudServlet";
    protected static final int cloud2ServerPort = 9992;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server2;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longlease")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer longLeaseLengthServer1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.noShutdown")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer noShutdownServer1;

    @BeforeClass
    public static void init() throws Exception {
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(longLeaseLengthServer1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(noShutdownServer1, APP_NAME, "com.ibm.ws.transaction.*");
    }

    public static void setUp(LibertyServer server) throws Exception {
        JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;
        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
    }

    @After
    public void cleanup() throws Exception {

        server1.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E");

        // Clean up XA resource files
        server1.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

        // Remove tranlog DB
        server1.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    @Test
    public void testLogFailure() throws Exception {
        final String method = "testLogFailure";

        // First server will get loads of FFDCs
        longLeaseLengthServer1.setFFDCChecking(false);
        server2.setHttpDefaultPort(cloud2ServerPort);
        startServers(longLeaseLengthServer1, server2);

        // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
        // (from server1's point of view) acquire server1's log and recover it.

        //  Check for key string to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud001", LOG_SEARCH_TIMEOUT));
        server2.stopServer();

        // server1 now attempts some 2PC and will fail and terminate because its logs have been taken
        try {
            // We expect this to fail since it is gonna crash the server
            runTestWithResponse(longLeaseLengthServer1, SERVLET_NAME, "setupRecLostLog");
        } catch (Throwable e) {
        }

        int serverStatus = longLeaseLengthServer1.executeServerScript("status", null).getReturnCode();
        Log.info(getClass(), method, "Status of " + longLeaseLengthServer1.getServerName() + " is " + serverStatus);

        int retries = 0;
        while (serverStatus == 0 && retries++ < 50) {
            Thread.sleep(5000);
            serverStatus = longLeaseLengthServer1.executeServerScript("status", null).getReturnCode();
            Log.info(getClass(), method, "Status of " + longLeaseLengthServer1.getServerName() + " is " + serverStatus);
        }

        // server1 should be stopped
        assertFalse(longLeaseLengthServer1.getServerName() + " is not stopped (" + serverStatus + ")", 0 == serverStatus);
    }

    @Test
    public void testLogFailureNoShutdown() throws Exception {
        final String method = "testLogFailureNoShutdown";

        // First server will get loads of FFDCs
        noShutdownServer1.setFFDCChecking(false);
        server2.setHttpDefaultPort(cloud2ServerPort);
        startServers(noShutdownServer1, server2);

        // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
        // (from server1's point of view) acquire server1's log and recover it.

        //  Check for key string to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud001", LOG_SEARCH_TIMEOUT));
        server2.stopServer();

        // server1 now attempts some 2PC which will fail because its logs have been taken but the server will NOT terminate
        runTestWithResponse(noShutdownServer1, SERVLET_NAME, "setupRecLostLog");

        int serverStatus = noShutdownServer1.executeServerScript("status", null).getReturnCode();
        Log.info(getClass(), method, "Status of " + noShutdownServer1.getServerName() + " is " + serverStatus);

        assertFalse(noShutdownServer1.getServerName() + " is not stopped", 1 == serverStatus);

        // If this fails the test failed
        noShutdownServer1.stopServer("WTRN0029E", "WTRN0000E");
    }

    private void startServers(LibertyServer... servers) {
        final String method = "startServers";

        for (LibertyServer server : servers) {
            assertNotNull("Attempted to start a null server", server);
            ProgramOutput po = null;
            try {
                setUp(server);
                po = server.startServerAndValidate(false, false, false);
                if (po.getReturnCode() != 0) {
                    Log.info(getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                    Log.info(getClass(), method, "Stdout: " + po.getStdout());
                    Log.info(getClass(), method, "Stderr: " + po.getStderr());
                    throw new Exception(po.getCommand() + " returned " + po.getReturnCode());
                }
                server.validateAppLoaded(APP_NAME);
            } catch (Throwable t) {
                Log.error(getClass(), method, t);
                assertNull("Failed to start server: " + t.getMessage() + (po == null ? "" : " " + po.getStdout()), t);
            }
        }
    }
}
