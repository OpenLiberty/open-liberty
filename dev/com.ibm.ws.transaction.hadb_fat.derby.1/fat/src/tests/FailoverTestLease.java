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

import java.time.Duration;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import suite.FATSuite;

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

    @Server("com.ibm.ws.transaction_retriablecloud")
    public static LibertyServer retriableCloudServer;

    @Server("com.ibm.ws.transaction_stalecloud")
    public static LibertyServer staleCloudServer;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001.fastcheck")
    public static LibertyServer server1fastcheck;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002.fastcheck")
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
        FailoverTest.commonSetUp(FailoverTestLease.class);
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

    private LibertyServer[] serversToStop;

    @After
    public void cleanup() throws Exception {
        FATUtils.stopServers(serversToStop);
        FailoverTest.commonCleanup(this.getClass().getName());
        serversToStop = null;
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Test
    public void testHADBLeaseUpdateFailover() throws Exception {
        serversToStop = new LibertyServer[] { retriableCloudServer };

        FATUtils.startServers(runner, retriableCloudServer);

        runTest(retriableCloudServer, SERVLET_NAME, "setupForLeaseUpdate");

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        FATUtils.startServers(runner, retriableCloudServer);

        runTest(retriableCloudServer, SERVLET_NAME, "driveTransactions");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when updating server lease for server with identity cloud0011
        assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException when updating server lease"));
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Test
    public void testHADBLeaseDeleteFailover() throws Exception {
        serversToStop = new LibertyServer[] { retriableCloudServer, staleCloudServer };

        FATUtils.startServers(runner, retriableCloudServer);

        runTest(retriableCloudServer, SERVLET_NAME, "setupForLeaseDelete");

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        // Ensure that the tables for a stale cloud server have been created
        // And set the com.ibm.ws.recoverylog.disablehomelogdeletion property for the stale server to ensure they survive shutdown.
        FATUtils.startServers(runner, staleCloudServer);
        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, staleCloudServer);

        FATUtils.startServers(runner, retriableCloudServer);

        runTest(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when deleting server lease for server with identity cloud0011
        List<String> recoveredAlready = retriableCloudServer.findStringsInLogs("Have recovered from SQLException when deleting server lease");
        // if not yet recovered, then wait for message
        if (recoveredAlready == null || recoveredAlready.isEmpty()) {
            assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException when deleting server lease"));
        }

        runTest(retriableCloudServer, SERVLET_NAME, "deleteStaleLease");
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Test
    public void testHADBLeaseClaimFailover() throws Exception {
        serversToStop = new LibertyServer[] { retriableCloudServer, staleCloudServer };

        FATUtils.startServers(runner, retriableCloudServer);

        runTest(retriableCloudServer, SERVLET_NAME, "setupForLeaseClaim");

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        // Ensure that the tables for a stale cloud server have been created
        // And set the com.ibm.ws.recoverylog.disablehomelogdeletion property for the stale server to ensure they survive shutdown.
        FATUtils.startServers(runner, staleCloudServer);
        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, staleCloudServer);

        FATUtils.startServers(runner, retriableCloudServer);

        runTest(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when deleting server lease for server with identity cloud0011
        List<String> recoveredAlready = retriableCloudServer.findStringsInLogs("Have recovered from SQLException for server with recovery identity");
        // if not yet recovered, then wait for message
        if (recoveredAlready == null || recoveredAlready.isEmpty()) {
            assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException for server with recovery identity"));
        }

        runTest(retriableCloudServer, SERVLET_NAME, "deleteStaleLease");
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Test
    public void testHADBLeaseGetFailover() throws Exception {
        serversToStop = new LibertyServer[] { retriableCloudServer, staleCloudServer };

        FATUtils.startServers(runner, retriableCloudServer);

        runTest(retriableCloudServer, SERVLET_NAME, "setupForLeaseGet");

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        // Ensure that the tables for a stale cloud server have been created
        // And set the com.ibm.ws.recoverylog.disablehomelogdeletion property for the stale server to ensure they survive shutdown.
        FATUtils.startServers(runner, staleCloudServer);
        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, staleCloudServer);

        retriableCloudServer.setServerStartTimeout(30000);

        FATUtils.startServers(runner, retriableCloudServer);

        runTest(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when deleting server lease for server with identity cloud0011
        List<String> recoveredAlready = retriableCloudServer.findStringsInLogs("Have recovered from SQLException when retrieving server leases");
        // if not yet recovered, then wait for message
        if (recoveredAlready == null || recoveredAlready.isEmpty()) {
            assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException when retrieving server leases"));
        }

        runTest(retriableCloudServer, SERVLET_NAME, "deleteStaleLease");
    }

    /**
     * Mimic situation where a server's logs have been deleted but a lease log entry remains.
     *
     * @throws Exception
     */
    // This test should be moved to cloud_fat
    @Test
    public void testLeaseDeletion() throws Exception {
        serversToStop = new LibertyServer[] { retriableCloudServer };

        FATUtils.startServers(runner, retriableCloudServer);

        // Tidy up any pre-existing tables
        runTest(retriableCloudServer, SERVLET_NAME, "dropStaleRecoveryLogTables");

        // Insert stale lease
        runTest(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        // Wait for string that shows we attempted to peer recover "cloudstale"
        assertNotNull("peer recovery failed", retriableCloudServer
                        .waitForStringInTrace("Peer server cloudstale has missing recovery log SQL tables", LOG_SEARCH_TIMEOUT));

        String s;
        if (FATSuite.isSQLServer()) {
            s = "DELETE FROM WAS_LEASES_LOG WITH (ROWLOCK, UPDLOCK, HOLDLOCK) WHERE SERVER_IDENTITY='cloudstale'";
        } else {
            s = "DELETE FROM WAS_LEASES_LOG WHERE SERVER_IDENTITY='cloudstale'";
        }
        assertNotNull("Orphan lease not deleted", retriableCloudServer.waitForStringInTrace(s, LOG_SEARCH_TIMEOUT));
    }

    @Test
    // Can get a benign InternalLogException if heartbeat happens at same time as lease claim
    // This test should be moved to cloud_fat
    @AllowedFFDC(value = { "com.ibm.ws.recoverylog.spi.InternalLogException" })
    public void testBatchLeaseDeletion() throws Exception {
        if (!TxTestContainerSuite.isDerby()) { // Exclude Derby
            serversToStop = new LibertyServer[] { server1fastcheck, server2fastcheck };

            FATUtils.startServers(runner, server1fastcheck);

            server1fastcheck.setServerStartTimeout(30000);

            // Insert stale leases
            runTest(server1fastcheck, SERVLET_NAME, "setupBatchOfStaleLeases1");

            server2fastcheck.useSecondaryHTTPPort();
            FATUtils.startServers(runner, server2fastcheck);

            // Insert more stale leases
            runTest(server2fastcheck, SERVLET_NAME, "setupBatchOfStaleLeases2");

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
                    Thread.sleep(Duration.ofSeconds(5).toMillis());
                }
            }
            if (!foundThemAll)
                fail("Did not attempt peer recovery for all servers");
        }
    }
}