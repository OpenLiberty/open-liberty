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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.ConnectException;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.HADBTestConstants.HADBTestType;
import com.ibm.tx.jta.ut.util.HADBTestControl;
import com.ibm.tx.jta.ut.util.TxTestUtils;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
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
public class FailoverTest1 extends FailoverTest {
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = "transaction/FailoverServlet";

    @Server("com.ibm.ws.transaction")
    public static LibertyServer defaultServer;

    @Server("com.ibm.ws.transaction_multipleretries")
    public static LibertyServer retriesServer;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction",
                                                        "com.ibm.ws.transaction_multipleretries",
    };

    @AfterClass
    public static void afterSuite() {
        FATSuite.afterSuite("WAS_TRAN_LOG", "WAS_PARTNER_LOG");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        FailoverTest.commonSetUp(FailoverTest1.class);
    }

    // Test we get back the actual exception that scuppered the test
    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testGetDriverConnectionFailure() throws Exception {
        final String method = "testGetDriverConnectionFailure";

        server = defaultServer;
        serverMsgs = new String[] { "WTRN0112E", };

        try {
            HADBTestControl.write(HADBTestType.RUNTIME, -4498, 12, 1);

            server.setAdditionalSystemProperties(Collections.singletonMap(TxTestUtils.CONNECTION_MANAGER_FAILS, "1"));

            FATUtils.startServers(runner, server);

            StringBuilder sb = runInServlet(server, SERVLET_NAME, "driveTransactions");

            assertFalse("driveTransactions unexpectedly succeeded", sb.toString().contains(SUCCESS)); // Log should be closed due to Connection failure
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof ConnectException) {
                if (!cause.getMessage().startsWith("Scuppering")) {
                    throw e;
                }
            } else {
                throw e;
            }
        } finally {
            server.setAdditionalSystemProperties(Collections.singletonMap(TxTestUtils.CONNECTION_MANAGER_FAILS, "0"));
        }
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Test
    public void testHADBRecoverableRuntimeFailover() throws Exception {
        final String method = "testHADBRecoverableRuntimeFailover";

        server = defaultServer;

        HADBTestControl.write(HADBTestType.RUNTIME, -4498, 12, 1);

        FATUtils.startServers(runner, server);

        runInServletAndCheck(server, SERVLET_NAME, "driveTransactions");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", server.waitForStringInLog("Have recovered from SQLException when forcing SQL RecoveryLog"));

        FATUtils.stopServers(server);
    }

    /**
     * Test the setting of the "logRetryLimit" server.xml attribute. Set the parameter to 3 retries but simulate
     * more than 3 (actually 5) failures in the test.
     */
    @Test
    @ExpectedFFDC(value = { "javax.transaction.SystemException", "java.lang.Exception", "com.ibm.ws.recoverylog.spi.InternalLogException", })
    public void testHADBRecoverableFailureMultipleRetries() throws Exception {
        final String method = "testHADBRecoverableFailureMultipleRetries";

        server = retriesServer;

        HADBTestControl.write(HADBTestType.RUNTIME, -4498, 12, 5); // Can fail up to 5 times

        FATUtils.startServers(runner, server);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(server, SERVLET_NAME, "driveTransactionsWithFailure");

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", server.waitForStringInLog("Cannot recover from SQLException when forcing SQL RecoveryLog"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        FATUtils.stopServers(server);

        FATUtils.startServers(runner, retriesServer);

        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        assertNotNull("Recovery didn't happen for " + server.getServerName(), server.waitForStringInTrace("Performed recovery for " + server.getServerName()));
    }

    /**
     * Run a set of transactions and simulate an unexpected sqlcode
     */
    @Test
    @AllowedFFDC(value = { "javax.transaction.SystemException", "java.lang.Exception", "com.ibm.ws.recoverylog.spi.InternalLogException", })
    // Defect RTC171085 - an XAException may or may not be generated during
    // recovery, depending on the "speed" of the recovery relative to work
    // going on in the main thread. It is most sensible to make the potential
    // set of observable FFDCs allowable.
    public void testHADBNonRecoverableRuntimeFailover() throws Exception {
        final String method = "testHADBNonRecoverableRuntimeFailover";

        server = defaultServer;

        HADBTestControl.write(HADBTestType.RUNTIME, -3, 12, 1);

        FATUtils.startServers(runner, server);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(server, SERVLET_NAME, "driveTransactionsWithFailure");

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", server.waitForStringInLog("Cannot recover from SQLException when forcing SQL RecoveryLog"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        FATUtils.stopServers(server);

        FATUtils.startServers(runner, server);

        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        assertNotNull("Recovery didn't happen for " + server.getServerName(), server.waitForStringInTrace("Performed recovery for " + server.getServerName()));
    }

    /**
     * Simulate an HA condition at server start (testing log open error
     * handling))
     */
    @Test
    public void testHADBRecoverableStartupFailover() throws Exception {
        final String method = "testHADBRecoverableStartupFailover";

        server = defaultServer;

        HADBTestControl.write(HADBTestType.STARTUP, -4498, 11, 1);

        FATUtils.startServers(runner, server);

        runInServletAndCheck(server, SERVLET_NAME, "driveTransactions");
    }

    @Test
    @ExpectedFFDC(value = { "com.ibm.ws.recoverylog.spi.InternalLogException", "java.lang.IllegalStateException" })
    public void testHADBNonRecoverableStartupFailover() throws Exception {
        final String method = "testHADBNonRecoverableStartupFailover";

        server = defaultServer;
        serverMsgs = new String[] { "WTRN0107W", "WTRN0000E", "WTRN0112E", "WTRN0153W" };

        HADBTestControl.write(HADBTestType.STARTUP, -3, 11, 1);

        FATUtils.startServers(runner, server);
        StringBuilder sb = runInServlet(server, SERVLET_NAME, "driveTransactions");

        assertFalse("driveTransactions unexpectedly succeeded", sb.toString().contains(SUCCESS)); // Log should be closed
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException", })
    public void testHADBEarlyNonRecoverableStartupFailover() throws Exception {
        final String method = "testHADBEarlyNonRecoverableStartupFailover";

        server = defaultServer;
        serverMsgs = new String[] { "WTRN0107W", "WTRN0000E", "WTRN0112E", "WTRN0153W" };

        HADBTestControl.write(HADBTestType.STARTUP, -3, 1, 1);

        FATUtils.startServers(runner, server);
        StringBuilder sb = runInServlet(server, SERVLET_NAME, "driveTransactions");

        assertFalse("driveTransactions unexpectedly succeeded", sb.toString().contains(SUCCESS)); // Log should be closed
    }

    /**
     * Inject duplicate recovery log entries into a database log, crash the server and test
     * that recovery succeeds.
     *
     */
    @Test
    public void testDuplicationInRecoveryLogsRestart() throws Exception {
        final String method = "testDuplicationInRecoveryLogsRestart";

        server = defaultServer;

        HADBTestControl.write(HADBTestType.DUPLICATE_RESTART, 0, 10, 1);

        FATUtils.startServers(runner, server);

        Log.info(this.getClass(), method, "call driveTransactions");
        try {
            runTestWithResponse(server, SERVLET_NAME, "driveTransactions");
            fail("driveTransactions did not throw an Exception");
        } catch (Exception e) {
            // Halting the server generates a java.net.SocketException
            Log.info(this.getClass(), method, "driveTransactions caught exception " + e);
        }

        // Server should have halted, check for message
        assertNotNull("Server has not been halted", server.waitForStringInTrace("duplicateAndHalt, now HALT", FATUtils.LOG_SEARCH_TIMEOUT));

        server.postStopServerArchive(); // must explicitly collect since server start failed

        // The server has been halted but its status variable won't have been reset because we crashed it. In order to
        // setup the server for a restart, set the server state manually.
        server.setStarted(false);

        FATUtils.startServers(runner, server);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        assertNotNull("No evidence of duplication", server.waitForStringInTrace("NMTEST: Replacing item", FATUtils.LOG_SEARCH_TIMEOUT));
    }

    /**
     * Drive a set of transactions and inject duplicate recovery log entries into a database log, check that the duplicate records are
     * in the database, drive more transactions and check that the duplicate records have been deleted by the runtime.
     *
     */
    @Test
    public void testDuplicationInRecoveryLogsRuntime() throws Exception {
        final String method = "testDuplicationInRecoveryLogsRuntime";

        server = defaultServer;

        HADBTestControl.write(HADBTestType.DUPLICATE_RUNTIME, 0, 10, 1);

        FATUtils.startServers(runner, server);

        runInServletAndCheck(server, SERVLET_NAME, "checkForDuplicates");

        List<String> lines = server.findStringsInLogs("SQL TRANLOG: Found DUPLICATE row");
        assertFalse("Unexpectedly found duplicates on startup", lines.size() > 0);

        // The processing here, should result (through our fake jdbc driver) in duplicate rows being inserted into the recovery logs
        //
        // The fake jdbc driver inserts those duplicate rows through the IfxPreparedStatement.duplicateAndHalt() method. The jdbc driver
        // knows that it should be inserting these duplicates because we configured the HATable in the FailOverServlet.setupForDuplicationRuntime()
        // method
        boolean retry = true;
        int attempts = 0;
        while (retry && attempts <= 1) {
            if (attempts == 1)
                Log.info(this.getClass(), method, "Retry the duplicate process");
            runInServletAndCheck(server, SERVLET_NAME, "driveSixTransactions");

            // The FailOverServlet.checkForDuplicates() method drives a query directly against the WAS_TRAN_LOG table in order to determine
            // the presence of duplicate rows. If it finds a duplicate row it will write "SQL TRANLOG: Found DUPLICATE row" to the logs.
            Log.info(this.getClass(), method, "call checkForDuplicates");
            StringBuilder sb = runTestWithResponse(server, SERVLET_NAME, "checkForDuplicates");
            assertTrue("checkForDuplicates did not return " + SUCCESS + ". Returned: " + sb.toString(), sb.toString().contains(SUCCESS));

            lines = server.findStringsInLogs("SQL TRANLOG: Found DUPLICATE row");
            if (lines.size() > 0)
                retry = false;

            attempts++;
        }

        Assert.assertTrue("Unexpectedly found no duplicates", lines.size() > 0);
        int numDups = lines.size();

        runInServletAndCheck(server, SERVLET_NAME, "driveSixTransactions");

        runInServletAndCheck(server, SERVLET_NAME, "checkForDuplicates");

        lines = server.findStringsInLogs("SQL TRANLOG: Found DUPLICATE row");
        assertFalse("Unexpectedly found duplicates on test completion", lines.size() > numDups);
    }

    /**
     * This is normal recovery processing. We want to check theat there are no duplicate rows in "normal" recovery processing.
     * Crash the server and check that recovery succeeds and that there are no duplicate records in the log.
     *
     */
    @Test
    public void testAbsenceOfDuplicatesInRecoveryLogs() throws Exception {
        final String method = "testAbsenceOfDuplicatesInRecoveryLogs";
        StringBuilder sb = null;

        server = defaultServer;

        HADBTestControl.write(HADBTestType.HALT, 0, 12, 1); // set the ioperation to the duplicate test value + 2

        FATUtils.startServers(runner, server);

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "driveTransactions");
            fail("driveTransactions did not throw an Exception");
        } catch (Exception e) {
            // Halting the server generates a java.net.SocketException
            Log.info(this.getClass(), method, "driveTransactions caught exception " + e);
        }

        // Server should have halted, check for message
        assertNotNull("Server has not been halted", server.waitForStringInTrace("Now HALT", FATUtils.LOG_SEARCH_TIMEOUT));

        server.postStopServerArchive(); // must explicitly collect since server start failed

        // The server has been halted but its status variable won't have been reset because we crashed it. In order to
        // setup the server for a restart, set the server state manually.
        server.setStarted(false);

        FATUtils.startServers(runner, server);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        assertNotNull("Recovery didn't happen for " + server.getServerName(), server.waitForStringInTrace("Performed recovery for " + server.getServerName()));

        // Check that there were no duplicates
        List<String> lines = server.findStringsInLogs("NMTEST: Replacing item");
        assertFalse("Unexpectedly found duplicates on recovery", lines.size() > 0);
    }
}