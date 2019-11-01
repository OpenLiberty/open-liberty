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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.postgresql.web.PostgreSQLNativeSSLTestServlet;
import jdbc.fat.postgresql.web.PostgreSQLSSLTestServlet;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PostgreSQLSSLTest extends FATServletClient {
    private static final Class<?> c = PostgreSQLSSLTest.class;

//    static {
//        // Temporary code to debug zOS file permissions issue
//        getUnixFileMode(new File("lib/LibertyFATTestFiles/ssl-certs/server.crt").getAbsolutePath());
//        getUnixFileMode(new File("lib/LibertyFATTestFiles/ssl-certs/server.key").getAbsolutePath());
//    }

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

    @ClassRule
    public static CustomPostgreSQLContainer<?> postgre = new CustomPostgreSQLContainer<>(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from("postgres:11.2-alpine")
                                    .copy("/var/lib/postgresql/server.crt", "/var/lib/postgresql/server.crt")
                                    .copy("/var/lib/postgresql/server.key", "/var/lib/postgresql/server.key")
                                    .run("chown postgres /var/lib/postgresql/server.key && chmod 600 /var/lib/postgresql/server.key && " +
                                         "chown postgres /var/lib/postgresql/server.crt && chmod 600 /var/lib/postgresql/server.crt")
                                    .build())
                    .withFileFromFile("/var/lib/postgresql/server.crt", new File("lib/LibertyFATTestFiles/ssl-certs/server.crt"),
                                      getUnixFileMode(new File("lib/LibertyFATTestFiles/ssl-certs/server.crt").getAbsolutePath()))
                    .withFileFromFile("/var/lib/postgresql/server.key", new File("lib/LibertyFATTestFiles/ssl-certs/server.key"),
                                      getUnixFileMode(new File("lib/LibertyFATTestFiles/ssl-certs/server.key").getAbsolutePath())))
                                                      .withDatabaseName(POSTGRES_DB)
                                                      .withUsername(POSTGRES_USER)
                                                      .withPassword(POSTGRES_PASS)
                                                      .withExposedPorts(5432)
                                                      .withConfigOption("ssl", "on")
                                                      .withConfigOption("ssl_cert_file", "/var/lib/postgresql/server.crt")
                                                      .withConfigOption("ssl_key_file", "/var/lib/postgresql/server.key")
                                                      .withLogConsumer(PostgreSQLSSLTest::log);

    private static int getUnixFileMode(final String pathAsString) {
        final String m = "getUnixFileMode";
        Log.info(c, m, "Debug for path: " + pathAsString);
        final Path path = Paths.get(pathAsString);

        int mode = 0;
        try {
            int unixMode = (int) Files.getAttribute(path, "unix:mode");
            // Truncate mode bits for z/OS
            if ("OS/390".equals(System.getProperty("os.name")) ||
                "z/OS".equals(System.getProperty("os.name")) ||
                "zOS".equals(System.getProperty("os.name"))) {
                Log.info(c, m, "  on z/OS with file perms: " + unixMode);
                unixMode &= 07777777L;
                unixMode |= Files.isDirectory(path) ? 040000 : 0100000;
            }
            Log.info(c, m, "  resulting file perms: " + unixMode);
            return unixMode;
        } catch (IOException | UnsupportedOperationException e) {
            Log.error(c, m, e, "  Exception obtaining 'unix:mode'");
            // fallback for non-posix environments
            mode = 0100644;
            if (Files.isDirectory(path)) {
                mode = 040755;
                Log.info(c, m, "  is a dir");
            } else if (Files.isExecutable(path)) {
                mode |= 0111; // equiv to +x for user/group/others
                Log.info(c, m, "  is executable");
            } else {
                Log.info(c, m, "  fallback case");
            }
        }

        if (mode > 2097151)
            Log.info(c, m, "  CAUGHT ERROR CONDITION -- MODE IS TOO BIG: " + mode);

        Log.info(c, m, "  returning mode: " + mode);
        return mode;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(serverLibertySSL, APP_NAME, "jdbc.fat.postgresql.web");
        ShrinkHelper.defaultApp(serverNativeSSL, APP_NAME, "jdbc.fat.postgresql.web");

        String host = postgre.getContainerIpAddress();
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
        serverLibertySSL.stopServer();
        serverNativeSSL.stopServer();
    }

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(c, "postgresql-ssl", msg);
    }
}
