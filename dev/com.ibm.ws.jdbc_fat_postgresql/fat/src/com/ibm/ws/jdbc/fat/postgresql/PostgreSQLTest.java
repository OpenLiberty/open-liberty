/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.postgresql;

import java.sql.Connection;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;

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
    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";

    @Server("postgresql-test-server")
    @TestServlet(servlet = PostgreSQLTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static PostgreSQLContainer<?> postgre = new PostgreSQLContainer<>("postgres:11.2-alpine")
                    .withDatabaseName(POSTGRES_DB)
                    .withUsername(POSTGRES_USER)
                    .withPassword(POSTGRES_PASS)
                    .withExposedPorts(5432)
                    .withLogConsumer(PostgreSQLTest::log);

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jdbc.fat.postgresql.web");

        String host = postgre.getContainerIpAddress();
        String port = String.valueOf(postgre.getMappedPort(5432));
        Log.info(c, "setUp", "Using PostgreSQL properties: host=" + host + "  port=" + port);
        server.addEnvVar("POSTGRES_HOST", host);
        server.addEnvVar("POSTGRES_PORT", port);
        server.addEnvVar("POSTGRES_DB", POSTGRES_DB);
        server.addEnvVar("POSTGRES_USER", POSTGRES_USER);
        server.addEnvVar("POSTGRES_PASS", POSTGRES_PASS);

        // Create tables for the DB
        try (Connection conn = postgre.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE people( id integer UNIQUE NOT NULL, name VARCHAR (50) );");
            stmt.close();
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(c, "postgresql", msg);
    }
}
