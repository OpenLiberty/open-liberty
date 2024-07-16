/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.postgresql;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.postgresql.web.PostgreSQLTestServlet;

@RunWith(FATRunner.class)
public class PostgreSQLTest extends FATServletClient {
    private static final Class<?> c = PostgreSQLTest.class;

    public static final String APP_NAME = "postgresqlApp";

    @Server("postgresql-test-server")
    @TestServlet(servlet = PostgreSQLTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));

        ShrinkHelper.defaultApp(server, APP_NAME, "jdbc.fat.postgresql.web");

        String host = FATSuite.postgre.getHost();
        String port = String.valueOf(FATSuite.postgre.getMappedPort(5432));
        String jdbcURL = FATSuite.postgre.getJdbcUrl() + "?user=" + FATSuite.postgre.getUsername() + "&password=" + FATSuite.postgre.getPassword();
        Log.info(c, "setUp", "Using PostgreSQL properties: host=" + host + "  port=" + port + ",  URL=" + jdbcURL);
        server.addEnvVar("POSTGRES_HOST", host);
        server.addEnvVar("POSTGRES_PORT", port);
        server.addEnvVar("POSTGRES_DB", FATSuite.postgre.getDatabaseName());
        server.addEnvVar("POSTGRES_USER", FATSuite.postgre.getUsername());
        server.addEnvVar("POSTGRES_PASS", FATSuite.postgre.getPassword());
        server.addEnvVar("POSTGRES_URL", jdbcURL);

        // Create tables for the DB
        try (Connection conn = FATSuite.postgre.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE people( id integer UNIQUE NOT NULL, name VARCHAR (50) );");
            stmt.execute("CREATE SCHEMA premadeschema");
            stmt.close();
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("DSRA0302E", // DSRA0302E:  XAException occurred.  Error code is: XAER_RMFAIL (-7).  Exception is: Error rolling back prepared transaction.
                          "DSRA0304E", // DSRA0304E:  XAException occurred. XAException contents and details are: Caused by org.postgresql.util.PSQLException: This connection has been closed.
                          "J2CA0027E" // J2CA0027E: An exception occurred while invoking rollback on an XA Resource Adapter from DataSource jdbc/postgres/xa
        );
    }
}
