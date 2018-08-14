/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.tck.launcher;

import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.microprofile.metrics.fat.utils.MetricsAuthTestUtil;
import com.ibm.ws.microprofile.metrics.fat.utils.MetricsConnection;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */

@RunWith(FATRunner.class)
public class MetricsAuthenticationTest {

    private final static int TIMEOUT = 60000; // in milliseconds

    @Server("MetricsAuthenticationServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer("MetricsAuthenticationServer.log", true);

        HttpUtils.trustAllCertificates();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testMetrics10Auth() throws Exception {

        MetricsAuthTestUtil.removeFeature(server, "mpMetrics-1.1");
        MetricsAuthTestUtil.addFeature(server, "mpMetrics-1.0");

        //Check that /metrics endpoint is available
        assertNotNull("Web application is not available at available */metrics/", server.waitForStringInLog("CWWKT0016I.*/metrics/", TIMEOUT, server.getDefaultLogFile()));

        testMetricsAuth();
    }

    @Test
    public void testMetrics11Auth() throws Exception {

        MetricsAuthTestUtil.removeFeature(server, "mpMetrics-1.0");
        MetricsAuthTestUtil.addFeature(server, "mpMetrics-1.1");

        //Check that /metrics endpoint is available
        assertNotNull("Web application is not available at available */metrics/", server.waitForStringInLog("CWWKT0016I.*/metrics/", TIMEOUT, server.getDefaultLogFile()));

        testMetricsAuth();
    }

    private static void setMetricsAuthConfig(LibertyServer server, Boolean authentication, boolean waitForLogMessages) throws Exception {
        if (waitForLogMessages) {
            server.setMarkToEndOfLog();
        }
        ServerConfiguration config = server.getServerConfiguration();
        config.getMetricsElement().setAuthentication(authentication);
        server.updateServerConfiguration(config);
        if (waitForLogMessages) {
            assertNotNull("Config wasn't updated successfully",
                          server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*", TIMEOUT));
        }
    }

    private void testMetricsAuth() throws Exception {

        //1. When authentication is not explicitly set in server.xml, it defaults to private,
        //  i.e. requires authentication into metrics endpoint

        //check that opening connection to /metrics requires authentication by default

        MetricsConnection authenticationNotSpecified = MetricsConnection.privateConnection(server);
        authenticationNotSpecified.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();

        //check that opening connection to /metrics with default authentication with invalid credentials returns HTPP 401 ERROR

        MetricsConnection authenticationNotSpecifiedInvalidHeader = MetricsConnection.privateConnectionInvalidHeader(server);
        authenticationNotSpecifiedInvalidHeader.expectedResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED).getConnection();

        //2. When authentication is explicitly set to true in server.xml, metrics endpoint requires authentication,
        //  i.e. is private

        setMetricsAuthConfig(server, true, true);
        MetricsConnection authenticationTrue = MetricsConnection.privateConnection(server);
        authenticationTrue.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();

        //3. When authentication is explicitly set to false in server.xml, metrics enpoint does not require authentication,
        // i.e. is public

        setMetricsAuthConfig(server, false, true);
        MetricsConnection authenticationFalse = MetricsConnection.publicConnection(server);
        authenticationFalse.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();

        //4. When mpMetrics authentication option is removed from the server.xml,
        // the default metrics endpoint requires authentication

        setMetricsAuthConfig(server, null, true);

        MetricsConnection authenticationRemoved = MetricsConnection.privateConnection(server);
        authenticationRemoved.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();
    }

}
