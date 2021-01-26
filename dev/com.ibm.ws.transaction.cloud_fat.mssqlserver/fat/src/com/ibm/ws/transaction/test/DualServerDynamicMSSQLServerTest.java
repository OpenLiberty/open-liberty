/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import static com.ibm.ws.transaction.test.FATSuite.sqlserver;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.test.tests.DualServerDynamicCoreTest;
import com.ibm.ws.transaction.web.Simple2PCCloudServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;

@Mode
@RunWith(FATRunner.class)
public class DualServerDynamicMSSQLServerTest extends DualServerDynamicCoreTest {

    private static final int LOG_SEARCH_TIMEOUT = 120000;

    @Server("com.ibm.ws.transaction_CLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer firstServer;

    @Server("com.ibm.ws.transaction_CLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer secondServer;

    public static void setUp(LibertyServer server) throws Exception {

        String dbName = "test";
        initDatabase(sqlserver, dbName);

        server.addEnvVar("DBNAME", dbName);
        server.addEnvVar("HOST", sqlserver.getContainerIpAddress());
        server.addEnvVar("PORT", Integer.toString(sqlserver.getFirstMappedPort()));
        server.addEnvVar("USER", sqlserver.getUsername());
        server.addEnvVar("PASSWORD", sqlserver.getPassword());
        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server1 = firstServer;
        server2 = secondServer;
        servletName = APP_NAME + "/Simple2PCCloudServlet";
        cloud1RecoveryIdentity = "cloud001";
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.*");
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
                Log.info(DualServerDynamicMSSQLServerTest.class, "initDatabase", "database exists already");
            } else {
                Log.info(DualServerDynamicMSSQLServerTest.class, "initDatabase", "create database returned sqlexception: " + sqlex);
                Log.info(DualServerDynamicMSSQLServerTest.class, "initDatabase", "Message: " + sqlex.getMessage());
                Log.info(DualServerDynamicMSSQLServerTest.class, "initDatabase", "SQLSTATE: " + sqlex.getSQLState());
                Log.info(DualServerDynamicMSSQLServerTest.class, "initDatabase", "Error code: " + sqlex.getErrorCode());
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

    @Override
    public void dynamicTest(LibertyServer server1, LibertyServer server2, int test, int resourceCount) throws Exception {
        final String method = "dynamicTest";
        final String id = String.format("%03d", test);
        StringBuilder sb = null;
        Log.info(this.getClass(), method, "Starting dynamic test in DualServerDynamicMSSQLServerTest");
        // Start Server1
        startServers(server1);
        Log.info(this.getClass(), method, "now invoke runTestWithResponse from DualServerDynamicMSSQLServerTest");
        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, servletName, "setupRec" + id);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "back from runTestWithResponse in DualServerDynamicMSSQLServerTest, sb is " + sb);
        assertNull("setupRec" + id + " returned: " + sb, sb);

        Log.info(this.getClass(), method, "wait for first server to go away in DualServerDynamicMSSQLServerTest");
        // wait for 1st server to have gone away
        assertNotNull(server1.getServerName() + " did not crash", server1.waitForStringInLog("Dump State:"));

        // Now start server2
        server2.setHttpDefaultPort(Cloud2ServerPort);
        startServers(server2);

        // wait for 2nd server to perform peer recovery
        assertNotNull(server2.getServerName() + " did not perform peer recovery",
                      server2.waitForStringInTrace("Performed recovery for " + cloud1RecoveryIdentity, LOG_SEARCH_TIMEOUT));

        // flush the resource states
        try {
            sb = runTestWithResponse(server2, servletName, "dumpState");
            Log.info(this.getClass(), method, sb.toString());
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            fail(e.getMessage());
        }

        //Stop server2
        server2.stopServer((String[]) null);

        // restart 1st server
        server1.resetStarted();
        startServers(server1);

        assertNotNull("Recovery incomplete on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0133I"));

        // check resource states
        Log.info(this.getClass(), method, "calling checkRec" + id);
        try {
            sb = runTestWithResponse(server1, servletName, "checkRec" + id);
        } catch (Exception e) {
            Log.error(this.getClass(), "dynamicTest", e);
            throw e;
        }
        Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);

        // Bounce first server to clear log
        server1.stopServer((String[]) null);
        startServers(server1);

        // Check log was cleared
        assertNotNull("Transactions left in transaction log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0135I"));
        assertNotNull("XAResources left in partner log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0134I.*0"));
    }

    @After
    public void tearDown() throws Exception {
        tidyServerAfterTest(server1);
        tidyServerAfterTest(server2);
    }

    private void startServers(LibertyServer... servers) {
        final String method = "startServers";

        for (LibertyServer server : servers) {
            assertNotNull("Attempted to start a null server", server);
            ProgramOutput po = null;
            try {
                setUp(server);
                po = server.startServerAndValidate(false, false, false);
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
