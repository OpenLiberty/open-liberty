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

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;

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
public class DualServerDynamicPostgreSQLTest extends DualServerDynamicCoreTest {
    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";
    private static final int LOG_SEARCH_TIMEOUT = 120000;

    @Server("com.ibm.ws.transaction_CLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer firstServer;

    @Server("com.ibm.ws.transaction_CLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer secondServer;

    @ClassRule
    public static CustomPostgreSQLContainer<?> postgre = new CustomPostgreSQLContainer<>(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from("postgres:11.2-alpine")
                                    .copy("/var/lib/postgresql/server.crt", "/var/lib/postgresql/server.crt")
                                    .copy("/var/lib/postgresql/server.key", "/var/lib/postgresql/server.key")
                                    .run("chown postgres /var/lib/postgresql/server.key && chmod 600 /var/lib/postgresql/server.key && " +
                                         "chown postgres /var/lib/postgresql/server.crt && chmod 600 /var/lib/postgresql/server.crt")
                                    .build())
                    .withFileFromFile("/var/lib/postgresql/server.crt", new File("lib/LibertyFATTestFiles/ssl-certs/server.crt"))
                    .withFileFromFile("/var/lib/postgresql/server.key", new File("lib/LibertyFATTestFiles/ssl-certs/server.key")))
                                    .withDatabaseName(POSTGRES_DB)
                                    .withUsername(POSTGRES_USER)
                                    .withPassword(POSTGRES_PASS)
                                    .withConfigOption("ssl", "on")
                                    .withConfigOption("max_prepared_transactions", "2")
                                    .withConfigOption("ssl_cert_file", "/var/lib/postgresql/server.crt")
                                    .withConfigOption("ssl_key_file", "/var/lib/postgresql/server.key")
                                    .withLogConsumer(DualServerDynamicPostgreSQLTest::log);

    public static void setUp(LibertyServer server) throws Exception {

        String host = postgre.getContainerIpAddress();
        String port = String.valueOf(postgre.getMappedPort(5432));
        String jdbcURL = postgre.getJdbcUrl() + "?user=" + POSTGRES_USER + "&password=" + POSTGRES_PASS;
        Log.info(DualServerDynamicPostgreSQLTest.class, "setUp", "Using PostgreSQL properties: host=" + host + "  port=" + port + ",  URL=" + jdbcURL);

        server.addEnvVar("POSTGRES_HOST", host);
        server.addEnvVar("POSTGRES_PORT", port);
        server.addEnvVar("POSTGRES_DB", POSTGRES_DB);
        server.addEnvVar("POSTGRES_USER", POSTGRES_USER);
        server.addEnvVar("POSTGRES_PASS", POSTGRES_PASS);
        server.addEnvVar("POSTGRES_URL", jdbcURL);
    }

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(DualServerDynamicPostgreSQLTest.class, "postgresql-ssl", msg);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("NYTRACE: DualServerDynamicDBTest.setUp called");
        server1 = firstServer;
        server2 = secondServer;
        servletName = "transaction/Simple2PCCloudServlet";
        cloud1RecoveryIdentity = "cloud001";
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.*");

    }

    @Override
    public void dynamicTest(LibertyServer server1, LibertyServer server2, int test, int resourceCount) throws Exception {
        final String method = "dynamicTest";
        final String id = String.format("%03d", test);
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // Start Server1
        setUp(server1);
        server1.startServer();

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, servletName, "setupRec" + id);
        } catch (Throwable e) {
            // as expected
            Log.error(this.getClass(), method, e); // TODO remove this
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        // wait for 1st server to have gone away
        if (server1.waitForStringInLog("Dump State:") == null) {
            testFailed = true;
            testFailureString = "First server did not crash";
        }

        // Now start server2
        if (!testFailed) {
            server2.setHttpDefaultPort(Cloud2ServerPort);
            setUp(server2);
            ProgramOutput po = server2.startServerAndValidate(false, true, true);

            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not start server2");
                Log.error(this.getClass(), "dynamicTest", ex);
                throw ex;
            }

            // wait for 2nd server to perform peer recovery
            if (server2.waitForStringInTrace("Performed recovery for " + cloud1RecoveryIdentity, LOG_SEARCH_TIMEOUT) == null) {
                testFailed = true;
                testFailureString = "Second server did not perform peer recovery";
            }
        }

        // flush the resource states
        if (!testFailed) {

            try {
                sb = runTestWithResponse(server2, servletName, "dumpState");
                Log.info(this.getClass(), method, sb.toString());
            } catch (Exception e) {
                Log.error(this.getClass(), method, e);
                throw e;
            }

            //Stop server2
            server2.stopServer(null);

            // restart 1st server
            server1.resetStarted();
            setUp(server1);
            server1.startServerAndValidate(false, true, true);

            if (server1.waitForStringInTrace("WTRN0133I") == null) {
                testFailed = true;
                testFailureString = "Recovery incomplete on first server";
            }
        }

        if (!testFailed) {

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
            server1.stopServer(null);
            setUp(server1);
            server1.startServerAndValidate(false, true, true);

            // Check log was cleared
            if (server1.waitForStringInTrace("WTRN0135I") == null) {
                testFailed = true;
                testFailureString = "Transactions left in transaction log on first server";
            }
            if (!testFailed && (server1.waitForStringInTrace("WTRN0134I.*0") == null)) {
                testFailed = true;
                testFailureString = "XAResources left in partner log on first server";
            }

        }

        tidyServerAfterTest(server1);
        tidyServerAfterTest(server2);
        // XA resource data is cleared in setup servlet methods. Probably should do it here.
        if (testFailed)
            fail(testFailureString);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // server1.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
    }

}
