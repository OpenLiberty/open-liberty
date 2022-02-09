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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
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
@AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", })
public class FailoverTest1 extends FATServletClient {
    private static final int LOG_SEARCH_TIMEOUT = 300000;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = "transaction/FailoverServlet";

    @Server("com.ibm.ws.transaction")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer defaultServer;

    @Server("com.ibm.ws.transaction_recover")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer recoverServer;

    @Server("com.ibm.ws.transaction_retriable")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer retriableServer;

    @Server("com.ibm.ws.transaction_nonretriable")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer nonRetriableServer;

    @Server("com.ibm.ws.transaction_multipleretries")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer retriesServer;

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.beforeSuite();

        ShrinkHelper.defaultApp(defaultServer, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(recoverServer, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(retriableServer, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(nonRetriableServer, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(retriesServer, APP_NAME, "com.ibm.ws.transaction.*");
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
        defaultServer.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

        // Remove tranlog DB
        defaultServer.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Mode(TestMode.LITE)
    @Test
    public void testHADBRecoverableRuntimeFailover() throws Exception {
        final String method = "testHADBRecoverableRuntimeFailover";
        StringBuilder sb = null;
        FATUtils.startServers(runner, defaultServer);

        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "setupForRecoverableFailover");
        assertTrue("setupForRecoverableFailover did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);

        Log.info(this.getClass(), method, "set timeout");
        defaultServer.setServerStartTimeout(30000);

        FATUtils.startServers(runner, defaultServer);

        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "driveTransactions");
        assertTrue("driveTransactions did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", defaultServer.waitForStringInLog("Have recovered from SQLException when forcing SQL RecoveryLog"));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);
    }

    /**
     * Test the setting of the "logRetryLimit" server.xml attribute. Set the parameter to 3 retries but simulate
     * more than 3 (actually 5) failures in the test.
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.transaction.SystemException", "java.sql.SQLRecoverableException", "java.lang.Exception",
    })
    public void testHADBRecoverableFailureMultipleRetries() throws Exception {
        final String method = "testHADBRecoverableFailureMultipleRetries";
        StringBuilder sb = null;

        FATUtils.startServers(runner, retriesServer);

        sb = runTestWithResponse(retriesServer, SERVLET_NAME, "setupForRecoverableFailureMultipleRetries");
        assertTrue("setupForRecoverableFailureMultipleRetries did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriesServer);

        Log.info(this.getClass(), method, "set timeout");
        retriesServer.setServerStartTimeout(30000);

        FATUtils.startServers(runner, retriesServer);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        sb = runTestWithResponse(retriesServer, SERVLET_NAME, "driveTransactionsWithFailure");
        assertTrue("driveTransactionsWithFailure did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", retriesServer.waitForStringInLog("Cannot recover from SQLException when forcing SQL RecoveryLog"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        FATUtils.stopServers(new String[] { "WTRN0029E", "WTRN0066W", "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriesServer);

        FATUtils.startServers(runner, retriesServer);

        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        assertNotNull("Recovery didn't happen for " + retriesServer.getServerName(), retriesServer.waitForStringInTrace("Performed recovery for " + retriesServer.getServerName()));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, retriesServer);
    }

    /**
     * Run a set of transactions and simulate an unexpected sqlcode
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.transaction.SystemException", "java.sql.SQLRecoverableException", "java.lang.Exception",
    })

    // Defect RTC171085 - an XAException may or may not be generated during
    // recovery, depending on the "speed" of the recovery relative to work
    // going on in the main thread. It is most sensible to make the potential
    // set of observable FFDCs allowable.
    public void testHADBNonRecoverableRuntimeFailover() throws Exception {
        final String method = "testHADBNonRecoverableRuntimeFailover";
        StringBuilder sb = null;

        FATUtils.startServers(runner, defaultServer);

        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "setupForNonRecoverableFailover");
        assertTrue("setupForNonRecoverableFailover did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);

        Log.info(this.getClass(), method, "set timeout");
        defaultServer.setServerStartTimeout(30000);

        FATUtils.startServers(runner, defaultServer);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "driveTransactionsWithFailure");
        assertTrue("driveTransactionsWithFailure did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", defaultServer.waitForStringInLog("Cannot recover from SQLException when forcing SQL RecoveryLog"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        FATUtils.stopServers(new String[] { "WTRN0029E", "WTRN0066W", "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);

        FATUtils.startServers(runner, defaultServer);

        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        assertNotNull("Recovery didn't happen for " + defaultServer.getServerName(), defaultServer.waitForStringInTrace("Performed recovery for " + defaultServer.getServerName()));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);
    }

    /**
     * Simulate an HA condition at server start (testing log open error
     * handling))
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", })
    public void testHADBRecoverableStartupFailover() throws Exception {
        final String method = "testHADBRecoverableStartupFailover";
        StringBuilder sb = null;

        FATUtils.startServers(runner, defaultServer);

        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "setupForStartupFailover");
        assertTrue("setupForStartupFailover did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);

        Log.info(this.getClass(), method, "set timeout");
        defaultServer.setServerStartTimeout(30000);

        FATUtils.startServers(runner, defaultServer);

        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "driveTransactions");
        assertTrue("driveTransactions did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);
    }

    /**
     * Inject duplicate recovery log entries into a database log, crash the server and test
     * that recovery succeeds.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", })
    public void testDuplicationInRecoveryLogsRestart() throws Exception {
        final String method = "testDuplicationInRecoveryLogsRestart";
        StringBuilder sb = null;

        FATUtils.startServers(runner, defaultServer);

        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "setupForDuplicationRestart");
        assertTrue("setupForDuplicationRestart did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);

        Log.info(this.getClass(), method, "set timeout");
        defaultServer.setServerStartTimeout(30000);

        FATUtils.startServers(runner, defaultServer);

        Log.info(this.getClass(), method, "call driveTransactions");
        try {
            sb = runTestWithResponse(defaultServer, SERVLET_NAME, "driveTransactions");
            fail("driveTransactions did not throw an Exception");
        } catch (Exception e) {
            // Halting the server generates a java.net.SocketException
            Log.info(this.getClass(), method, "driveTransactions caught exception " + e);
        }

        // Server should have halted, check for message
        assertNotNull("Server has not been halted", defaultServer.waitForStringInTrace("duplicateAndHalt, now HALT", LOG_SEARCH_TIMEOUT));

        defaultServer.postStopServerArchive(); // must explicitly collect since server start failed

        // The server has been halted but its status variable won't have been reset because we crashed it. In order to
        // setup the server for a restart, set the server state manually.
        defaultServer.setStarted(false);

        FATUtils.startServers(runner, defaultServer);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        assertNotNull("No evidence of duplication", defaultServer.waitForStringInTrace("NMTEST: Replacing item", LOG_SEARCH_TIMEOUT));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);
    }

    /**
     * Drive a set of transactions and inject duplicate recovery log entries into a database log, check that the duplicate records are
     * in the database, drive more transactions and check that the duplicate records have been deleted by the runtime.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", })
    public void testDuplicationInRecoveryLogsRuntime() throws Exception {
        final String method = "testDuplicationInRecoveryLogsRuntime";
        StringBuilder sb = null;

        FATUtils.startServers(runner, defaultServer);

        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "setupForDuplicationRuntime");
        assertTrue("setupForDuplicationRuntime did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);

        Log.info(this.getClass(), method, "set timeout");
        defaultServer.setServerStartTimeout(30000);

        FATUtils.startServers(runner, defaultServer);

        Log.info(this.getClass(), method, "call checkForDuplicates");
        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "checkForDuplicates");
        assertTrue("checkForDuplicates did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        List<String> lines = defaultServer.findStringsInLogs("SQL TRANLOG: Found DUPLICATE row");
        assertFalse("Unexpectedly found duplicates on startup", lines.size() > 0);

        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "driveSixTransactions");
        assertTrue("driveSixTransactions did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        Log.info(this.getClass(), method, "call checkForDuplicates");
        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "checkForDuplicates");
        assertTrue("checkForDuplicates did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        lines = defaultServer.findStringsInLogs("SQL TRANLOG: Found DUPLICATE row");
        Assert.assertTrue("Unexpectedly found no duplicates", lines.size() > 0);
        int numDups = lines.size();

        Log.info(this.getClass(), method, "call driveSixTransactions");
        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "driveSixTransactions");
        assertTrue("driveSixTransactions did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        Log.info(this.getClass(), method, "call checkForDuplicates");
        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "checkForDuplicates");
        assertTrue("checkForDuplicates did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        lines = defaultServer.findStringsInLogs("SQL TRANLOG: Found DUPLICATE row");
        assertFalse("Unexpectedly found duplicates on test completion", lines.size() > numDups);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);
    }

    /**
     * Inject duplicate recovery log entries into a database log, crash the server and test
     * that recovery succeeds.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", })
    public void testAbsenceOfDuplicatesInRecoveryLogs() throws Exception {
        final String method = "testAbsenceOfDuplicatesInRecoveryLogs";
        StringBuilder sb = null;

        FATUtils.startServers(runner, defaultServer);

        sb = runTestWithResponse(defaultServer, SERVLET_NAME, "setupForHalt");
        assertTrue("setupForHalt did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);

        Log.info(this.getClass(), method, "set timeout");
        defaultServer.setServerStartTimeout(30000);

        FATUtils.startServers(runner, defaultServer);

        try {
            sb = runTestWithResponse(defaultServer, SERVLET_NAME, "driveTransactions");
            fail("driveTransactions did not throw an Exception");
        } catch (Exception e) {
            // Halting the server generates a java.net.SocketException
            Log.info(this.getClass(), method, "driveTransactions caught exception " + e);
        }

        // Server should have halted, check for message
        assertNotNull("Server has not been halted", defaultServer.waitForStringInTrace("Now HALT", LOG_SEARCH_TIMEOUT));

        Log.info(this.getClass(), method, "Call postStopServerArchive");
        defaultServer.postStopServerArchive(); // must explicitly collect since server start failed

        // The server has been halted but its status variable won't have been reset because we crashed it. In order to
        // setup the server for a restart, set the server state manually.
        defaultServer.setStarted(false);

        FATUtils.startServers(runner, defaultServer);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        assertNotNull("Recovery didn't happen for " + defaultServer.getServerName(), defaultServer.waitForStringInTrace("Performed recovery for " + defaultServer.getServerName()));

        // Check that there were no duplicates
        List<String> lines = defaultServer.findStringsInLogs("NMTEST: Replacing item");
        assertFalse("Unexpectedly found duplicates on recovery", lines.size() > 0);

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, defaultServer);
    }
}