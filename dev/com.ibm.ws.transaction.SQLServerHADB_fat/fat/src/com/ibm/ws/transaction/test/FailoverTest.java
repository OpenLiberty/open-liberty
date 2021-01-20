/*******************************************************************************
 * Copyright (c) 2020 2021 IBM Corporation and others.
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

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

    @Mode(TestMode.LITE)
    @Test
    public void testHADBControl() throws Exception {
        final String method = "testHADBControl";
        StringBuilder sb = null;
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testControlSetup");
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "testControlSetup returned: " + sb);

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testDriveTransactions");
        } catch (Throwable e) {
        }
    }

    /**
     * Run a set of transactions and simulate an HA condition
     */
    @Mode(TestMode.LITE)
    @Test
    public void testHADBRuntimeFailoverKnownSqlcode() throws Exception {
        final String method = "testHADBRuntimeFailoverKnownSqlcode";
        StringBuilder sb = null;

        logny("testHADBRuntimeFailoverKnownSqlcode - call testSetupKnownSqlcode");

        Log.info(this.getClass(), method, "Call testSetupKnownSqlcode");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testSetupKnownSqlcode");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "testSetupKnownSqlcode returned: " + sb);
        Log.info(this.getClass(), method, "Call stopserver");

        server.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E");

        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);

        Log.info(this.getClass(), method, "call startserver");
        startServers(server);

        Log.info(this.getClass(), method, "Call testDriveTransactions");
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testDriveTransactions");
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
    public void testHADBRuntimeFailoverUnKnownSqlcode() throws Exception {
        final String method = "testHADBRuntimeFailoverUnKnownSqlcode";
        StringBuilder sb = null;

        Log.info(this.getClass(), method, "call testSetupUnKnownSqlcode");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testSetupUnKnownSqlcode");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "call stopserver");
        server.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E");
        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);
        Log.info(this.getClass(), method, "call startserver");
        startServers(server);

        Log.info(this.getClass(), method, "complete");
        Log.info(this.getClass(), method, "call testDriveTransactionsWithFailure");
        // An unhandled sqlcode will lead to a failure to write to the log, the
        // invalidation of the log and the throwing of Internal LogExceptions
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testDriveTransactionsWithFailure");
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
    public void testHADBStartupFailoverKnownSqlcode() throws Exception {
        final String method = "testHADBStartupFailoverKnownSqlcode";
        StringBuilder sb = null;

        Log.info(this.getClass(), method, "call testStartupSetup");

        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testStartupSetup");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "call stopserver");
        server.stopServer("WTRN0075W", "WTRN0076W", "CWWKE0701E", "DSRA8020E");
        Log.info(this.getClass(), method, "set timeout");
        server.setServerStartTimeout(30000);
        Log.info(this.getClass(), method, "call startserver");
        startServers(server);
        Log.info(this.getClass(), method, "call testDriveTransactions");
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, "testDriveTransactions");
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "complete");
    }

    private static void logny(String text) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:/temp/HADBTranlogTest.txt", true)));
            java.util.Date date = new java.util.Date();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
            String formattedDate = sdf.format(date);
            out.println(formattedDate + ": " + text);
            out.close();
        } catch (IOException e) {
            // exception handling left as an exercise for the reader
        }
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
