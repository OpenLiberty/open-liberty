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
import static org.junit.Assert.fail;

import java.io.IOException;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

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
@AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
public class DBRotationTest extends CloudFATServletClient {
    private static final Class<?> c = DBRotationTest.class;

    protected static final int cloud2ServerPort = Integer.parseInt(System.getProperty("HTTP_secondary"));
    protected static final int longLeaseServerPortB = Integer.parseInt(System.getProperty("HTTP_tertiary"));;
    protected static final int longLeaseServerPortC = Integer.parseInt(System.getProperty("HTTP_quaternary"));;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001")
    public static LibertyServer s1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002")
    public static LibertyServer s2;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002.fastcheck")
    public static LibertyServer s4;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002.nopeerlocking")
    public static LibertyServer server2nopeerlocking;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longleasecompete")
    public static LibertyServer s3;

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

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longleaseA")
    public static LibertyServer longLeaseServerA;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longleaseB")
    public static LibertyServer longLeaseServerB;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longleaseC")
    public static LibertyServer longLeaseServerC;

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
                                                        "com.ibm.ws.transaction_ANYDBCLOUD002.fastcheck",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.longleaseA",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.longleaseB",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.longleaseC",
    };

    @BeforeClass
    public static void init() throws Exception {
        Log.info(c, "init", "BeforeClass");

        //System.getProperties().entrySet().stream().forEach(e -> Log.info(DBRotationTest.class, "beforeClass", e.getKey() + " -> " + e.getValue()));

        initialize(s1, s2, "transaction", "/Simple2PCCloudServlet", new SetupRunner() {
            @Override
            public void run(LibertyServer s) throws Exception {
                setUp(s);
            }
        });

        longLeaseCompeteServer1 = s3;
        server2fastcheck = s4;

        final WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "servlets.*");
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

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (!isDerby()) {
            dropTables();
        }
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

        FATUtils.startServers(_runner, server1);

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
        String id = "001";

        serversToCleanup = new LibertyServer[] { server1 };

        FATUtils.startServers(_runner, server1);
        try {
            // We expect this to fail since it is gonna crash the server
            runTest(server1, SERVLET_NAME, "setupRec" + id);
            fail();
        } catch (IOException e) {
        }

        // wait for 1st server to have gone away
        assertNotNull(server1.getServerName() + " did not crash", server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        // The server has been halted but its status variable won't have been reset because we crashed it. In order to
        // setup the server for a restart, set the server state manually.
        server1.setStarted(false);

        // Now re-start cloud1
        FATUtils.startServers(_runner, server1);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        assertNotNull("recovery failed", server1.waitForStringInTrace("Performed recovery for cloud0011", FATUtils.LOG_SEARCH_TIMEOUT));

        // check resource states - retry a few times if this fails
        FATUtils.runWithRetries(() -> runTestWithResponse(server1, SERVLET_NAME, "checkRec" + id).toString());
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
    @AllowedFFDC(value = { "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testDBRecoveryTakeover() throws Exception {
        String id = "001";
        serversToCleanup = new LibertyServer[] { server1, server2 };

        FATUtils.startServers(_runner, server1);
        try {
            // We expect this to fail since it is gonna crash the server
            runTest(server1, SERVLET_NAME, "setupRec" + id);
            fail();
        } catch (IOException e) {
        }

        assertNotNull(server1.getServerName() + " didn't crash properly", server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        // Now start server2
        server2.setHttpDefaultPort(cloud2ServerPort);
        FATUtils.startServers(_runner, server2);

        // Server appears to have started ok. Check for key string to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud0011", FATUtils.LOG_SEARCH_TIMEOUT));

        // check resource states - retry a few times if this fails
        FATUtils.runWithRetries(() -> runTestWithResponse(server2, SERVLET_NAME, "checkRec" + id).toString());

        // Check ABSENCE of WAS_TRAN_LOGCLOUD0011 database table
        runTest(server2, SERVLET_NAME, "testTranlogTableAccess");
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

        FATUtils.startServers(_runner, peerPrecedenceServer1);
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
        FATUtils.startServers(_runner, server2);

        // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud0011", FATUtils.LOG_SEARCH_TIMEOUT));
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

        String id = "001";

        serversToCleanup = new LibertyServer[] { longLeaseCompeteServer1 };

        FATUtils.startServers(_runner, longLeaseCompeteServer1);
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
        FATUtils.startServers(_runner, longLeaseCompeteServer1);

        // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", longLeaseCompeteServer1.waitForStringInTrace("Performed recovery for cloud0011", FATUtils.LOG_SEARCH_TIMEOUT));
    }

    @Test
    @AllowedFFDC(value = { "java.lang.IllegalStateException" })
    public void testLogFailure() throws Exception {

        if (!TxTestContainerSuite.isDerby()) { // Embedded Derby cannot support tests with concurrent server startup
            serversToCleanup = new LibertyServer[] { longLeaseLogFailServer1, server2nopeerlocking };

            longLeaseLogFailServer1.setFFDCChecking(false);
            server2nopeerlocking.setHttpDefaultPort(cloud2ServerPort);
            FATUtils.startServers(_runner, longLeaseLogFailServer1, server2nopeerlocking);

            // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
            // (from server1's point of view) acquire server1's log and recover it.

            //  Check for key string to see whether peer recovery has succeeded
            assertNotNull("peer recovery failed", server2nopeerlocking.waitForStringInTrace("Performed recovery for cloud0011", FATUtils.LOG_SEARCH_TIMEOUT));
            FATUtils.stopServers(server2nopeerlocking);

            // server1 now attempts some 2PC and will fail and terminate because its logs have been taken
            try {
                // We expect this to fail since it is gonna crash the server
                runTest(longLeaseLogFailServer1, SERVLET_NAME, "setupRecLostLog");
            } catch (IOException e) {
            }

            // Check that server1 is dead
            assertNotNull(longLeaseLogFailServer1.getServerName() + " did not shutdown", longLeaseLogFailServer1.waitForStringInLog("CWWKE0036I", FATUtils.LOG_SEARCH_TIMEOUT));
        }
    }

    @Test
    public void testLogFailureNoShutdown() throws Exception {

        if (!TxTestContainerSuite.isDerby()) { // Embedded Derby cannot support tests with concurrent server startup
            serversToCleanup = new LibertyServer[] { noShutdownServer1, server2nopeerlocking };

            noShutdownServer1.setFFDCChecking(false);
            server2nopeerlocking.setHttpDefaultPort(cloud2ServerPort);
            FATUtils.startServers(_runner, noShutdownServer1, server2nopeerlocking);

            // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
            // (from server1's point of view) acquire server1's log and recover it.

            //  Check for key string to see whether peer recovery has succeeded
            assertNotNull("peer recovery failed", server2nopeerlocking.waitForStringInTrace("Performed recovery for cloud0011", FATUtils.LOG_SEARCH_TIMEOUT));
            FATUtils.stopServers(server2nopeerlocking);

            // server1 now attempts some 2PC which will fail because its logs have been taken but the server will NOT terminate
            runTest(noShutdownServer1, SERVLET_NAME, "setupRecLostLog");

            // Check that server1 is not dead
            isDead(noShutdownServer1);
        }
    }

    @Test
    @AllowedFFDC(value = { "com.ibm.ws.recoverylog.spi.LogsUnderlyingTablesMissingException" })
    public void testBackwardCompatibility() throws Exception {

        serversToCleanup = new LibertyServer[] { server1 };

        FATUtils.startServers(_runner, server1);

        runTest(server1, SERVLET_NAME, "setupV1LeaseLog");

        // Check for key string to see whether the lease has been updated with the owner/backendURL combo. Set the trace mark so that
        // we pickup the trace from the next home server lease update where the V1 data will be replaced by V2 and from the claim for
        // cloud0022's logs.
        server1.setTraceMarkToEndOfDefaultTrace();
        assertNotNull("V1 lease Owner column not discovered", server1.waitForStringInTrace("Lease_owner column contained cloud0011$", FATUtils.LOG_SEARCH_TIMEOUT));
        assertNotNull("V2 lease Owner column not inserted", server1.waitForStringInTrace("Insert combined string cloud0011,http", FATUtils.LOG_SEARCH_TIMEOUT));

        assertNotNull("Peer recovery not attempted",
                      server1.waitForStringInTrace("CWRLS0011I: Performing recovery processing for a peer WebSphere server ", FATUtils.LOG_SEARCH_TIMEOUT));
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testLeaseIndexBackwardCompatibility() throws Exception {

        serversToCleanup = new LibertyServer[] { noRecoveryGroupServer1, shortLeaseServer1 };

        FATUtils.startServers(_runner, noRecoveryGroupServer1);

        runTest(noRecoveryGroupServer1, SERVLET_NAME, "setupNonUniqueLeaseLog");

        FATUtils.stopServers(noRecoveryGroupServer1);

        FATUtils.startServers(_runner, shortLeaseServer1);

        // Check for key strings to see whether the lease has been updated and read
        shortLeaseServer1.setTraceMarkToEndOfDefaultTrace();
        assertNotNull("Lease Renewer has not fired", shortLeaseServer1.waitForStringInTrace("Have updated Server row", FATUtils.LOG_SEARCH_TIMEOUT));
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
            FATUtils.startServers(_runner, server2);
            assertNotNull("Home server recovery failed", server2.waitForStringInTrace("Transaction recovery processing for this server is complete", FATUtils.LOG_SEARCH_TIMEOUT));
            FATUtils.startServers(_runner, noRecoveryGroupServer1);

            sb = runTestWithResponse(noRecoveryGroupServer1, SERVLET_NAME, "dropServer2Tables");
            Log.info(c, method, "testReactionToDeletedTables dropServer2Tables returned: " + sb);

            assertNotNull("Failed to drop tables", noRecoveryGroupServer1.waitForStringInTrace("<<< END:   dropServer2Tables", FATUtils.LOG_SEARCH_TIMEOUT));

            sb = runTestWithResponse(server2, SERVLET_NAME, "twoTrans");
            Log.info(c, method, "testReactionToDeletedTables twoTrans returned: " + sb);
            assertNotNull("Home server tables are still present", server2.waitForStringInTrace("Underlying SQL tables missing", FATUtils.LOG_SEARCH_TIMEOUT));
        }
    }

    /**
     * The test is the inverse of testLogFailure() and checks recovery log locking in a peer server environment.
     *
     * Start 2 servers, com.ibm.ws.transaction_ANYDBCLOUD001.longleaseA which is configured with a long (5 minute) leaseTimeout
     * and com.ibm.ws.transaction_ANYDBCLOUD002 which is configured with a 20 second leaseTimeout. com.ibm.ws.transaction_ANYDBCLOUD002
     * does not know that com.ibm.ws.transaction_ANYDBCLOUD001.longleaseA has a much longer leaseTimeout configured so it will prematurely
     * (from com.ibm.ws.transaction_ANYDBCLOUD001.longleaseA's point of view) attempt to
     * acquire com.ibm.ws.transaction_ANYDBCLOUD001.longleaseA's logs. In the database case, the takeover will fail because peer database log
     * locking is enabled by default and com.ibm.ws.transaction_ANYDBCLOUD001.longleaseA "still holds its lock" on its logs.
     *
     * @throws Exception
     */
    @Test
    public void testPeerTakeoverFailure() throws Exception {
        //Servers are concurrent, disable for standalone Derby database which will fail
        if (DatabaseContainerType.valueOf(TxTestContainerSuite.testContainer) == DatabaseContainerType.Derby) {
            return;
        }

        serversToCleanup = new LibertyServer[] { longLeaseServerA, server2 };

        longLeaseServerA.setFFDCChecking(false);
        server2.setHttpDefaultPort(cloud2ServerPort);

        // serverA has waitforrecovery true so server2 will not be started till serverA's logs are fully ready
        FATUtils.startServers(_runner, longLeaseServerA, server2);

        // server2 does not know that serverA has a much longer leaseTimeout configured so it will prematurely
        // (from serverA's point of view) attempt to acquire serverA's log. In the filesystem case,
        // the takeover will fail because serverA still holds its lock.

        //  Check for key string to see whether peer recovery has failed
        assertNotNull("peer recovery unexpectedly succeeded",
                      server2.waitForStringInTrace("WTRN0108I: Peer recovery will not be attempted, this server was unable to claim the logs of the server with recovery identity cloud0011",
                                                   FATUtils.LOG_SEARCH_TIMEOUT));
    }

    /**
     * Same as above but with 3 servers
     *
     * @throws Exception
     */
    @Test
    public void testMultiPeerTakeoverFailure() throws Exception {
        //Servers are concurrent, disable for standalone Derby database which will fail
        if (DatabaseContainerType.valueOf(TxTestContainerSuite.testContainer) == DatabaseContainerType.Derby) {
            return;
        }

        serversToCleanup = new LibertyServer[] { longLeaseServerA, server2, longLeaseServerB, longLeaseServerC };

        longLeaseServerA.setFFDCChecking(false);
        server2.setHttpDefaultPort(cloud2ServerPort);
        longLeaseServerB.setHttpDefaultPort(longLeaseServerPortB);
        longLeaseServerC.setHttpDefaultPort(longLeaseServerPortC);

        FATUtils.startServers(_runner, longLeaseServerA, longLeaseServerB, longLeaseServerC, server2);

        //  Check for key strings to see whether peer recovery has failed
        assertNotNull("First peer recovery unexpectedly succeeded",
                      server2.waitForStringInTrace("WTRN0108I: Peer recovery will not be attempted, this server was unable to claim the logs of the server with recovery identity cloud0011",
                                                   FATUtils.LOG_SEARCH_TIMEOUT));
        assertNotNull("Second peer recovery unexpectedly succeeded",
                      server2.waitForStringInTrace("WTRN0108I: Peer recovery will not be attempted, this server was unable to claim the logs of the server with recovery identity cloud0012",
                                                   FATUtils.LOG_SEARCH_TIMEOUT));
        assertNotNull("Third peer recovery unexpectedly succeeded",
                      server2.waitForStringInTrace("WTRN0108I: Peer recovery will not be attempted, this server was unable to claim the logs of the server with recovery identity cloud0013",
                                                   FATUtils.LOG_SEARCH_TIMEOUT));
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
    }

    @Override
    protected void checkLogPresence() throws Exception {
    }

    @Override
    protected void setupOrphanLease(LibertyServer server, String path, String serverName) throws Exception {
        runTest(server, path, "insertOrphanLease");
    }

    @Override
    protected boolean checkOrphanLeaseExists(LibertyServer server, String path, String serverName) throws Exception {
        try {
            runTest(server, path, "checkOrphanLeaseAbsence");
            return false;
        } catch (Exception e) {
        }
        return true;
    }

    @Override
    protected void setupBatchesOfOrphanLeases(LibertyServer server1, LibertyServer server2, String path) throws Exception {

        // Insert stale leases
        runTest(server1, path, "setupBatchOfOrphanLeases1");

        // Insert more stale leases
        runTest(server2, path, "setupBatchOfOrphanLeases2");
    }

    @Override
    protected String logsMissingMarker() {
        return "WTRN0107W: Peer server .* has missing recovery log SQL tables. Delete its lease";
    }
}