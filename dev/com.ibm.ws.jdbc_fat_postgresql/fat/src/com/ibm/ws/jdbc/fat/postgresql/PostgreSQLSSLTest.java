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
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.PostgreSQLContainer;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.postgresql.web.PostgreSQLNativeSSLTestServlet;
import jdbc.fat.postgresql.web.PostgreSQLSSLTestServlet;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PostgreSQLSSLTest extends FATServletClient {
    private static final Class<?> c = PostgreSQLSSLTest.class;

    public static final String APP_NAME = "postgresqlApp";
    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";

    @Server("server-PostgreSQLSSLTest")
    @TestServlet(servlet = PostgreSQLSSLTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverLibertySSL;

    @Server("server-PostgreSQLSSLTest-native")
    @TestServlet(servlet = PostgreSQLNativeSSLTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverNativeSSL;

    public static PostgreSQLContainer postgre = new PostgreSQLContainer("kyleaure/postgres-ssl:1.0")
                    .withDatabaseName(POSTGRES_DB)
                    .withUsername(POSTGRES_USER)
                    .withPassword(POSTGRES_PASS)
                    .withSSL()
                    .withLogConsumer(new SimpleLogConsumer(c, "postgre-ssl"));

    @BeforeClass
    public static void setUp() throws Exception {

        serverLibertySSL.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        serverNativeSSL.addIgnoredErrors(Arrays.asList("CWPKI0063W"));

        postgre.start();

        ShrinkHelper.defaultApp(serverLibertySSL, APP_NAME, "jdbc.fat.postgresql.web");
        ShrinkHelper.defaultApp(serverNativeSSL, APP_NAME, "jdbc.fat.postgresql.web");

        String host = postgre.getHost();
        String port = String.valueOf(postgre.getMappedPort(5432));
        String jdbcURL = postgre.getJdbcUrl() + "?user=" + POSTGRES_USER + "&password=" + POSTGRES_PASS;
        Log.info(c, "setUp", "Using PostgreSQL properties: host=" + host + "  port=" + port + ",  URL=" + jdbcURL);

        serverLibertySSL.addEnvVar("POSTGRES_HOST", host);
        serverLibertySSL.addEnvVar("POSTGRES_PORT", port);
        serverLibertySSL.addEnvVar("POSTGRES_DB", POSTGRES_DB);
        serverLibertySSL.addEnvVar("POSTGRES_USER", POSTGRES_USER);
        serverLibertySSL.addEnvVar("POSTGRES_PASS", POSTGRES_PASS);
        serverLibertySSL.addEnvVar("POSTGRES_URL", jdbcURL);

        serverNativeSSL.addEnvVar("POSTGRES_HOST", host);
        serverNativeSSL.addEnvVar("POSTGRES_PORT", port);
        serverNativeSSL.addEnvVar("POSTGRES_DB", POSTGRES_DB);
        serverNativeSSL.addEnvVar("POSTGRES_USER", POSTGRES_USER);
        serverNativeSSL.addEnvVar("POSTGRES_PASS", POSTGRES_PASS);
        serverNativeSSL.addEnvVar("POSTGRES_URL", jdbcURL);

        // Create tables for the DB
        try (Connection conn = postgre.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE people( id integer UNIQUE NOT NULL, name VARCHAR (50) );");
            stmt.close();
        }
        serverLibertySSL.startServer();
        serverNativeSSL.useSecondaryHTTPPort();
        serverNativeSSL.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            serverLibertySSL.stopServer();
        } finally {
            try {
                serverNativeSSL.stopServer();
            } finally {
                postgre.stop();
            }
        }
    }
}
