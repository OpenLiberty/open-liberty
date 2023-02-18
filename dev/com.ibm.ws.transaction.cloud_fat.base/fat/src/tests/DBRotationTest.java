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
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import servlets.Simple2PCCloudServlet;

@RunWith(FATRunner.class)
@AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException" })
@SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
public class DBRotationTest extends FATServletClient {
    private static final Class<?> c = DBRotationTest.class;

    private static final int LOG_SEARCH_TIMEOUT = 300000;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/Simple2PCCloudServlet";
    private static final String APP_PATH = "../com.ibm.ws.transaction.cloud_fat.base/";
    protected static final int cloud2ServerPort = 9992;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server2;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longleasecompete")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer longLeaseCompeteServer1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longleaselogfail")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer longLeaseLogFailServer1;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.noShutdown")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer noShutdownServer1;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD002",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.longleasecompete",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.longleaselogfail",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.noShutdown",
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
        TxTestContainerSuite.beforeSuite();
        final WebArchive app = ShrinkHelper.buildDefaultAppFromPath(APP_NAME, APP_PATH, "servlets.*");
        final DeployOptions[] dO = new DeployOptions[0];

        ShrinkHelper.exportAppToServer(server1, app, dO);
        ShrinkHelper.exportAppToServer(server2, app, dO);
        ShrinkHelper.exportAppToServer(longLeaseCompeteServer1, app, dO);
        ShrinkHelper.exportAppToServer(longLeaseLogFailServer1, app, dO);
        ShrinkHelper.exportAppToServer(noShutdownServer1, app, dO);
    }

    public static void setUp(LibertyServer server) throws Exception {
        JdbcDatabaseContainer<?> testContainer = TxTestContainerSuite.testContainer;
        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

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
        String id = "001";

        serversToCleanup = new LibertyServer[] { longLeaseCompeteServer1, server2 };

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

        // Now re-start server1 but we fully expect this to fail
        try {
            // The server has been halted but its status variable won't have been reset because we crashed it. In order to
            // setup the server for a restart, set the server state manually.
            longLeaseCompeteServer1.setStarted(false);
            setUp(longLeaseCompeteServer1);
            longLeaseCompeteServer1.startServerExpectFailure("recovery-dblog-fail.log", false, true);
        } catch (Exception ex) {
            // Tolerate an exception here, as recovery is asynch and the "successful start" message
            // may have been produced by the main thread before the recovery thread had completed
            Log.info(c, method, "startServerExpectFailure threw exc: " + ex);
        }

        // Server appears to have failed as expected. Check for log failure string
        if (longLeaseCompeteServer1.waitForStringInLog("RECOVERY_LOG_FAILED", FATUtils.LOG_SEARCH_TIMEOUT) == null) {
            Exception ex = new Exception("Recovery logs should have failed");
            Log.error(c, "recoveryTestCompeteForLock", ex);
            throw ex;
        }
        longLeaseCompeteServer1.postStopServerArchive(); // must explicitly collect since server start failed
        // defect 210055: Now start cloud2 so that we can tidy up the environment, otherwise cloud1
        // is unstartable because its lease is owned by cloud2.
        server2.setHttpDefaultPort(cloud2ServerPort);
        FATUtils.startServers(runner, server2);

        // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
        assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud0011", LOG_SEARCH_TIMEOUT));
    }

    @Test
    @AllowedFFDC(value = { "java.lang.IllegalStateException" })
    public void testLogFailure() throws Exception {
        final String method = "testLogFailure";
        if (TxTestContainerSuite.databaseContainerType != DatabaseContainerType.Derby) { // Embedded Derby cannot support tests with concurrent server startup
            serversToCleanup = new LibertyServer[] { longLeaseLogFailServer1, server2 };

            longLeaseLogFailServer1.setFFDCChecking(false);
            server2.setHttpDefaultPort(cloud2ServerPort);
            try {
                FATUtils.startServers(runner, longLeaseLogFailServer1, server2);
            } catch (Exception e) {
                Log.error(c, method, e);
                // If we're here, the test will fail but we need to make sure both servers are stopped so the next test has a chance
                FATUtils.stopServers(longLeaseLogFailServer1, server2);
                throw e;
            }

            // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
            // (from server1's point of view) acquire server1's log and recover it.

            //  Check for key string to see whether peer recovery has succeeded
            assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud0011", LOG_SEARCH_TIMEOUT));
            FATUtils.stopServers(server2);

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
        if (TxTestContainerSuite.databaseContainerType != DatabaseContainerType.Derby) { // Embedded Derby cannot support tests with concurrent server startup
            serversToCleanup = new LibertyServer[] { noShutdownServer1, server2 };

            noShutdownServer1.setFFDCChecking(false);
            server2.setHttpDefaultPort(cloud2ServerPort);
            try {
                FATUtils.startServers(runner, noShutdownServer1, server2);
            } catch (Exception e) {
                Log.error(c, method, e);
                // If we're here, the test will fail but we need to make sure both servers are stopped so the next test has a chance
                FATUtils.stopServers(noShutdownServer1, server2);
                throw e;
            }

            // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
            // (from server1's point of view) acquire server1's log and recover it.

            //  Check for key string to see whether peer recovery has succeeded
            assertNotNull("peer recovery failed", server2.waitForStringInTrace("Performed recovery for cloud0011", LOG_SEARCH_TIMEOUT));
            FATUtils.stopServers(server2);

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
}
