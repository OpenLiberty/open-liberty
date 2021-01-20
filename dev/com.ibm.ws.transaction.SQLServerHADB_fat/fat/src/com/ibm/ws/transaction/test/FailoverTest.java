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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.containers.MSSQLServerContainer;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.FailoverServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
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
 * named ifxjdbc.jar. This is inferred to be an Informix driver by the Liberty JDBC driver code.
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
 * ->The tests will drive a batch of 2PC transactions using artificial XAResourceImpl resources. The jdbc driver will
 * generate a SQLException at a point defined in the HATABLE row.
 */
@Mode
@RunWith(FATRunner.class)
public class FailoverTest extends FATServletClient {
    private static final int LOG_SEARCH_TIMEOUT = 300000;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = "transaction/FailoverServlet";

    @ClassRule
    public static MSSQLServerContainer<?> sqlserver = FATSuite.sqlserver;

    @Server("com.ibm.ws.transaction")
    @TestServlet(servlet = FailoverServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.*");
    }

    public static void setUp(LibertyServer server) throws Exception {

        String dbName = "test";
        initDatabase(sqlserver, dbName);

        server.addEnvVar("DBNAME", dbName);
        server.addEnvVar("HOST", sqlserver.getContainerIpAddress());
        Log.info(FailoverTest.class, "setUp", "host - " + sqlserver.getContainerIpAddress());
        server.addEnvVar("PORT", Integer.toString(sqlserver.getFirstMappedPort()));
        Log.info(FailoverTest.class, "setUp", "port - " + sqlserver.getFirstMappedPort());
        server.addEnvVar("USER", sqlserver.getUsername());
        Log.info(FailoverTest.class, "setUp", "user - " + sqlserver.getUsername());
        server.addEnvVar("PASSWORD", sqlserver.getPassword());
        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
    }

    //Helper method
    private static void initDatabase(JdbcDatabaseContainer<?> cont, String dbName) throws NoDriverFoundException, SQLException {
        // Create Database
        try (Connection conn = cont.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE [" + dbName + "];");
            stmt.close();
        } catch (SQLException sqlex) {
            // Specifically don't barf if the db exists already
            if (sqlex.getErrorCode() == 1801) {
                Log.info(FailoverTest.class, "initDatabase", "database exists already");
            } else {
                Log.info(FailoverTest.class, "initDatabase", "create database returned sqlexception: " + sqlex);
                Log.info(FailoverTest.class, "initDatabase", "Message: " + sqlex.getMessage());
                Log.info(FailoverTest.class, "initDatabase", "SQLSTATE: " + sqlex.getSQLState());
                Log.info(FailoverTest.class, "initDatabase", "Error code: " + sqlex.getErrorCode());
                // rethrow exception
                throw sqlex;
            }
        }

        //Setup distributed connection.
        try (Connection conn = cont.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("EXEC sp_sqljdbc_xa_install");
            stmt.close();
        }
    }

    @Before
    public void before() throws Exception {
        startServers(server);
    }

    @After
    public void cleanup() throws Exception {

        server.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E");

        // Clean up XA resource files
        server.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

        // Remove tranlog DB
        server.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Mode(TestMode.LITE)
    @Test
    public void testHADBRecoverableRuntimeFailover() throws Exception {
        final String method = "testHADBRecoverableRuntimeFailover";
        StringBuilder sb = null;

        Log.info(this.getClass(), method, "Call setupForRecoverableFailover");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "setupForRecoverableFailover");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setupForRecoverableFailover returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver");

        server.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E");

        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        startServers(server);

        Log.info(this.getClass(), method, "Call driveTransactions");
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "driveTransactions");
        } catch (Throwable e) {
        }

        // Should see a message like
        // WTRN0108I: Have recovered from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No warning message signifying failover", server.waitForStringInLog("Have recovered from SQLException"));

        Log.info(this.getClass(), method, "Complete");
    }

    /**
     * Run a set of transactions and simulate an unexpected sqlcode
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.transaction.SystemException", "java.sql.SQLRecoverableException", "java.lang.Exception"
    })

    // Defect RTC171085 - an XAException may or may not be generated during
    // recovery, depending on the "speed" of the recovery relative to work
    // going on in the main thread. It is most sensible to make the potential
    // set of observable FFDCs allowable.
    public void testHADBNonRecoverableRuntimeFailover() throws Exception {
        final String method = "testHADBNonRecoverableRuntimeFailover";
        StringBuilder sb = null;

        Log.info(this.getClass(), method, "call setupForNonRecoverableFailover");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "setupForNonRecoverableFailover");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "call stopserver");
        server.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E");
        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);
        Log.info(this.getClass(), method, "call startserver");
        startServers(server);

        Log.info(this.getClass(), method, "complete");
        Log.info(this.getClass(), method, "call driveTransactionsWithFailure");
        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "driveTransactionsWithFailure");
        } catch (Throwable e) {
        }

        // Should see a message like
        // WTRN0100E: Cannot recover from SQLException when forcing SQL RecoveryLog tranlog for server com.ibm.ws.transaction
        assertNotNull("No error message signifying log failure", server.waitForStringInLog("Cannot recover from SQLException"));

        // We need to tidy up the environment at this point. We cannot guarantee
        // test order, so we should ensure
        // that we do any necessary recovery at this point
        Log.info(this.getClass(), method, "call stopserver");
        server.stopServer("WTRN0029E", "WTRN0066W", "WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E");
        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);
        Log.info(this.getClass(), method, "call startserver");
        startServers(server);
        Log.info(this.getClass(), method, "call testControlSetup");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testControlSetup");
        } catch (Throwable e) {
        }
        server.waitForStringInLog("testControlSetup complete");
        // RTC defect 170741
        // Wait for recovery to be driven - this may suffer from a delay (see
        // RTC 169082), so wait until the "recover("
        // string appears in the messages.log
        server.waitForStringInLog("recover\\(");
    }

    /**
     * Simulate an HA condition at server start (testing log open error
     * handling))
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHADBRecoverableStartupFailover() throws Exception {
        final String method = "testHADBRecoverableStartupFailover";
        StringBuilder sb = null;

        Log.info(this.getClass(), method, "call setupForStartupFailover");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "setupForStartupFailover");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "call stopserver");
        server.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E");
        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);
        Log.info(this.getClass(), method, "call startserver");
        startServers(server);
        Log.info(this.getClass(), method, "call driveTransactions");
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "driveTransactions");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "complete");
    }

    private void startServers(LibertyServer... servers) {
        final String method = "startServers";

        for (LibertyServer server : servers) {
            assertNotNull("Attempted to start a null server", server);
            ProgramOutput po = null;
            try {
                setUp(server);
                po = server.startServerAndValidate(false, true, true);
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
