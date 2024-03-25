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
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.fat.util.TxShrinkHelper;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import suite.FATSuite;
import web.FailoverServlet;

/**
 * These tests are designed to exercise the ability of the SQLMultiScopeRecoveryLog (transaction logs stored
 * in a database) to recover from transient SQL errors, such as those encountered when a High Availability (HA)
 * database fails over.
 *
 * They work as follows:
 *
 * -> A JDBC driver is implemented that wraps the underlying SQL Server driver. The driver is provided in a jar
 * named ifxjdbc.jar. This is inferred to be an Informix driver by the Liberty JDBC driver code. The source for
 * the driver is located in test-bundles/ifxlib/src.
 *
 * The code specific to a database implementation is in the com.informix.database.ConnectionManager class.
 * The generic wrapper code is in the com.informix.jdbcx package,
 *
 * IfxConnection.java
 * IfxConnectionPoolDataSource.java
 * IfxConnectionPoolDataSourceBeanInfo.java
 * IfxDatabaseMetaData.java
 * IfxPooledConnection.java
 * IfxPreparedStatement.java
 * IfxStatement.java
 *
 * The wrapper code generally passes calls straight through to the real jdbc driver it wraps but, when prompted,
 * it can generate SQLExceptions that will be flowed to calling code, such as SQLMultiScopeRecoveryLog.
 *
 * -> The JDBC driver is configured through a table named HATABLE with 3 columns,
 * testtype - is this a runtime or startup test
 * failoverval - how many SQL operations should be executed before generating an SQLException
 * simsqlcode - what sqlcode should be passed in the generated SQLexception
 *
 * Note, in modern versions of jdbc drivers, the driver will generate SQLTransientExceptions rather than SQLExceptions
 * with a specific sqlcode value.
 *
 * -> Each test starts by (re)creating HATable and inserting a row to specify the test characteristics.
 *
 * -> The tests will drive a batch of 2PC transactions using artificial XAResourceImpl resources. The jdbc driver will
 * generate a SQLException at a point defined in the HATABLE row.
 *
 * NOTES ON ADDING TESTS TO CHECK HOW THE SQLMULTISCOPERECOVERYLOG HANDLES DUPLICATE RECOVERY RECORDS
 * ==================================================================================================
 *
 * 1/ This class, FailioverTest, has testDuplicationInRecoveryLogsRestart
 *
 * (A) sb = runTestWithResponse(defaultServer, SERVLET_NAME, "setupForDuplication");
 * Configures the environment for the test
 *
 * (B) sb = runTestWithResponse(defaultServer, SERVLET_NAME, "driveTransactions");
 * Runs a batch of transactions
 *
 * 2/ FailoverServlet has
 *
 * public void setupForDuplication(HttpServletRequest request, HttpServletResponse response) throws Exception {
 * setupTestParameters(request, response, TestType.DUPLICATE, 0, 10, 1);
 * }
 *
 * which drives,
 *
 * stmt.executeUpdate("insert into hatable values (" + testType.ordinal() + ", " + operationToFail + ", " + numberOfFailures + ", " + thesqlcode + ")");
 *
 * In insert a row in hatable. The hatable is the mechanism for conveying info on how to configure the test env.
 *
 * 3/ The hatable configuration is read in the IfxPooledConnection.getConnection() method
 *
 * rsBasic = stmt.executeQuery("SELECT testtype, failingoperation, numberoffailures, simsqlcode" + " FROM hatable");
 *
 * The code fragment where (testTypeInt == 2) is used to set the ifxfdbc code to test duplication in the recovery logs
 *
 * 4/ Duplicate processing occurs each time we call IfxPreparedStatement.executeBatch(). This call is made 3 times in each SQLMultiScopeRecoveryLog.forceSections() invocation,
 * for the set of inserts, updates and deletes.
 *
 * 5/ The insertion of duplicate data into the database is handled by IfxPreparedStatement.duplicateAndHalt();
 *
 * 6/ Duplicate data is assembled though the IfxPreparedStatement.set<Type> methods which call IfxPreparedStatement.collectDataForDuplicateRows()
 *
 * 7/ The handling of duplicate rows in recovery is signalled by the presence of "NMTEST: Replacing item" strings in the server trace. This string is
 * written by SQLRecoverableUnitSectionImpl.addData() when a duplicate log entry is encountered.
 */
@RunWith(FATRunner.class)
@AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", })
//Skip on IBM i test class depends on datasource inferrence
@SkipIfSysProp(SkipIfSysProp.OS_IBMI)
public class FailoverTestLease extends FATServletClient {
    private static final int LOG_SEARCH_TIMEOUT = 300000;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/FailoverServlet";
    public static final String APP_PATH = "../../../../com.ibm.ws.transaction.hadb_fat.derby.1/";

    @Server("com.ibm.ws.transaction_retriablecloud")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer retriableCloudServer;

    @Server("com.ibm.ws.transaction_stalecloud")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer staleCloudServer;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.fastcheck")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1fastcheck;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002.fastcheck")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server2fastcheck;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.longleasecompete")
    public static LibertyServer longLeaseCompeteServer1;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction_retriablecloud",
                                                        "com.ibm.ws.transaction_stalecloud",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.longleasecompete",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001.fastcheck",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD002.fastcheck"
    };

    @BeforeClass
    public static void setUp() throws Exception {
        TxShrinkHelper.buildDefaultApp(retriableCloudServer, APP_NAME, APP_PATH, "web");
        TxShrinkHelper.buildDefaultApp(staleCloudServer, APP_NAME, APP_PATH, "web");
        TxShrinkHelper.buildDefaultApp(longLeaseCompeteServer1, APP_NAME, APP_PATH, "web");
        TxShrinkHelper.buildDefaultApp(server1fastcheck, APP_NAME, APP_PATH, "web");
        TxShrinkHelper.buildDefaultApp(server2fastcheck, APP_NAME, APP_PATH, "web");
    }

    @AfterClass
    public static void afterSuite() {
        FATSuite.afterSuite("HATABLE", "WAS_LEASES_LOG", "WAS_PARTNER_LOGCLOUDSTALE", "WAS_TRAN_LOGCLOUDSTALE");
    }

    public static void setUp(LibertyServer server) throws Exception {
        JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;
        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
    }

    private SetupRunner runner = new SetupRunner() {
        @Override
        public void run(LibertyServer s) throws Exception {
            setUp(s);
        }
    };

    @After
    public void cleanup() throws Exception {
        FailoverTest.commonCleanup(this.getClass().getName());
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Test
    public void testHADBLeaseUpdateFailover() throws Exception {
        final String method = "testHADBLeaseUpdateFailover";
        StringBuilder sb = null;

        FATUtils.startServers(runner, retriableCloudServer);

        Log.info(this.getClass(), method, "Call setupForLeaseUpdate");

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "setupForLeaseUpdate");

        Log.info(this.getClass(), method, "setupForLeaseUpdate returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver on " + retriableCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        Log.info(this.getClass(), method, "set timeout");
        retriableCloudServer.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        FATUtils.startServers(runner, retriableCloudServer);

        Log.info(this.getClass(), method, "Call driveTransactions");

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "driveTransactions");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when updating server lease for server with identity cloud0011
        assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException when updating server lease"));

        // cleanup HATable
        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "dropHATable");
        Log.info(this.getClass(), method, "dropHATable returned: " + sb);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Test
    public void testHADBLeaseDeleteFailover() throws Exception {
        final String method = "testHADBLeaseDeleteFailover";
        StringBuilder sb = null;

        FATUtils.startServers(runner, retriableCloudServer);

        Log.info(this.getClass(), method, "Call setupForLeaseDelete");

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "setupForLeaseDelete");

        Log.info(this.getClass(), method, "setupForLeaseDelete returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver on " + retriableCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        // Ensure that the tables for a stale cloud server have been created
        // And set the com.ibm.ws.recoverylog.disablehomelogdeletion property for the stale server to ensure they survive shutdown.
        Log.info(this.getClass(), method, "call startserver for staleCloudServer");
        FATUtils.startServers(runner, staleCloudServer);
        Log.info(this.getClass(), method, "Call stopserver on " + staleCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, staleCloudServer);

        Log.info(this.getClass(), method, "set timeout");
        retriableCloudServer.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        FATUtils.startServers(runner, retriableCloudServer);

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        Log.info(this.getClass(), method, "insertStaleLease returned: " + sb);

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when deleting server lease for server with identity cloud0011
        List<String> recoveredAlready = retriableCloudServer.findStringsInLogs("Have recovered from SQLException when deleting server lease");
        // if not yet recovered, then wait for message
        if (recoveredAlready == null || recoveredAlready.isEmpty()) {
            assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException when deleting server lease"));
        }

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "deleteStaleLease");

        Log.info(this.getClass(), method, "deleteStaleLease returned: " + sb);

        // cleanup HATable
        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "dropHATable");
        Log.info(this.getClass(), method, "dropHATable returned: " + sb);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Test
    public void testHADBLeaseClaimFailover() throws Exception {
        final String method = "testHADBLeaseClaimFailover";
        StringBuilder sb = null;

        FATUtils.startServers(runner, retriableCloudServer);

        Log.info(this.getClass(), method, "Call setupForLeaseClaim");

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "setupForLeaseClaim");

        Log.info(this.getClass(), method, "setupForLeaseClaim returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver on " + retriableCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        // Ensure that the tables for a stale cloud server have been created
        // And set the com.ibm.ws.recoverylog.disablehomelogdeletion property for the stale server to ensure they survive shutdown.
        Log.info(this.getClass(), method, "call startserver for staleCloudServer");
        FATUtils.startServers(runner, staleCloudServer);
        Log.info(this.getClass(), method, "Call stopserver on " + staleCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, staleCloudServer);

        Log.info(this.getClass(), method, "set timeout");
        retriableCloudServer.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        FATUtils.startServers(runner, retriableCloudServer);

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        Log.info(this.getClass(), method, "insertStaleLease returned: " + sb);

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when deleting server lease for server with identity cloud0011
        List<String> recoveredAlready = retriableCloudServer.findStringsInLogs("Have recovered from SQLException for server with recovery identity");
        // if not yet recovered, then wait for message
        if (recoveredAlready == null || recoveredAlready.isEmpty()) {
            assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException for server with recovery identity"));
        }

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "deleteStaleLease");

        Log.info(this.getClass(), method, "deleteStaleLease returned: " + sb);

        // cleanup HATable
        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "dropHATable");
        Log.info(this.getClass(), method, "dropHATable returned: " + sb);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Test
    public void testHADBLeaseGetFailover() throws Exception {
        final String method = "testHADBLeaseGetFailover";
        StringBuilder sb = null;

        FATUtils.startServers(runner, retriableCloudServer);

        Log.info(this.getClass(), method, "Call setupForLeaseGet");

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "setupForLeaseGet");

        Log.info(this.getClass(), method, "setupForLeaseGet returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver on " + retriableCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        // Ensure that the tables for a stale cloud server have been created
        // And set the com.ibm.ws.recoverylog.disablehomelogdeletion property for the stale server to ensure they survive shutdown.
        Log.info(this.getClass(), method, "call startserver for staleCloudServer");
        FATUtils.startServers(runner, staleCloudServer);
        Log.info(this.getClass(), method, "Call stopserver on " + staleCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, staleCloudServer);

        Log.info(this.getClass(), method, "set timeout");
        retriableCloudServer.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        FATUtils.startServers(runner, retriableCloudServer);

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        Log.info(this.getClass(), method, "insertStaleLease returned: " + sb);

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when deleting server lease for server with identity cloud0011
        List<String> recoveredAlready = retriableCloudServer.findStringsInLogs("Have recovered from SQLException when retrieving server leases");
        // if not yet recovered, then wait for message
        if (recoveredAlready == null || recoveredAlready.isEmpty()) {
            assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException when retrieving server leases"));
        }

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "deleteStaleLease");

        Log.info(this.getClass(), method, "deleteStaleLease returned: " + sb);

        // cleanup HATable
        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "dropHATable");
        Log.info(this.getClass(), method, "dropHATable returned: " + sb);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);
    }

    /**
     * Mimic situation where a server's logs have been deleted but a lease log entry remains.
     *
     * @throws Exception
     */
    @Test
    public void testLeaseDeletion() throws Exception {
        final String method = "testLeaseDeletion";
        if (!TxTestContainerSuite.isDerby()) { // Exclude Derby
            StringBuilder sb = null;

            FATUtils.startServers(runner, retriableCloudServer);

            Log.info(this.getClass(), method, "set timeout");
            retriableCloudServer.setServerStartTimeout(30000);

            // Tidy up any pre-existing tables
            sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "dropStaleRecoveryLogTables");
            Log.info(this.getClass(), method, "dropStaleRecoveryLogTables returned: " + sb);

            // Insert stale lease
            sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "insertStaleLease");
            Log.info(this.getClass(), method, "insertStaleLease returned: " + sb);

            // Wait for string that shows we attempted to peer recover "cloudstale"
            assertNotNull("peer recovery failed", retriableCloudServer
                            .waitForStringInTrace("Peer server cloudstale has missing recovery log SQL tables", LOG_SEARCH_TIMEOUT));

            FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);
        }
        Log.info(this.getClass(), method, "test complete");
    }

    @Test
    public void testBatchLeaseDeletion() throws Exception {
        final String method = "testBatchLeaseDeletion";
        if (!TxTestContainerSuite.isDerby()) { // Exclude Derby
            StringBuilder sb = null;

            FATUtils.startServers(runner, server1fastcheck);

            Log.info(this.getClass(), method, "set timeout");
            server1fastcheck.setServerStartTimeout(30000);

            // Insert stale leases
            sb = runTestWithResponse(server1fastcheck, SERVLET_NAME, "setupBatchOfStaleLeases1");
            Log.info(this.getClass(), method, "setupBatchOfStaleLeases1 returned: " + sb);
            server2fastcheck.useSecondaryHTTPPort();
            FATUtils.startServers(runner, server2fastcheck);

            Log.info(this.getClass(), method, "set timeout");
            server2fastcheck.setServerStartTimeout(30000);
            // Insert more stale leases
            sb = runTestWithResponse(server2fastcheck, SERVLET_NAME, "setupBatchOfStaleLeases2");
            Log.info(this.getClass(), method, "setupBatchOfStaleLeases1 returned: " + sb);

            // Check peer recovery attempts for dummy servers
            int server1Recoveries = 0;
            int server2Recoveries = 0;
            boolean foundThemAll = false;
            int searchAttempts = 0;
            while (!foundThemAll && searchAttempts < 60) {
                List<String> recoveredAlready1 = server1fastcheck.findStringsInLogs("has missing recovery log SQL tables");
                List<String> recoveredAlready2 = server2fastcheck.findStringsInLogs("has missing recovery log SQL tables");
                // Check number of recovery attempts
                if (recoveredAlready1 != null)
                    server1Recoveries = recoveredAlready1.size();
                if (recoveredAlready2 != null)
                    server2Recoveries = recoveredAlready2.size();
                if (server1Recoveries + server2Recoveries > 19)
                    foundThemAll = true;
                if (!foundThemAll) {
                    searchAttempts++;
                    Thread.sleep(1000 * 5);
                }
                Log.info(this.getClass(), method, "testBatchLeaseDeletion found " + server1Recoveries +
                                                  " in server1 logs and " + server2Recoveries + " in server2 logs");
            }
            if (!foundThemAll)
                fail("Did not attempt peer recovery for all servers");
            FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, server1fastcheck, server2fastcheck);
        }
        Log.info(this.getClass(), method, "test complete");
    }

    /**
     * Test aggressive takeover of recovery logs by a home server
     *
     * @throws Exception
     */
    @Test
    public void testAggressiveTakeover1() throws Exception {
        final String method = "testAggressiveTakeover1";
        if (!TxTestContainerSuite.isDerby()) { // Embedded Derby cannot support tests with concurrent server startup
            StringBuilder sb = null;

            FATUtils.startServers(runner, longLeaseCompeteServer1);

            Log.info(this.getClass(), method, "Call longLeaseCompeteServer1");

            sb = runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "setupForAggressivePeerRecovery1");

            Log.info(this.getClass(), method, "setupForAggressivePeerRecovery returned: " + sb);
            try {
                // We expect this to fail since it is gonna crash the server
                sb = runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "setupRec007");
            } catch (IOException e) {
            }
            Log.info(this.getClass(), method, "back from runTestWithResponse in testAggressiveTakeover1, sb is " + sb);

            // wait for 1st server to have gone away
            Log.info(this.getClass(), method, "wait for first server to go away in testDBBaseRecovery");
            assertNotNull(longLeaseCompeteServer1.getServerName() + " did not crash", longLeaseCompeteServer1.waitForStringInLog(XAResourceImpl.DUMP_STATE));
            longLeaseCompeteServer1.postStopServerArchive(); // must explicitly collect since crashed server
            // The server has been halted but its status variable won't have been reset because we crashed it. In order to
            // setup the server for a restart, set the server state manually.
            longLeaseCompeteServer1.setStarted(false);

            // Now start server2
            server2fastcheck.setHttpDefaultPort(9992);
            FATUtils.startServers(runner, server2fastcheck);

            // Now start server1
            FATUtils.startServers(runner, longLeaseCompeteServer1);

            // Server appears to have started ok. Check for key string to see whether recovery has succeeded, irrespective of what server2fastcheck has done
            assertNotNull("peer recovery failed", longLeaseCompeteServer1
                            .waitForStringInTrace("All persistent services have been directed to perform recovery processing for this WebSphere server", LOG_SEARCH_TIMEOUT));

            // Did server2 attempt peer recovery? Report only.
            List<String> theList = server2fastcheck
                            .findStringsInTrace("Server with identity cloud0021 attempted but failed to recover the logs of peer server cloud0011");
            if (theList.isEmpty())
                Log.info(this.getClass(), method, "Server2 was NOT interrupted when peer recovering server1");
            else
                Log.info(this.getClass(), method, "Server2 was interrupted when peer recovering server1");

            theList = server2fastcheck
                            .findStringsInTrace("Server with identity cloud0021 has recovered the logs of peer server cloud0011");
            if (theList.isEmpty())
                Log.info(this.getClass(), method, "Server2 did not peer recover server1");
            else
                Log.info(this.getClass(), method, "Server2 did peer recover server1");

            // cleanup HATable
            sb = runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "dropHATable");
            Log.info(this.getClass(), method, "dropHATable returned: " + sb);

            FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, server2fastcheck, longLeaseCompeteServer1);
        }
        Log.info(this.getClass(), method, "test complete");
    }

    /**
     * Test aggressive takeover of recovery logs by a home server. Take over at different point in server2's processing.
     *
     * @throws Exception
     */
    @Test
    public void testAggressiveTakeover2() throws Exception {
        final String method = "testAggressiveTakeover2";
        if (!TxTestContainerSuite.isDerby()) { // Embedded Derby cannot support tests with concurrent server startup
            StringBuilder sb = null;

            FATUtils.startServers(runner, longLeaseCompeteServer1);

            Log.info(this.getClass(), method, "Call longLeaseCompeteServer1");

            sb = runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "setupForAggressivePeerRecovery2");

            Log.info(this.getClass(), method, "setupForAggressivePeerRecovery returned: " + sb);
            try {
                // We expect this to fail since it is gonna crash the server
                sb = runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "setupRec007");
            } catch (IOException e) {
            }
            Log.info(this.getClass(), method, "back from runTestWithResponse in testAggressiveTakeover2, sb is " + sb);

            // wait for 1st server to have gone away
            Log.info(this.getClass(), method, "wait for first server to go away in testDBBaseRecovery");
            assertNotNull(longLeaseCompeteServer1.getServerName() + " did not crash", longLeaseCompeteServer1.waitForStringInLog(XAResourceImpl.DUMP_STATE));
            longLeaseCompeteServer1.postStopServerArchive(); // must explicitly collect since crashed server
            // The server has been halted but its status variable won't have been reset because we crashed it. In order to
            // setup the server for a restart, set the server state manually.
            longLeaseCompeteServer1.setStarted(false);

            // Now start server2
            server2fastcheck.setHttpDefaultPort(9992);
            FATUtils.startServers(runner, server2fastcheck);

            // Now start server1
            FATUtils.startServers(runner, longLeaseCompeteServer1);

            // Server appears to have started ok. Check for key string to see whether recovery has succeeded, irrespective of what server2fastcheck has done
            assertNotNull("peer recovery failed", longLeaseCompeteServer1
                            .waitForStringInTrace("All persistent services have been directed to perform recovery processing for this WebSphere server", LOG_SEARCH_TIMEOUT));

            // Did server2 attempt peer recovery? Report only.
            List<String> theList = server2fastcheck
                            .findStringsInTrace("Server with identity cloud0021 attempted but failed to recover the logs of peer server cloud0011");
            if (theList.isEmpty())
                Log.info(this.getClass(), method, "Server2 was NOT interrupted when peer recovering server1");
            else
                Log.info(this.getClass(), method, "Server2 was interrupted when peer recovering server1");

            theList = server2fastcheck
                            .findStringsInTrace("Server with identity cloud0021 has recovered the logs of peer server cloud0011");
            if (theList.isEmpty())
                Log.info(this.getClass(), method, "Server2 did not peer recover server1");
            else
                Log.info(this.getClass(), method, "Server2 did peer recover server1");

            // cleanup HATable
            sb = runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "dropHATable");
            Log.info(this.getClass(), method, "dropHATable returned: " + sb);

            FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, server2fastcheck, longLeaseCompeteServer1);
        }
        Log.info(this.getClass(), method, "test complete");
    }

}
