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

import org.junit.After;
import org.junit.Before;
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

    @Before
    public void setUp() throws Exception {

        HttpUtils.trustAllCertificates();

        server.startServer("MetricsAuthenticationServer.log", true);

    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
        MetricsAuthTestUtil.removeFeature(server, "mpMetrics-1.0");
        MetricsAuthTestUtil.removeFeature(server, "mpMetrics-1.1");
    }

    @Test
    public void testMetrics10Auth() throws Exception {
        addFeature(false);

        testMetricsAuth();
    }

    @Test
    public void testMetrics11Auth() throws Exception {
        addFeature(true);

        testMetricsAuth();
    }

    private void addFeature(boolean enable11) throws Exception {
        server.setMarkToEndOfLog();

        if (enable11) {
            MetricsAuthTestUtil.addFeature(server, "mpMetrics-1.1");
        } else {
            MetricsAuthTestUtil.addFeature(server, "mpMetrics-1.0");
        }

        waitForMetricsEndpoint(server);
    }

    private void waitForMetricsEndpoint(LibertyServer server) throws Exception {
        assertNotNull("Web application is not available at */metrics/", server.waitForStringInLog("CWWKT0016I.*/metrics/", TIMEOUT, server.getDefaultLogFile()));
    }

    private static void setMetricsAuthConfig(LibertyServer server, Boolean authentication) throws Exception {
        server.setMarkToEndOfLog();

        ServerConfiguration config = server.getServerConfiguration();
        config.getMPMetricsElement().setAuthentication(authentication);
        server.updateServerConfiguration(config);
        assertNotNull("Config wasn't updated successfully",
                      server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*", TIMEOUT));
    }

    private void testMetricsAuth() throws Exception {

        //1. When authentication is not explicitly set in server.xml, it defaults to private,
        //  i.e. requires authentication into metrics endpoint

        //check that opening connection to /metrics requires authentication by default

        Thread.sleep(10000);

        MetricsConnection authenticationNotSpecified = MetricsConnection.privateConnection(server);
        authenticationNotSpecified.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();

        //check that opening connection to /metrics with default authentication with invalid credentials returns HTTP 401 ERROR

        MetricsConnection authenticationNotSpecifiedInvalidHeader = MetricsConnection.privateConnectionInvalidHeader(server);
        authenticationNotSpecifiedInvalidHeader.expectedResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED).getConnection();

        //2. When authentication is explicitly set to true in server.xml, metrics endpoint requires authentication,
        //  i.e. is private

        setMetricsAuthConfig(server, true);
        MetricsConnection authenticationTrue = MetricsConnection.privateConnection(server);
        authenticationTrue.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();

        //3. When authentication is explicitly set to false in server.xml, metrics enpoint does not require authentication,
        // i.e. is public

        setMetricsAuthConfig(server, false);
        waitForMetricsEndpoint(server);
        MetricsConnection authenticationFalse = MetricsConnection.publicConnection(server);
        //Thread.sleep(5000);
        authenticationFalse.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();

        //4. When mpMetrics authentication option is removed from the server.xml,
        // the default metrics endpoint requires authentication

        setMetricsAuthConfig(server, null);
        waitForMetricsEndpoint(server);
        MetricsConnection authenticationRemoved = MetricsConnection.privateConnection(server);
        authenticationRemoved.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();
    }

}
