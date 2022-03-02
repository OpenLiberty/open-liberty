/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.test.FATSuite;
import com.ibm.ws.transaction.web.FailoverServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

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
@Mode
@RunWith(FATRunner.class)
public class FailoverTestLease extends FATServletClient {
    private static final int LOG_SEARCH_TIMEOUT = 300000;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = "transaction/FailoverServlet";

    @Server("com.ibm.ws.transaction_retriablecloud")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer retriableCloudServer;

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.beforeSuite();

        ShrinkHelper.defaultApp(retriableCloudServer, APP_NAME, "com.ibm.ws.transaction.*");
    }

    @AfterClass
    public static void afterSuite() {
        FATSuite.afterSuite();
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

        // Clean up XA resource files
        retriableCloudServer.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

        // Remove tranlog DB
        retriableCloudServer.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException" })
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
        Log.info(this.getClass(), method, "call stopserver");
        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);
        Log.info(this.getClass(), method, "Complete");
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException" })
    public void testHADBLeaseDeleteFailover() throws Exception {
        final String method = "testHADBLeaseDeleteFailover";
        StringBuilder sb = null;
        FATUtils.startServers(runner, retriableCloudServer);
        Log.info(this.getClass(), method, "Call setupForLeaseDelete");

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "setupForLeaseDelete");

        Log.info(this.getClass(), method, "setupForLeaseDelete returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver on " + retriableCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        Log.info(this.getClass(), method, "set timeout");
        retriableCloudServer.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        FATUtils.startServers(runner, retriableCloudServer);

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        Log.info(this.getClass(), method, "insertStaleLease returned: " + sb);

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when deleting server lease for server with identity cloud0011
        assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException when deleting server lease"));

        Log.info(this.getClass(), method, "call stopserver");
        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        Log.info(this.getClass(), method, "Complete");
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException" })
    public void testHADBLeaseClaimFailover() throws Exception {
        final String method = "testHADBLeaseClaimFailover";
        StringBuilder sb = null;
        FATUtils.startServers(runner, retriableCloudServer);
        Log.info(this.getClass(), method, "Call setupForLeaseClaim");

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "setupForLeaseClaim");

        Log.info(this.getClass(), method, "setupForLeaseClaim returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver on " + retriableCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        Log.info(this.getClass(), method, "set timeout");
        retriableCloudServer.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        FATUtils.startServers(runner, retriableCloudServer);

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        Log.info(this.getClass(), method, "insertStaleLease returned: " + sb);

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when deleting server lease for server with identity cloud0011
        assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException for server with recovery identity"));

        Log.info(this.getClass(), method, "call stopserver");
        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        Log.info(this.getClass(), method, "Complete");
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException" })
    public void testHADBLeaseGetFailover() throws Exception {
        final String method = "testHADBLeaseGetFailover";
        StringBuilder sb = null;
        FATUtils.startServers(runner, retriableCloudServer);
        Log.info(this.getClass(), method, "Call setupForLeaseGet");

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "setupForLeaseGet");

        Log.info(this.getClass(), method, "setupForLeaseGet returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver on " + retriableCloudServer);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        Log.info(this.getClass(), method, "set timeout");
        retriableCloudServer.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        FATUtils.startServers(runner, retriableCloudServer);

        sb = runTestWithResponse(retriableCloudServer, SERVLET_NAME, "insertStaleLease");

        Log.info(this.getClass(), method, "insertStaleLease returned: " + sb);

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when deleting server lease for server with identity cloud0011
        assertNotNull("No warning message signifying failover", retriableCloudServer.waitForStringInLog("Have recovered from SQLException when retrieving server leases"));

        Log.info(this.getClass(), method, "call stopserver");
        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriableCloudServer);

        Log.info(this.getClass(), method, "Complete");
    }
}