/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.utility.DockerImageName;

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

    //TODO Start using ImageBuilder
//    private static final DockerImageName POSTGRES_SSL = ImageBuilder.build("postgres-ssl:17")
//                    .getDockerImageName()
//                    .asCompatibleSubstituteFor("postgres");

    private static final DockerImageName POSTGRES_SSL = DockerImageName.parse("kyleaure/postgres-ssl:1.0").asCompatibleSubstituteFor("postgres");

    public static PostgreSQLContainer postgre = new PostgreSQLContainer(POSTGRES_SSL)
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

        // TODO extract security files from container prior to server start
        // TODO delete security files from git

//        postgre.copyFileFromContainer("/tmp/clientKeystore.p12", serverLibertySSL.getServerRoot() + "/resources/security/outboundKeys.p12");
//        postgre.copyFileFromContainer("/var/lib/postgresql/server.crt", serverLibertySSL.getServerRoot() + "/resources/security/server.crt");
//        importServerCert(serverLibertySSL.getServerRoot() + "/resources/security/outboundKeys.p12",
//                         serverLibertySSL.getServerRoot() + "/resources/security/server.crt");
//
//        postgre.copyFileFromContainer("/tmp/clientKeystore.p12", serverNativeSSL.getServerRoot() + "/resources/security/outboundKeys.p12");
//        postgre.copyFileFromContainer("/var/lib/postgresql/server.crt", serverNativeSSL.getServerRoot() + "/resources/security/server.crt");

        serverLibertySSL.startServer();
        serverNativeSSL.useSecondaryHTTPPort();
        serverNativeSSL.startServer();
    }

    private static void importServerCert(String source, String serverCert) {
        final String m = "importServerCert";

        String[] command = new String[] {
                                          "keytool", "-import", //
                                          "-alias", "server", //
                                          "-file", serverCert, //
                                          "-keystore", source, //
                                          "-storetype", "pkcs12", //
                                          "-storepass", "liberty", //
                                          "-noprompt"
        };

        String errorPrelude = "Could not import server certificate into client keystore: " + source;
        try {
            Process p = Runtime.getRuntime().exec(command);
            if (!p.waitFor(FATRunner.FAT_TEST_LOCALRUN ? 10 : 20, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                dumpOutput(m, "Keytool process timed out", p);
                throw new RuntimeException(errorPrelude + " timed out waiting for process to finish.");
            }
            if (p.exitValue() != 0) {
                dumpOutput(m, "Non 0 exit code from keytool", p);
                throw new RuntimeException(errorPrelude + " see logs for details");
            }
            dumpOutput(m, "Keytool command completed successfully", p);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(errorPrelude, e);
        }
    }

    private static void dumpOutput(String method, String message, Process p) {
        String out = "stdOut:" + System.lineSeparator() + readInputStream(p.getInputStream());
        String err = "stdErr:" + System.lineSeparator() + readInputStream(p.getErrorStream());
        Log.info(c, method, message + //
                            System.lineSeparator() + out + //
                            System.lineSeparator() + err);
    }

    private static String readInputStream(InputStream is) {
        @SuppressWarnings("resource")
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
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
