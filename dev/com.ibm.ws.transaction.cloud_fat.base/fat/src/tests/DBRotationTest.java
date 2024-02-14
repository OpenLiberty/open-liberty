/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException" })
public class DBRotationTest extends CloudFATServletClient {
    private static final Class<?> c = DBRotationTest.class;

    private static final int LOG_SEARCH_TIMEOUT = 300000;
    private static final String APP_PATH = "../com.ibm.ws.transaction.cloud_fat.base/";
    protected static final int cloud2ServerPort = 9992;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001")
    public static LibertyServer s1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002")
    public static LibertyServer s2;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002.nopeerlocking")
    public static LibertyServer server2nopeerlocking;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longleasecompete")
    public static LibertyServer longLeaseCompeteServer1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.shortlease")
    public static LibertyServer shortLeaseServer1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002.shortlease")
    public static LibertyServer shortLeaseServer2;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.norecoverygroup")
    public static LibertyServer noRecoveryGroupServer1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.peerServerPrecedence")
    public static LibertyServer peerPrecedenceServer1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longleaselogfail")
    public static LibertyServer longLeaseLogFailServer1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.noShutdown")
    public static LibertyServer noShutdownServer1;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD002",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD002.nopeerlocking",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.longleasecompete",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.peerServerPrecedence",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.longleaselogfail",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.noShutdown",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.shortlease",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.norecoverygroup",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD002.shortlease",
    };

    private LibertyServer[] serversToCleanup;
    private static final String[] toleratedMsgs = new String[] { ".*" };

    public static SetupRunner runner = new SetupRunner() {
        @Override
        public void run(LibertyServer s) throws Exception {
            setUp(s);
        }
    };

    @BeforeClass
    public static void init() throws Exception {
        Log.info(c, "init", "BeforeClass");

        initialize(s1, s2, "transaction", "/Simple2PCCloudServlet", runner);

        final WebArchive app = ShrinkHelper.buildDefaultAppFromPath(APP_NAME, APP_PATH, "servlets.*");
        final DeployOptions[] dO = new DeployOptions[0];

        ShrinkHelper.exportAppToServer(server1, app, dO);
        ShrinkHelper.exportAppToServer(server2, app, dO);
        ShrinkHelper.exportAppToServer(longLeaseCompeteServer1, app, dO);
        ShrinkHelper.exportAppToServer(shortLeaseServer1, app, dO);
        ShrinkHelper.exportAppToServer(shortLeaseServer2, app, dO);
        ShrinkHelper.exportAppToServer(noRecoveryGroupServer1, app, dO);
        ShrinkHelper.exportAppToServer(peerPrecedenceServer1, app, dO);
        ShrinkHelper.exportAppToServer(longLeaseLogFailServer1, app, dO);
        ShrinkHelper.exportAppToServer(noShutdownServer1, app, dO);
        ShrinkHelper.exportAppToServer(server2nopeerlocking, app, dO);
    }

    public static void setUp(LibertyServer server) throws Exception {
        JdbcDatabaseContainer<?> testContainer = TxTestContainerSuite.testContainer;
        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, testContainer);

        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
    }

    @After
    public void cleanup() throws Exception {
        // If any servers have been added to the serversToCleanup array, we'll stop them now
        // test is long gone so we don't care about messages & warnings anymore
        if (serversToCleanup != null && serversToCleanup.length > 0) {
            FATUtils.stopServers(toleratedMsgs, serversToCleanup);
            serversToCleanup = null;
        }

        // Clean up XA resource files
        server1.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

        // Remove tranlog DB
        server1.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    @AfterClass
    public static void teardown() throws Exception {

        TxTestContainerSuite.afterSuite();
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
        String id = "001";

        serversToCleanup = new LibertyServer[] { server1 };

        FATUtils.startServers(runner, server1);

        sb = runTestWithResponse(server1, SERVLET_NAME, "testLeaseTableAccess");

        Log.info(c, method, "testLeaseTableAccess" + id + " returned: " + sb);
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
        final String method = "testDBBaseRecovery";
        StringBuilder sb = null;
        String id = "001";

        serversToCleanup = new LibertyServer[] { server1 };

        Log.info(c, method, "Starting testDBBaseRecovery in DBRotationTest");
        FATUtils.startServers(runner, server1);
        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupRec" + id);
        } catch (IOException e) {
        }
        Log.info(c, method, "back from runTestWithResponse in testDBBaseRecovery, sb is " + sb);
        assertNull("setupRec" + id + " returned: " + sb, sb);

        // wait for 1st server to have gone away
        Log.info(c, method, "wait for first server to go away in testDBBaseRecovery");
        assertNotNull(server1.getServerName() + " did not crash", server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        // The server has been halted but its status variable won't have been reset because we crashed it. In order to
        // setup the server for a restart, set the server state manually.
        server1.setStarted(false);

        Log.info(c, method, "restart server1");

        // Now re-start cloud1
        FATUtils.startServers(runner, server1);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        assertNotNull("peer recovery failed", server1.waitForStringInTrace("Performed recovery for cloud0011", LOG_SEARCH_TIMEOUT));
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
        final String method = "testDBRecoveryTakeover";
        StringBuilder sb = null;
        String id = "001";

        serversToCleanup = new LibertyServer[] { server1, server2 };

        FATUtils.startServers(runner, server1);
        try {
            // We expect this to fail since it is gonna crash the server
            runTest(server1, SERVLET_NAME, "setupRec" + id);
        } catch (IOException e) {
        }

        assertNotNull(server1.getServerName() + " didn't crash properly", server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        // Now start server2
        server2.setHttpDefaultPort(cloud2ServerPort);
        FATUtils.startServers(runner, server2);

        // Server appears to have started ok. Check for key string to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud0011", LOG_SEARCH_TIMEOUT));

        // Check ABSENCE of WAS_TRAN_LOGCLOUD0011 database table
        try {
            sb = runTestWithResponse(server2, SERVLET_NAME, "testTranlogTableAccess");

        } catch (IOException e) {
            Log.info(c, method, "testTranlogTableAccess" + id + " caught exception: " + e);
        }
        Log.info(c, method, "testTranlogTableAccess" + id + " returned: " + sb);
        FATUtils.stopServers(server2);

        if (sb != null && sb.toString().contains("Unexpectedly"))
            fail(sb.toString());
    }

    /**
     * The purpose of this test is to verify correct behaviour when peer servers compete for a log.
     *
     * The Cloud001 server is started and a servlet invoked. The servlet modifies the owner of the server's
     * lease recored in the lease table. This simulates the situation where a peer server has acquired the
     * ownership of the lease and is recovering Cloud001's logs. Finally the servlet halts the server leaving
     * an indoubt transaction.
     *
     * The peerRecoveryPrecedence server.xml attribute is set to "true" to enable the "original" behaviour.
     * So Cloud001 is restarted but should fail to acquire the lease to its recovery logs as it is no longer the owner.
     *
     * @throws Exception
     */
    @Test
//    @ExpectedFFDC(value = { "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.tx.jta.XAResourceNotAvailableException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException",
                           "java.lang.IllegalStateException" })
    // defect 227411, if cloud002 starts slowly, then access to cloud001's indoubt tx
    // XAResources may need to be retried (tx recovery is, in such cases, working as designed.
    public void testDBRecoveryCompeteForLogPeerPrecedence() throws Exception {

        final String method = "testDBRecoveryCompeteForLogPeerPrecedence";
        String id = "001";

        serversToCleanup = new LibertyServer[] { peerPrecedenceServer1, server2 };

        FATUtils.startServers(runner, peerPrecedenceServer1);
        try {
            runTest(peerPrecedenceServer1, SERVLET_NAME, "modifyLeaseOwner");

            // We expect this to fail since it is gonna crash the server
            runTest(peerPrecedenceServer1, SERVLET_NAME, "setupRec" + id);
        } catch (IOException e) {
        }

        assertNotNull(peerPrecedenceServer1.getServerName() + " didn't crash properly", peerPrecedenceServer1.waitForStringInLog(XAResourceImpl.DUMP_STATE));
        peerPrecedenceServer1.postStopServerArchive(); // must explicitly collect since crashed server
        // Need to ensure we have a long (5 minute) timeout for the lease, otherwise we may decide that we CAN delete
        // and renew our own lease. longLeasLengthServer1 is a clone of server1 with a longer lease length.

        // Now re-start server1 but we fully expect this to fail
        try {
            // The server has been halted but its status variable won't have been reset because we crashed it. In order to
            // setup the server for a restart, set the server state manually.
            peerPrecedenceServer1.setStarted(false);
            setUp(peerPrecedenceServer1);
            peerPrecedenceServer1.startServerExpectFailure("recovery-dblog-fail.log", false, true);
        } catch (Exception ex) {
            // Tolerate an exception here, as recovery is asynch and the "successful start" message
            // may have been produced by the main thread before the recovery thread had completed
            Log.info(c, method, "startServerExpectFailure threw exc: " + ex);
        }

        // Server appears to have failed as expected. Check for log failure string
        if (peerPrecedenceServer1.waitForStringInLog("RECOVERY_LOG_FAILED", FATUtils.LOG_SEARCH_TIMEOUT) == null) {
            Exception ex = new Exception("Recovery logs should have failed");
            Log.error(c, "recoveryTestCompeteForLock", ex);
            throw ex;
        }
        peerPrecedenceServer1.postStopServerArchive(); // must explicitly collect since server start failed
        // defect 210055: Now start cloud2 so that we can tidy up the environment, otherwise cloud1
        // is unstartable because its lease is owned by cloud2.
        server2.setHttpDefaultPort(cloud2ServerPort);
        FATUtils.startServers(runner, server2);

        // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud0011", LOG_SEARCH_TIMEOUT));
    }

    /**
     * The purpose of this test is to verify correct behaviour when peer servers compete for a log.
     *
     * The Cloud001 server is started and a servlet invoked. The servlet modifies the owner of the server's
     * lease recored in the lease table. This simulates the situation where a peer server has acquired the
     * ownership of the lease and is recovering Cloud001's logs. Finally the servlet halts the server leaving
     * an indoubt transaction.
     *
     * The default behaviour is that Cloud001 is restarted and wil aggressively acquire the lease to its recovery logs
     * even though it is not the owner.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.tx.jta.XAResourceNotAvailableException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException",
                           "java.lang.IllegalStateException" })
    // defect 227411, if cloud002 starts slowly, then access to cloud001's indoubt tx
    // XAResources may need to be retried (tx recovery is, in such cases, working as designed.
    public void testDBRecoveryCompeteForLog() throws Exception {

        final String method = "testDBRecoveryCompeteForLog";
        String id = "001";

        serversToCleanup = new LibertyServer[] { longLeaseCompeteServer1 };

        FATUtils.startServers(runner, longLeaseCompeteServer1);
        try {
            runTest(longLeaseCompeteServer1, SERVLET_NAME, "modifyLeaseOwner");

            // We expect this to fail since it is gonna crash the server
            runTest(longLeaseCompeteServer1, SERVLET_NAME, "setupRec" + id);
        } catch (IOException e) {
        }

        assertNotNull(longLeaseCompeteServer1.getServerName() + " didn't crash properly", longLeaseCompeteServer1.waitForStringInLog(XAResourceImpl.DUMP_STATE));
        longLeaseCompeteServer1.postStopServerArchive(); // must explicitly collect since crashed server
        // Need to ensure we have a long (5 minute) timeout for the lease, otherwise we may decide that we CAN delete
        // and renew our own lease. longLeasLengthServer1 is a clone of server1 with a longer lease length.

        // The server has been halted but its status variable won't have been reset because we crashed it. In order to
        // setup the server for a restart, set the server state manually.
        longLeaseCompeteServer1.setStarted(false);
        // Now re-start server1
        FATUtils.startServers(runner, longLeaseCompeteServer1);

        // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", longLeaseCompeteServer1.waitForStringInTrace("Performed recovery for cloud0011", LOG_SEARCH_TIMEOUT));
    }

    @Test
    @AllowedFFDC(value = { "java.lang.IllegalStateException" })
    public void testLogFailure() throws Exception {
        final String method = "testLogFailure";
        if (!TxTestContainerSuite.isDerby()) { // Embedded Derby cannot support tests with concurrent server startup
            serversToCleanup = new LibertyServer[] { longLeaseLogFailServer1, server2 };

            longLeaseLogFailServer1.setFFDCChecking(false);
            server2nopeerlocking.setHttpDefaultPort(cloud2ServerPort);
            try {
                FATUtils.startServers(runner, longLeaseLogFailServer1, server2nopeerlocking);
            } catch (Exception e) {
                Log.error(c, method, e);
                // If we're here, the test will fail but we need to make sure both servers are stopped so the next test has a chance
                FATUtils.stopServers(longLeaseLogFailServer1, server2nopeerlocking);
                throw e;
            }

            // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
            // (from server1's point of view) acquire server1's log and recover it.

            //  Check for key string to see whether peer recovery has succeeded
            assertNotNull("peer recovery failed", server2nopeerlocking.waitForStringInTrace("Performed recovery for cloud0011", LOG_SEARCH_TIMEOUT));
            FATUtils.stopServers(server2nopeerlocking);

            // server1 now attempts some 2PC and will fail and terminate because its logs have been taken
            try {
                // We expect this to fail since it is gonna crash the server
                runTest(longLeaseLogFailServer1, SERVLET_NAME, "setupRecLostLog");
            } catch (IOException e) {
            }

            // Check that server1 is dead
            assertNotNull(longLeaseLogFailServer1.getServerName() + " did not shutdown", longLeaseLogFailServer1.waitForStringInLog("CWWKE0036I", LOG_SEARCH_TIMEOUT));

            // The server has been halted but its status variable won't have been reset because we crashed it. In order to
            // setup the server for a restart, set the server state manually.
            longLeaseLogFailServer1.setStarted(false);
        }
        Log.info(c, method, "test complete");
    }

    @Test
    public void testLogFailureNoShutdown() throws Exception {
        final String method = "testLogFailureNoShutdown";
        if (!TxTestContainerSuite.isDerby()) { // Embedded Derby cannot support tests with concurrent server startup
            serversToCleanup = new LibertyServer[] { noShutdownServer1, server2 };

            noShutdownServer1.setFFDCChecking(false);
            server2nopeerlocking.setHttpDefaultPort(cloud2ServerPort);
            try {
                FATUtils.startServers(runner, noShutdownServer1, server2nopeerlocking);
            } catch (Exception e) {
                Log.error(c, method, e);
                // If we're here, the test will fail but we need to make sure both servers are stopped so the next test has a chance
                FATUtils.stopServers(noShutdownServer1, server2nopeerlocking);
                throw e;
            }

            // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
            // (from server1's point of view) acquire server1's log and recover it.

            //  Check for key string to see whether peer recovery has succeeded
            assertNotNull("peer recovery failed", server2nopeerlocking.waitForStringInTrace("Performed recovery for cloud0011", LOG_SEARCH_TIMEOUT));
            FATUtils.stopServers(server2nopeerlocking);

            // server1 now attempts some 2PC which will fail because its logs have been taken but the server will NOT terminate
            runTest(noShutdownServer1, SERVLET_NAME, "setupRecLostLog");

            // Check that server1 is not dead
            try {
                isDead(noShutdownServer1);
                FATUtils.stopServers(new String[] { "WTRN0029E", "WTRN0000E" }, noShutdownServer1);
            } catch (Exception e) {
                // server was stopped
                fail(noShutdownServer1.getServerName() + " stopped unexpectedly");
            }
        }
        Log.info(c, method, "test complete");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testBackwardCompatibility() throws Exception {
        final String method = "testBackwardCompatibility";

        serversToCleanup = new LibertyServer[] { server1 };

        FATUtils.startServers(runner, server1);

        runTest(server1, SERVLET_NAME, "setupV1LeaseLog");

        // Check for key string to see whether the lease has been updated with the owner/backendURL combo. Set the trace mark so that
        // we pickup the trace from the next home server lease update where the V1 data will be replaced by V2 and from the claim for
        // cloud0022's logs.
        server1.setTraceMarkToEndOfDefaultTrace();
        assertNotNull("Lease Owner column not updated", server1.waitForStringInTrace("Lease_owner column contained cloud0011,http", LOG_SEARCH_TIMEOUT));
        assertNotNull("Lease Owner column not inserted", server1.waitForStringInTrace("Insert combined string cloud0011,http", LOG_SEARCH_TIMEOUT));

        // Now tidy up after test
        runTest(server1, SERVLET_NAME, "tidyupV1LeaseLog");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testLeaseIndexBackwardCompatibility() throws Exception {
        final String method = "testLeaseIndexBackwardCompatibility";

        serversToCleanup = new LibertyServer[] { shortLeaseServer1 };

        FATUtils.startServers(runner, noRecoveryGroupServer1);

        runTest(noRecoveryGroupServer1, SERVLET_NAME, "setupNonUniqueLeaseLog");

        FATUtils.stopServers(noRecoveryGroupServer1);

        FATUtils.startServers(runner, shortLeaseServer1);

        // Check for key strings to see whether the lease has been updated and read
        shortLeaseServer1.setTraceMarkToEndOfDefaultTrace();
        assertNotNull("Lease Renewer has not fired", shortLeaseServer1.waitForStringInTrace("Have updated Server row", LOG_SEARCH_TIMEOUT));
        assertNotNull("Lease checker has not fired", shortLeaseServer1.waitForStringInTrace("Lease Table: read recoveryId", LOG_SEARCH_TIMEOUT));
        Log.info(c, method, "testLeaseIndexBackwardCompatibility is complete");
    }

    @Test
    @AllowedFFDC(value = { "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testAggressiveDBRecoveryTakeover() throws Exception {
        final String method = "testAggressiveDBRecoveryTakeover";
        if (!TxTestContainerSuite.isDerby()) { // Embedded Derby cannot support tests with concurrent server startup
            StringBuilder sb = null;

            serversToCleanup = new LibertyServer[] { server1, shortLeaseServer2 };

            FATUtils.startServers(runner, server1);
            try {
                // We expect this to fail since it is gonna crash the server
                runTest(server1, SERVLET_NAME, "setupRecForAggressiveTakeover");
            } catch (IOException e) {
            }

            assertNotNull(server1.getServerName() + " didn't crash properly", server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));

            // Now start server2
            shortLeaseServer2.setHttpDefaultPort(cloud2ServerPort);
            FATUtils.startServers(runner, shortLeaseServer2);
            FATUtils.startServers(runner, server1);

            // Servers appear to have started ok. Check for key string to see whether peer recovery has succeeded
            assertNotNull("peer unexpectedly recovered home server logs", server1.waitForStringInTrace("WTRN0140I: Recovered transaction", LOG_SEARCH_TIMEOUT));
        }
        Log.info(c, method, "test complete");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException",
                           "javax.transaction.SystemException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "com.ibm.ws.recoverylog.spi.LogsUnderlyingTablesMissingException", "java.lang.Exception" })
    public void testReactionToDeletedTables() throws Exception {
        final String method = "testReactionToDeletedTables";
        StringBuilder sb = null;
        if (!TxTestContainerSuite.isDerby()) { // Embedded Derby cannot support tests with concurrent server startup

            serversToCleanup = new LibertyServer[] { server2, noRecoveryGroupServer1 };
            //            server2.setHttpDefaultPort(cloud2ServerPort);
            server2.useSecondaryHTTPPort();
            FATUtils.startServers(runner, server2);
            assertNotNull("Home server recovery failed", server2.waitForStringInTrace("Transaction recovery processing for this server is complete", LOG_SEARCH_TIMEOUT));
            FATUtils.startServers(runner, noRecoveryGroupServer1);

            sb = runTestWithResponse(noRecoveryGroupServer1, SERVLET_NAME, "dropServer2Tables");
            Log.info(c, method, "testReactionToDeletedTables dropServer2Tables returned: " + sb);

            assertNotNull("Failed to drop tables", noRecoveryGroupServer1.waitForStringInTrace("<<< END:   dropServer2Tables", LOG_SEARCH_TIMEOUT));

            sb = runTestWithResponse(server2, SERVLET_NAME, "twoTrans");
            Log.info(c, method, "testReactionToDeletedTables twoTrans returned: " + sb);
            assertNotNull("Home server tables are still present", server2.waitForStringInTrace("Underlying SQL tables missing", LOG_SEARCH_TIMEOUT));
        }
        Log.info(c, method, "testReactionToDeletedTables is complete");
    }

    // Returns false if the server is alive, throws Exception otherwise
    private boolean isDead(LibertyServer server) throws Exception {
        final int status = server.executeServerScript("status", null).getReturnCode();
        if (status == 0) {
            return false;
        }

        final String msg = "Status of " + server.getServerName() + " is " + status;
        Log.info(c, "checkDead", msg);
        throw new Exception(msg);
    }

    @Override
    protected void checkLogAbsence() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    protected void checkLogPresence() throws Exception {
        // TODO Auto-generated method stub
    }
}
