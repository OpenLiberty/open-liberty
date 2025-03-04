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

import java.time.Duration;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.postgresql.web.ThreadLocalConnectionTestServlet;

@RunWith(FATRunner.class)
public class ThreadLocalConnectionTest extends FATServletClient {
    private static final Class<?> c = ThreadLocalConnectionTest.class;

    public static final String APP_NAME = "postgresqlApp";
    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";
    private static final int POSTGRE_HOST_PORT = 61000;
    private static final int POSTGRE_CONTAINER_PORT = 5432;

    @Server("server-ThreadLocalConnectionTest")
    @TestServlet(servlet = ThreadLocalConnectionTestServlet.class, contextRoot = APP_NAME, path = APP_NAME + "/ThreadLocalConnectionTestServlet")
    public static LibertyServer server;

    //Need to use FixedHostPortGenericContainer because when using PostgreSQLContainer, when you stop and then
    //restart the container, it randomly selects a different port, which causes the connections after the restart
    //to fail because we have to inject the port number via envVar to the running liberty server, and we can't change it
    //on the liberty server without restarting/updating the config, which would destory the connection pool and
    //invalidate the test.
    //This also means that we can only run this test manually because if this test were to run more than once at the same time
    //on the same host, the ports would conflict and it would fail.
    //But it is still useful to have in case a similar incident happens in the future.
    public static FixedHostPortGenericContainer<?> postgre = new FixedHostPortGenericContainer<>("public.ecr.aws/docker/library/postgres:17-alpine")
                    .withFixedExposedPort(POSTGRE_HOST_PORT, POSTGRE_CONTAINER_PORT)
                    .withEnv("POSTGRES_DB", POSTGRES_DB)
                    .withEnv("POSTGRES_USER", POSTGRES_USER)
                    .withEnv("POSTGRES_PASSWORD", POSTGRES_PASS)
                    .withLogConsumer(new SimpleLogConsumer(c, "postgres"))
                    .waitingFor(new LogMessageWaitStrategy()
                                    .withRegEx(".*database system is ready to accept connections.*\\s")
                                    .withTimes(2)
                                    .withStartupTimeout(Duration.ofSeconds(60)));

    @BeforeClass
    public static void setUp() throws Exception {

        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));

        postgre.start();

        ShrinkHelper.defaultApp(server, APP_NAME, "jdbc.fat.postgresql.web");

        String host = postgre.getHost();
        String port = String.valueOf(POSTGRE_HOST_PORT);
        // String port = String.valueOf(postgre.getMappedPort(5432));
        //String jdbcURL = postgre.getJdbcUrl() + "?user=" + POSTGRES_USER + "&password=" + POSTGRES_PASS;
        String jdbcURL = "jdbc:postgresql://" + postgre.getHost()
                         + ":" + POSTGRE_HOST_PORT
                         + "/" + POSTGRES_DB;
        //Log.info(c, "setUp", "Using PostgreSQL properties: URL=" + jdbcURL);
        Log.info(c, "setUp", "Using PostgreSQL properties: host=" + host + "  port=" + port + ",  URL=" + jdbcURL);
        server.addEnvVar("POSTGRES_HOST", host);
        server.addEnvVar("POSTGRES_PORT", port);
        server.addEnvVar("POSTGRES_DB", POSTGRES_DB);
        server.addEnvVar("POSTGRES_USER", POSTGRES_USER);
        server.addEnvVar("POSTGRES_PASS", POSTGRES_PASS);
        server.addEnvVar("POSTGRES_URL", jdbcURL);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("DSRA0302E", // DSRA0302E:  XAException occurred.  Error code is: XAER_RMFAIL (-7).  Exception is: Error rolling back prepared transaction.
                              "DSRA0304E", // DSRA0304E:  XAException occurred. XAException contents and details are: Caused by org.postgresql.util.PSQLException: This connection has been closed.
                              "J2CA0046E",
                              "J2CA0027E" // J2CA0027E: An exception occurred while invoking rollback on an XA Resource Adapter from DataSource jdbc/postgres/xa
            );
        } finally {
            postgre.stop();
        }
    }

    // You may need to run this test several times before you would be able to recreate the issue in
    // https://github.com/OpenLiberty/open-liberty/issues/20599 without the associated fixes in PoolManager.java
    //
    // If testTLSConnectionValidationAfterShutDown happens on a thread that does NOT have an associated
    // TLS connection in the pool, it would attempt to get a new connection, fail and correctly purge the pool.
    // However, the error would occur when testTLSConnectionValidationAfterShutDown happens on a thread that already had a TLS
    // connection in the pool. It would get that connection and return it without validating it and it would fail in the application
    // and the pool would not get purged.
    //
    // There is not a way to force the thread that services the request but the following output illustrates the error
    // (ie allocateConnection happens on thread 00000037 which would attempt to get the MCWrapper id aacafeaf also on thread 00000037
    //
    //    Free TLS Connection information
    //    MCWrapper id 9a71720c  Managed connection WSRdbManagedConnectionImpl@77ba7e5c  State:STATE_ACTIVE_FREE Thread Id: 00000035 Thread Name: Default Executor-thread-1 Connections being held 0
    //    MCWrapper id aacafeaf  Managed connection WSRdbManagedConnectionImpl@f586c5f0  State:STATE_ACTIVE_FREE Thread Id: 00000037 Thread Name: Default Executor-thread-3 Connections being held 0
    //  Total number of connection in free TLS pool: 2
    //
    //UnShared TLS Connection information
    //  No unshared TLS connections
    //[8/24/22, 10:15:24:275 CDT] 00000037 SystemOut                                                    O <<< END:   testTLSConnectionValidation
    //[8/24/22, 10:15:24:724 CDT] 00000037 SystemOut                                                    O >>> BEGIN: testTLSConnectionValidationAfterShutDown
    //[8/24/22, 10:15:24:725 CDT] 00000037 SystemOut                                                    O Request URL: http://localhost:8010/postgresqlApp/ThreadLocalConnectionTestServlet?testMethod=testTLSConnectionValidationAfterShutDown
    //...
    //...
    //[8/24/22, 10:15:24:811 CDT] 00000037 com.ibm.ejs.j2c.ConnectionManager                            E J2CA0021E: An exception occurred while trying to get a Connection from the Managed Connection resource jdbc/ds3tls : javax.resource.ResourceException: Resource adatepr called connection error event during getConnection processing and did not throw a resource exception.  The reason for this falue may have been logged during the connection error event logging.
    //        at com.ibm.ejs.j2c.ConnectionManager.allocateConnection(ConnectionManager.java:325)
    //        at com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource.getConnection(WSJdbcDataSource.java:140)
    //        at com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource.getConnection(WSJdbcDataSource.java:114)
    @Test
    @AllowedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.resource.spi.ResourceAllocationException" })
    public void testTLSConnectionValidation() throws Exception {

        //Fill connection pool with TLS connections
        FATServletClient.runTest(server, APP_NAME + '/' + "ThreadLocalConnectionTestServlet", "testTLSConnectionValidation");
        FATServletClient.runTest(server, APP_NAME + '/' + "ThreadLocalConnectionTestServlet", "testTLSConnectionValidation");
        FATServletClient.runTest(server, APP_NAME + '/' + "ThreadLocalConnectionTestServlet", "testTLSConnectionValidation");
        FATServletClient.runTest(server, APP_NAME + '/' + "ThreadLocalConnectionTestServlet", "testTLSConnectionValidation");

        Log.info(c, "testTLSConnectionValidation", "Shutting down postgre container");
        postgre.stop();

        //the following call should trigger the pool to be purged
        FATServletClient.runTest(server, APP_NAME + '/' + "ThreadLocalConnectionTestServlet", "testTLSConnectionValidationAfterShutDown");

        Log.info(c, "testTLSConnectionValidation", "Restarting postgre container");
        postgre.start();
        Log.info(c, "testTLSConnectionValidation", "Postgre container restarted");

        String host = postgre.getHost();
        String port = String.valueOf(POSTGRE_HOST_PORT);
        String jdbcURL = "jdbc:postgresql://" + postgre.getHost()
                         + ":" + String.valueOf(POSTGRE_HOST_PORT)
                         + "/" + POSTGRES_DB;
        Log.info(c, "testTLSConnectionValidation", "Using PostgreSQL properties: host=" + host + "  port=" + port + ",  URL=" + jdbcURL);

        FATServletClient.runTest(server, APP_NAME + '/' + "ThreadLocalConnectionTestServlet", "testTLSConnectionValidationAfterRestart");
        FATServletClient.runTest(server, APP_NAME + '/' + "ThreadLocalConnectionTestServlet", "checkPoolAfterTestTLSConnectionValidation");
    }

}
