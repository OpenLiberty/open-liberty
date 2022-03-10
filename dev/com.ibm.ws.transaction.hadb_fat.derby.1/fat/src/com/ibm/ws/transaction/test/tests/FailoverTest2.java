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

import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Transaction;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.test.FATSuite;
import com.ibm.ws.transaction.web.FailoverServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

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
public class FailoverTest2 extends FailoverTest {

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

    @AfterClass
    public static void afterSuite() {
        FATSuite.afterSuite();
    }

    @BeforeClass
    public static void setUp() throws Exception {
        servers = new LibertyServer[] { defaultServer, recoverServer, retriableServer, nonRetriableServer, retriesServer };

        FailoverTest.commonSetUp();
    }

    @After
    public void cleanup() throws Exception {
        FailoverTest.commonCleanup();
    }

    /**
     * Run the same test as in testHADBNonRecoverableRuntimeFailover but against a server that has the retriableSqlCodes server.xml
     * entry set to include a sqlcode of "-3" so that the operation is retried.
     */
    @Test
    public void testHADBRetriableSqlCodeRuntimeFailover() throws Exception {
        final String method = "testHADBRetriableSqlCodeRuntimeFailover";

        FATUtils.startServers(runner, retriableServer);

        runInServletAndCheck(retriableServer, SERVLET_NAME, "setupForNonRecoverableFailover");

        FATUtils.stopServers(retriableServer);

        Log.info(this.getClass(), method, "set timeout");
        retriableServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, retriableServer);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(retriableServer, SERVLET_NAME, "driveTransactions");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", retriableServer.waitForStringInLog("Have recovered from SQLException when forcing SQL RecoveryLog"));

        FATUtils.stopServers(/* new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, */retriableServer);
    }

    /**
     * Run the same test as in testHADBRetriableSqlCodeRuntimeFailover but this time the simulated exception with sqlcode -3 is chained off
     * a simulated BatchUpdateException.
     */
    @Test
    public void testHADBRetriableSqlCodeBatchFailover() throws Exception {
        final String method = "testHADBRetriableSqlCodeBatchFailover";

        FATUtils.startServers(runner, retriableServer);

        Log.info(this.getClass(), method, "call setupForNonRecoverableBatchFailover");

        runInServletAndCheck(retriableServer, SERVLET_NAME, "setupForNonRecoverableBatchFailover");

        FATUtils.stopServers(retriableServer);

        Log.info(this.getClass(), method, "set timeout");
        retriableServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, retriableServer);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(retriableServer, SERVLET_NAME, "driveTransactions");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", retriableServer.waitForStringInLog("Have recovered from SQLException when forcing SQL RecoveryLog"));

        FATUtils.stopServers(retriableServer);
    }

    /**
     * Run the same test as in testHADBNonRecoverableRuntimeFailover but against a server that has the enableLogRetries server.xml
     * entry set to "true" so that non-transient (as well as transient) sqlcodes lead to an operation retry. EXCEPT we also
     * configure the nonRetriableSqlCodes server.xml entry set to include a sqlcode of "-3" so that this operation is NOT retried,
     * the transaction log is invalidated and the server shuts down
     *
     * sqlcode
     */
    @Test
    @ExpectedFFDC(value = { "javax.transaction.SystemException", "java.lang.Exception", "com.ibm.ws.recoverylog.spi.InternalLogException", })
    public void testHADBNonRetriableRuntimeFailover() throws Exception {
        final String method = "testHADBNonRetriableRuntimeFailover";

        FATUtils.startServers(runner, nonRetriableServer);

        runInServletAndCheck(nonRetriableServer, SERVLET_NAME, "setupForNonRecoverableFailover");

        FATUtils.stopServers(nonRetriableServer);

        Log.info(this.getClass(), method, "set timeout");
        nonRetriableServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, nonRetriableServer);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(nonRetriableServer, SERVLET_NAME, "driveTransactionsWithFailure");

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", nonRetriableServer.waitForStringInLog("Cannot recover from SQLException when forcing SQL RecoveryLog"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        FATUtils.stopServers(nonRetriableServer);

        FATUtils.startServers(runner, nonRetriableServer);

        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        assertNotNull("Recovery didn't happen for " + nonRetriableServer.getServerName(),
                      nonRetriableServer.waitForStringInTrace("Performed recovery for " + nonRetriableServer.getServerName()));

        FATUtils.stopServers(nonRetriableServer);
    }

    /**
     * Run the same test as in testHADBNonRetriableRuntimeFailover but this time the simulated exception with sqlcode -3 is chained off
     * a simulated BatchUpdateException.
     */
    @Test
    @ExpectedFFDC(value = { "javax.transaction.SystemException", "java.lang.Exception", "com.ibm.ws.recoverylog.spi.InternalLogException", })
    public void testHADBNonRetriableBatchFailover() throws Exception {
        final String method = "testHADBNonRetriableBatchFailover";

        FATUtils.startServers(runner, nonRetriableServer);

        runInServletAndCheck(nonRetriableServer, SERVLET_NAME, "setupForNonRecoverableBatchFailover");

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, nonRetriableServer);

        Log.info(this.getClass(), method, "set timeout");
        nonRetriableServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, nonRetriableServer);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(nonRetriableServer, SERVLET_NAME, "driveTransactionsWithFailure");

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", nonRetriableServer.waitForStringInLog("Cannot recover from SQLException when forcing SQL RecoveryLog"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        FATUtils.stopServers(new String[] { "WTRN0029E", "WTRN0066W", "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, nonRetriableServer);

        FATUtils.startServers(runner, nonRetriableServer);

        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        assertNotNull("Recovery didn't happen for " + nonRetriableServer.getServerName(),
                      nonRetriableServer.waitForStringInTrace("Performed recovery for " + nonRetriableServer.getServerName()));

        FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, nonRetriableServer);
    }

    /**
     * Run the same test as in testHADBNonRecoverableRuntimeFailover but against a server that has the enableLogRetries server.xml
     * entry set to "true" so that non-transient (as well as transient) sqlcodes lead to an operation retry.
     * sqlcode
     */
    @Test
    public void testHADBNewBehaviourRuntimeFailover() throws Exception {
        final String method = "testHADBNewBehaviourRuntimeFailover";

        FATUtils.startServers(runner, recoverServer);

        runInServletAndCheck(recoverServer, SERVLET_NAME, "setupForNonRecoverableFailover");

        FATUtils.stopServers(recoverServer);

        Log.info(this.getClass(), method, "set timeout");
        recoverServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, recoverServer);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(recoverServer, SERVLET_NAME, "driveTransactions");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", recoverServer.waitForStringInLog("Have recovered from SQLException when forcing SQL RecoveryLog"));

        FATUtils.stopServers(recoverServer);
    }

    /**
     * Run the same test as in testHADBNonRecoverableRuntimeFailover but against a server that has the enableLogRetries server.xml
     * entry set to "true" so that non-transient (as well as transient) sqlcodes lead to an operation retry. Then reconfigure
     * server.xml to have a set of nonRetriableSqlCodes including "-3". Drive more transactions and this time the server should fail.
     */
    @Test
    @ExpectedFFDC(value = { "javax.transaction.SystemException", "java.lang.Exception", "com.ibm.ws.recoverylog.spi.InternalLogException", })
    public void testHADBNewBehaviourUpdateConfigFailover() throws Exception {
        final String method = "testHADBNewBehaviourUpdateConfigFailover";

        FATUtils.startServers(runner, recoverServer);

        runInServletAndCheck(recoverServer, SERVLET_NAME, "setupForNonRecoverableFailover");

        FATUtils.stopServers(recoverServer);

        Log.info(this.getClass(), method, "set timeout");
        recoverServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, recoverServer);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(recoverServer, SERVLET_NAME, "driveTransactions");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", recoverServer.waitForStringInLog("Have recovered from SQLException when forcing SQL RecoveryLog"));

        // Tweak the config update timeout, it can be slooooooooooooooooooooow
        final int configUpdateTimeout = recoverServer.getConfigUpdateTimeout();
        if (configUpdateTimeout < 120000) {
            recoverServer.setConfigUpdateTimeout(120000);
        }

        // Update the server configuration on the fly to make the simulated sqlcode non-retriable.
        ServerConfiguration config = recoverServer.getServerConfiguration();
        Transaction tranConfig = config.getTransaction();
        Log.info(this.getClass(), method, "retrieved transaction config " + tranConfig);
        tranConfig.setNonRetriableSqlCodes("10100, 10200,-3,10900,99999 , 88888");
        recoverServer.setMarkToEndOfLog();
        recoverServer.updateServerConfiguration(config);
        recoverServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runInServletAndCheck(recoverServer, SERVLET_NAME, "setupForNonRecoverableFailover");

        FATUtils.stopServers(recoverServer);

        FATUtils.startServers(runner, recoverServer);

        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(recoverServer, SERVLET_NAME, "driveTransactionsWithFailure");

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", recoverServer.waitForStringInLog("Cannot recover from SQLException when forcing SQL RecoveryLog"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        FATUtils.stopServers(recoverServer);

        FATUtils.startServers(runner, recoverServer);

        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        assertNotNull("Recovery didn't happen for " + recoverServer.getServerName(), recoverServer.waitForStringInTrace("Performed recovery for " + recoverServer.getServerName()));

        // Reset the tran config to its initial state
        Log.info(this.getClass(), method, "Reset transaction config");
        tranConfig.setNonRetriableSqlCodes("");
        recoverServer.setMarkToEndOfLog();
        recoverServer.updateServerConfiguration(config);
        recoverServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        FATUtils.stopServers(recoverServer);
    }

    /**
     * Run the same test as in testHADBNonRecoverableRuntimeFailover and watch the server fail. Then reconfigure server.xml to
     * have a set of RetriableSqlCodes including "-3". Drive more transactions and this time the server should tolerate the SQLException.
     */
    @Test
    @ExpectedFFDC(value = { "com.ibm.ws.recoverylog.spi.InternalLogException", "javax.transaction.SystemException", "java.lang.Exception", })
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", })
    // Defect RTC171085 - an XAException may or may not be generated during
    // recovery, depending on the "speed" of the recovery relative to work
    // going on in the main thread. It is most sensible to make the potential
    // set of observable FFDCs allowable.
    public void testHADBUpdateConfigFailover() throws Exception {
        final String method = "testHADBUpdateConfigFailover";

        FATUtils.startServers(runner, defaultServer);

        runInServletAndCheck(defaultServer, SERVLET_NAME, "setupForNonRecoverableFailover");

        FATUtils.stopServers(/* new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, */defaultServer);

        Log.info(this.getClass(), method, "set timeout");
        defaultServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, defaultServer);

        Log.info(this.getClass(), method, "call driveTransactionsWithFailure");
        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(defaultServer, SERVLET_NAME, "driveTransactionsWithFailure");

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", defaultServer.waitForStringInLog("Cannot recover from SQLException when forcing SQL RecoveryLog"));

        // We need to tidy up the environment at this point.
        FATUtils.stopServers(/* new String[] { "WTRN0029E", "WTRN0066W", "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, */defaultServer);

        FATUtils.startServers(runner, defaultServer);

        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        assertNotNull("Recovery didn't happen for " + defaultServer.getServerName(), defaultServer.waitForStringInTrace("Performed recovery for " + defaultServer.getServerName()));

        runInServletAndCheck(defaultServer, SERVLET_NAME, "setupForNonRecoverableFailover");

        FATUtils.stopServers(/* new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, */defaultServer);

        FATUtils.startServers(runner, defaultServer);

        // Tweak the config update timeout, it can be slooooooooooooooooooooow
        final int configUpdateTimeout = defaultServer.getConfigUpdateTimeout();
        if (configUpdateTimeout < 120000) {
            defaultServer.setConfigUpdateTimeout(120000);
        }
        // Update the server configuration on the fly to make the simulated sqlcode retriable.
        ServerConfiguration config = defaultServer.getServerConfiguration();
        Transaction tranConfig = config.getTransaction();
        Log.info(this.getClass(), method, "retrieved transaction config " + tranConfig);
        tranConfig.setRetriableSqlCodes("10100, 10200,-3,10900,99999 , 88888");
        defaultServer.setMarkToEndOfLog();
        defaultServer.updateServerConfiguration(config);
        defaultServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        Log.info(this.getClass(), method, "call driveTransactions");
        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        runInServletAndCheck(defaultServer, SERVLET_NAME, "driveTransactions");

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", defaultServer.waitForStringInLog("Have recovered from SQLException when forcing SQL RecoveryLog"));

        // Reset the config back to the initial state.
        Log.info(this.getClass(), method, "Reset the transaction config");
        tranConfig.setRetriableSqlCodes("");
        defaultServer.setMarkToEndOfLog();
        defaultServer.updateServerConfiguration(config);
        defaultServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        FATUtils.stopServers(/* new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, */defaultServer);
    }

    /**
     * Simulate an unexpected sqlcode on the first attempt to connect to the database
     */
    @Test
    @ExpectedFFDC(value = { "com.ibm.ws.recoverylog.spi.InternalLogException", "java.sql.SQLException", })
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.transaction.SystemException", "java.sql.SQLRecoverableException", "java.lang.Exception",
                           "java.sql.SQLException",
    })
    public void testHADBConnectFailover() throws Exception {
        final String method = "testHADBConnectFailover";

        FATUtils.startServers(runner, defaultServer);

        runInServletAndCheck(defaultServer, SERVLET_NAME, "setupForConnectFailover");

        FATUtils.stopServers(/* new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, */defaultServer);

        Log.info(this.getClass(), method, "set timeout");
        defaultServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, defaultServer);

        // Should see a message like
        // WTRN0112E: An unexpected error occured whilst opening the recovery log. The log configuration was SQLMultiScopeRecoveryLog.......
        assertNotNull("No error message signifying log failure", defaultServer.waitForStringInLog("An unexpected error occured whilst opening the recovery log"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        FATUtils.stopServers(/* new String[] { "WTRN0112E", "WTRN0029E", "WTRN0066W", "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E" }, */defaultServer);
    }

    /**
     * Run the same test as in testHADBConnectFailover but against a server that has the enableLogRetries server.xml
     * entry set to "true" so that non-transient (as well as transient) sqlcodes lead to an operation retry.
     *
     * In the simulation a connect attempt will be successfully retried.
     */
    @Test
    @ExpectedFFDC(value = { "java.sql.SQLException", })
    public void testHADBNewBehaviourConnectFailover() throws Exception {
        final String method = "testHADBNewBehaviourConnectFailover";

        FATUtils.startServers(runner, recoverServer);

        runInServletAndCheck(recoverServer, SERVLET_NAME, "setupForConnectFailover");

        FATUtils.stopServers(recoverServer);

        Log.info(this.getClass(), method, "set timeout");
        recoverServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, recoverServer);

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", recoverServer.waitForStringInLog("Have recovered from SQLException when opening SQL RecoveryLog"));

        FATUtils.stopServers(recoverServer);
    }

    /**
     * Run the same test as in testHADBConnectFailover but against a server that has the enableLogRetries server.xml
     * entry set to "true" so that non-transient (as well as transient) sqlcodes lead to an operation retry.
     *
     * In the simulation a connect attempt will be successfully retried.
     */
    @Test
    @ExpectedFFDC(value = { "java.sql.SQLException", })
    public void testHADBNewBehaviourMultiConnectFailover() throws Exception {
        final String method = "testHADBNewBehaviourMultiConnectFailover";

        FATUtils.startServers(runner, recoverServer);
        Log.info(this.getClass(), method, "call testHADBNewBehaviourMultiConnectFailover");

        runInServletAndCheck(recoverServer, SERVLET_NAME, "setupForMultiConnectFailover");

        FATUtils.stopServers(recoverServer);

        Log.info(this.getClass(), method, "set timeout");
        recoverServer.setServerStartTimeout(START_TIMEOUT);

        FATUtils.startServers(runner, recoverServer);

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when opening SQL RecoveryLog
        assertNotNull("No warning message signifying failover", recoverServer.waitForStringInLog("Have recovered from SQLException when opening SQL RecoveryLog"));

        FATUtils.stopServers(recoverServer);
    }
}