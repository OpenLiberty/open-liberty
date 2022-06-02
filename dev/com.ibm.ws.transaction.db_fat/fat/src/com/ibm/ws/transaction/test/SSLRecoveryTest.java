/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.wait.strategy.Wait;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.web.SSLRecoveryServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.PostgreSQLContainer;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SSLRecoveryTest extends FATServletClient {
    private static final Class<?> c = SSLRecoveryTest.class;

    public static final String APP_NAME = "sslRecovery";
    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";

    @Server("ssl-recovery")
    @TestServlet(servlet = SSLRecoveryServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverLibertySSL;

    // The Dockerfile for 'jonhawkes/postgresql-ssl:1.0' can be found in the com.ibm.ws.jdbc_fat_postgresql project
    public static PostgreSQLContainer testContainer = new PostgreSQLContainer("jonhawkes/postgresql-ssl:1.0")
                    .withDatabaseName(POSTGRES_DB)
                    .withUsername(POSTGRES_USER)
                    .withPassword(POSTGRES_PASS)
                    .withSSL()
                    .withLogConsumer(new SimpleLogConsumer(SSLRecoveryTest.class, "postgre-ssl"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        testContainer.withStartupTimeout(FATUtils.TESTCONTAINER_STARTUP_TIMEOUT).waitingFor(Wait.forLogMessage(".*database system is ready.*", 2)).start();

        setUp();

        // Create tables for the DB
        try (Connection conn = testContainer.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE people( id integer UNIQUE NOT NULL, name VARCHAR (50) );");
            stmt.close();
        }

        serverLibertySSL.startServer();
    }

    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(serverLibertySSL, APP_NAME, "com.ibm.ws.transaction.web");

        String host = testContainer.getContainerIpAddress();
        String port = String.valueOf(testContainer.getMappedPort(5432));
        String jdbcURL = testContainer.getJdbcUrl() + "?user=" + POSTGRES_USER + "&password=" + POSTGRES_PASS;
        Log.info(c, "setUp", "Using PostgreSQL properties: host=" + host + "  port=" + port + ",  URL=" + jdbcURL);

        serverLibertySSL.addEnvVar("POSTGRES_HOST", host);
        serverLibertySSL.addEnvVar("POSTGRES_PORT", port);
        serverLibertySSL.addEnvVar("POSTGRES_DB", POSTGRES_DB);
        serverLibertySSL.addEnvVar("POSTGRES_USER", POSTGRES_USER);
        serverLibertySSL.addEnvVar("POSTGRES_PASS", POSTGRES_PASS);
        serverLibertySSL.addEnvVar("POSTGRES_URL", jdbcURL);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            serverLibertySSL.stopServer();
        } finally {
            testContainer.stop();
        }
    }

    @Test
    @AllowedFFDC(value = { "org.postgresql.util.PSQLException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testRec001() throws Exception {
        recoveryTest("001");
    }

    protected void recoveryTest(String id) throws Exception {
        final String method = "recoveryTest";

        StringBuilder sb = null;

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(serverLibertySSL, APP_NAME + "/SSLRecoveryServlet", "setupRec" + id);
            fail("setupRec" + id + " returned: " + sb);
        } catch (IOException e) {
            // This is what we expect. The setup servlet crashed its server
        }

        serverLibertySSL.waitForStringInLog(XAResourceImpl.DUMP_STATE);
        serverLibertySSL.resetStarted();

        setUp();
        serverLibertySSL.startServer();

        // Server appears to have started ok
        assertNotNull(serverLibertySSL.getServerName() + " did not recover", serverLibertySSL.waitForStringInTrace("Performed recovery for " + serverLibertySSL.getServerName()));
        Log.info(this.getClass(), method, "calling checkRec" + id);

        sb = runTestWithResponse(serverLibertySSL, APP_NAME + "/SSLRecoveryServlet", "checkRec" + id);
        Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);
    }
}