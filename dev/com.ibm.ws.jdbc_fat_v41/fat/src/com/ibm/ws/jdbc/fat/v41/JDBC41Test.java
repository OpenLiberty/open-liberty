/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.v41;

import static com.ibm.ws.jdbc.fat.v41.FATSuite.appName;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.v41.web.BasicTestServlet;
import jdbc.fat.v41.web.DefaultDataSourceTestServlet;
import jdbc.fat.v41.web.NetworkTimeoutTestServlet;

@RunWith(FATRunner.class)
public class JDBC41Test extends FATServletClient {

    @Server("com.ibm.ws.jdbc.fat.v41")
    @TestServlets({ @TestServlet(servlet = BasicTestServlet.class, path = appName + "/BasicTestServlet"),
                    @TestServlet(servlet = NetworkTimeoutTestServlet.class, path = appName + "/NetworkTimeoutTestServlet"),
                    @TestServlet(servlet = DefaultDataSourceTestServlet.class, path = appName + "/DefaultDataSourceTestServlet") })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // TODO get DB rotation working in OL
        // server.configureForAnyDatabase();
        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("J2CA0081E.*slowDS", // expected by testNetworkTimeoutSimple
                          "J2CA0026E.*RollbackException", // from testTransactionTimeoutAbort when transaction times out before enlist
                          "J2CA0027E", // expected by testTransactionTimeoutAbort - XAds removed because it does not appear on the same line as the message ID in every language
                          "DSRA0302E.*XAException*", // expected by testTransactionTimeoutAbort
                          "DSRA0304E.*XAException*", // expected by testTransactionTimeoutAbort
                          "DSRA9400E.*addSync", // from testTransactionTimeoutAbort when transaction times out before enlist
                          "J2CA0045E", //expected from testMaxPoolSizeWithTLS
                          "J2CA0079E"); // expected by testMBeanPurgeAbort
    }

    /**
     * First servlet function gets and aborts a shareable connection.
     * Second one gets and closes a shareable connection.
     * Test ensures that the aborted connection is immediately destroyed
     * so that new getConnection() attempts do not grab aborted connection.
     */
    @Test
    public void testAbortedConnectionDestroyed() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testAbortedConnectionDestroyed");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "getSingleConnectionAfterAbort");
    }

    @Test
    public void testNumConnectionsPerThreadLocal() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testNumConnectionsPerThreadLocal");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "checkPoolAfterNumConnectionsPerThreadLocal");
    }

    @Test
    public void testAgedTimeoutImmediateWithTLS() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testAgedTimeoutImmediateWithTLS");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "checkPoolAfterTestAgedTimeoutImmediateWithTLS");
    }

    @Test
    public void testAgedTimeoutWithTLS() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testAgedTimeoutWithTLS");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "checkPoolAfterTestAgedTimeoutWithTLS");
    }

    @Test
    public void testAgedTimeout90mWithTLS() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testAgedTimeout90mWithTLS");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "checkPoolAfterTestAgedTimeout90mWithTLS");
    }

    @Test
    public void testAgedTimeoutDisabledWithTLS() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testAgedTimeoutDisabledWithTLS");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "checkPoolAfterTestAgedTimeoutDisabledWithTLS");
    }

    @Test
    public void testReapDisabledWithTLS() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testReapDisabledWithTLS");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "checkPoolAfterTestReapDisabledWithTLS");
    }

    @Test
    public void testMinPoolSizeMettWithTLS() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testMinPoolSizeMettWithTLS");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "checkPoolAfterTestMinPoolSizeMettWithTLS");
    }

    @Test
    public void testMinPoolSizeNotMettWithTLS() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testMinPoolSizeNotMettWithTLS");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "checkPoolAfterTestMinPoolSizeNotMettWithTLS");
    }

    @Test
    public void testMaxIdleTimeDisabledtWithTLS() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testMaxIdleTimeDisabledtWithTLS");
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "checkPoolAfterTestMaxIdleTimeDisabledtWithTLS");
    }

    @Test
    public void testJDBCVersionLimiting() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testJDBCVersionLimiting&expectedVersion=4.1");
    }

    @Test
    public void testMaxInUseTime() throws Exception {
        FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testMaxInUseTime");
    }

    @Test
    public void testMaxInUsePropertyUpdate() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ServerConfiguration newConfig = config.clone();
        try {
            FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testGet2MaxInUseTimeConnections");
            FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testGet2MaxInUseTimeConnectionsAreGone");

            for (DataSource datasource : newConfig.getDataSources()) {
                if (datasource.getId().equalsIgnoreCase("maxInUseUpdateTest")) {
                    datasource.getConnectionManagers().get(0).setMaxInUseTime("0");
                    break;
                }
            }

            // Update the config, to make sure the server registers it gone.
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(newConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(appName));
            server.waitForStringInLogUsingMark(".*CWWKZ000[13]I.*");

            FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testGet2MaxInUseTimeConnections");
            FATServletClient.runTest(server, appName + '/' + "BasicTestServlet", "testGet2MaxInUseTimeConnectionsRemain");
        } finally {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(appName));
            server.waitForStringInLogUsingMark(".*CWWKZ000[13]I.*");
        }

    }

}
