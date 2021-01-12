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

import static com.ibm.ws.transaction.test.FATSuite.POSTGRES_DB;
import static com.ibm.ws.transaction.test.FATSuite.POSTGRES_PASS;
import static com.ibm.ws.transaction.test.FATSuite.POSTGRES_USER;
import static com.ibm.ws.transaction.test.FATSuite.postgre;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.Simple2PCCloudServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class PostgreSQLTest extends FATServletClient {
    private static final Class<?> c = PostgreSQLTest.class;

    private static final int LOG_SEARCH_TIMEOUT = 300000;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/Simple2PCCloudServlet";
    protected static final int cloud2ServerPort = 9992;

    @Server("com.ibm.ws.transaction_CLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1;

    @Server("com.ibm.ws.transaction_CLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server2;

    @Server("com.ibm.ws.transaction_CLOUD001.longlease")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer longLeaseLengthServer1;

    @Server("com.ibm.ws.transaction_CLOUD001.noShutdown")
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

        String host = postgre.getContainerIpAddress();
        String port = String.valueOf(postgre.getMappedPort(5432));
        String jdbcURL = postgre.getJdbcUrl() + "?user=" + POSTGRES_USER + "&password=" + POSTGRES_PASS;
        Log.info(c, "setUp", "Using PostgreSQL properties: host=" + host + "  port=" + port + ",  URL=" + jdbcURL);

        server.resetStarted();
        server.addEnvVar("POSTGRES_HOST", host);
        server.addEnvVar("POSTGRES_PORT", port);
        server.addEnvVar("POSTGRES_DB", POSTGRES_DB);
        server.addEnvVar("POSTGRES_USER", POSTGRES_USER);
        server.addEnvVar("POSTGRES_PASS", POSTGRES_PASS);
        server.addEnvVar("POSTGRES_URL", jdbcURL);
        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
    }

    @After
    public void cleanup() throws Exception {
        // Clean up XA resource files
        server1.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

        // Remove tranlog DB
        server1.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    /**
     * Test access to the Lease table.
     *
     * This is a readiness check to verify that resources are available and accessible.
     *
     * @throws Exception
     */
    @Test
    public void testLeaseTableAccess() throws Exception {
        final String method = "testLeaseTableAccess";
        StringBuilder sb = null;

        startServers(server1);

        try {
            sb = runTestWithResponse(server1, SERVLET_NAME, "testLeaseTableAccess");

        } catch (Throwable e) {
        }

        server1.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E");

        Log.info(this.getClass(), method, "testLeaseTableAccess returned: " + sb);
    }

    /**
     * The purpose of this test is as a control to verify that single server recovery is working.
     *
     * The Cloud001 server is started and halted by a servlet that leaves an indoubt transaction.
     * Cloud001 is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testDBBaseRecovery() throws Exception {
        startServers(server1);

        try {
            // We expect this to fail since it is gonna crash the server
            runTestWithResponse(server1, SERVLET_NAME, "setupRec001");
        } catch (Throwable e) {
        }

        assertNotNull(server1.getServerName() + " didn't crash properly", server1.waitForStringInLog("Dump State:"));

        // Now re-start cloud1
        startServers(server1);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        assertNotNull("peer recovery failed", server1.waitForStringInTrace("Performed recovery for cloud001", LOG_SEARCH_TIMEOUT));

        server1.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E");
    }

    /**
     * The purpose of this test is to verify simple peer transaction recovery.
     *
     * The Cloud001 server is started and halted by a servlet that leaves an indoubt transaction.
     * Cloud002, a peer server as it belongs to the same recovery group is started and recovery the
     * transaction that belongs to Cloud001.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testDBRecoveryTakeover() throws Exception {
        startServers(server1);

        try {
            // We expect this to fail since it is gonna crash the server
            runTestWithResponse(server1, SERVLET_NAME, "setupRec001");
        } catch (Throwable e) {
        }

        assertNotNull(server1.getServerName() + " didn't crash properly", server1.waitForStringInLog("Dump State:"));

        // Now start server2
        server2.setHttpDefaultPort(cloud2ServerPort);
        startServers(server2);

        // Server appears to have started ok. Check for key string to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud001", LOG_SEARCH_TIMEOUT));
        server2.stopServer();

        server1.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E");
    }

    /**
     * The purpose of this test is to verify correct behaviour when peer servers compete for a log.
     *
     * The Cloud001 server is started and a servlet invoked. The servlet modifies the owner of the server's
     * lease recored in the lease table. This simulates the situation where a peer server has acquired the
     * ownership of the lease and is recovering Cloud001's logs. Finally the servlet halts the server leaving
     * an indoubt transaction.
     *
     * Cloud001 is restarted but should fail to acquire the lease to its recovery logs as it is no longer the owner.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC(value = { "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.tx.jta.XAResourceNotAvailableException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException",
                           "java.lang.IllegalStateException" })
    // defect 227411, if cloud002 starts slowly, then access to cloud001's indoubt tx
    // XAResources may need to be retried (tx recovery is, in such cases, working as designed.
    public void testDBRecoveryCompeteForLog() throws Exception {
        final String method = "testDBRecoveryCompeteForLog";

        startServers(longLeaseLengthServer1);

        try {
            runTestWithResponse(longLeaseLengthServer1, SERVLET_NAME, "modifyLeaseOwner");

            // We expect this to fail since it is gonna crash the server
            runTestWithResponse(longLeaseLengthServer1, SERVLET_NAME, "setupRec001");
        } catch (Throwable e) {
        }

        assertNotNull(longLeaseLengthServer1.getServerName() + " didn't crash properly", longLeaseLengthServer1.waitForStringInLog("Dump State:"));
        longLeaseLengthServer1.postStopServerArchive(); // must explicitly collect since crashed server
        // Need to ensure we have a long (5 minute) timeout for the lease, otherwise we may decide that we CAN delete
        // and renew our own lease. longLeasLengthServer1 is a clone of server1 with a longer lease length.

        // Now re-start server1 but we fully expect this to fail
        try {
            setUp(longLeaseLengthServer1);
            longLeaseLengthServer1.startServerExpectFailure("recovery-dblog-fail.log", false, true);
        } catch (Exception ex) {
            // Tolerate an exception here, as recovery is asynch and the "successful start" message
            // may have been produced by the main thread before the recovery thread had completed
            Log.info(getClass(), method, "startServerExpectFailure threw exc: " + ex);
        }

        // Server appears to have failed as expected. Check for log failure string
        if (longLeaseLengthServer1.waitForStringInLog("RECOVERY_LOG_FAILED") == null) {
            Exception ex = new Exception("Recovery logs should have failed");
            Log.error(getClass(), "recoveryTestCompeteForLock", ex);
            throw ex;
        }
        longLeaseLengthServer1.postStopServerArchive(); // must explicitly collect since server start failed
        // defect 210055: Now start cloud2 so that we can tidy up the environment, otherwise cloud1
        // is unstartable because its lease is owned by cloud2.
        server2.setHttpDefaultPort(cloud2ServerPort);
        startServers(server2);

        // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud001", LOG_SEARCH_TIMEOUT));
        server2.stopServer();

        server1.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E");
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
