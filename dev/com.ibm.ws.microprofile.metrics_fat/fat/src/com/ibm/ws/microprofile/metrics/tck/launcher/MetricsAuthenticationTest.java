/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

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
        addFeature("mpMetrics-1.0");
        testMetricsAuth();
    }

    @Test
    public void testMetrics11Auth() throws Exception {
        addFeature("mpMetrics-1.1");
        testMetricsAuth();
    }

    private void addFeature(String feature) throws Exception {
        server.setMarkToEndOfLog();
        MetricsAuthTestUtil.addFeature(server, feature);
        waitForMetricsEndpoint(server);
    }

    private void waitForMetricsEndpoint(LibertyServer server) throws Exception {
        // by default waitForStringInLogUsingMark will look at the default log when a log is not specified
        assertNotNull("Web application is not available at */metrics/", server.waitForStringInLogUsingMark("CWWKT0016I.*/metrics/"));
    }

    private void waitForMetricsFeature(LibertyServer server) throws Exception {
        assertNotNull("[/metrics] failed to initialize", server.waitForStringInLogUsingMark("SRVE0242I.*/metrics.*"));
    }

    private void waitForSecurityPrerequisites(LibertyServer server) throws Exception {
        assertNotNull("LTPA keys are not created/ready within timeout period of " + TIMEOUT + "ms.", server.waitForStringInLog("CWWKS4104A.*|CWWKS4105I.*", TIMEOUT));
        assertNotNull("TCP Channel defaultHttpEndpoint has not started", server.waitForStringInLog("CWWKO0219I.*defaultHttpEndpoint"));
        assertNotNull("TCP Channel defaultHttpEndpoint-ssl has not started", server.waitForStringInLog("CWWKO0219I.*defaultHttpEndpoint-ssl"));
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

        // wait for metrics installations to complete before running test
        waitForMetricsFeature(server);
        // wait for TCP Channels to start listening
        waitForSecurityPrerequisites(server);

        //1. When authentication is not explicitly set in server.xml, it defaults to private,
        //  i.e. requires authentication into metrics endpoint
        privateAsserts();

        //2. When authentication is explicitly set to true in server.xml, metrics endpoint requires authentication,
        //  i.e. is private
        setMetricsAuthConfig(server, true);
        privateAsserts();

        //3. When authentication is explicitly set to false in server.xml, metrics endpoint does not require authentication,
        // i.e. is public
        setMetricsAuthConfig(server, false);
        waitForMetricsEndpoint(server);
        publicAsserts();

        //4. When mpMetrics authentication option is removed from the server.xml,
        // the default metrics endpoint requires authentication
        setMetricsAuthConfig(server, null);
        waitForMetricsEndpoint(server);
        privateAsserts();
    }

    /**
     * Make assertions to verify that the public metrics endpoint allows unauthenticated access.
     */
    private static void publicAsserts() throws Exception {
        // Authentication is NOT required.
        MetricsConnection.connection_unauthenticated(server).expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();
        MetricsConnection.connection_unauthenticated(server).method(HTTPRequestMethod.OPTIONS).header("accept", "application/json")
                        .expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();
    }

    /**
     * Make assertions to verify that the private metrics endpoint restricts access to authenticated/authorized users.
     */
    private static void privateAsserts() throws Exception {
        // check that when the authenticated user is in the Administrator role, authorization is granted
        MetricsConnection.connection_administratorRole(server).expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();
        MetricsConnection.connection_administratorRole(server).method(HTTPRequestMethod.OPTIONS).header("accept", "application/json")
                        .expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();

        // check that when the authenticated user is in the Reader role, authorization is granted
        MetricsConnection.connection_readerRole(server).expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();
        MetricsConnection.connection_readerRole(server).method(HTTPRequestMethod.OPTIONS).header("accept", "application/json")
                        .expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();

        // check that when the valid user is not in an authorized role, 403 FORBIDDEN is returned
        MetricsConnection.connection_unauthorized(server).expectedResponseCode(HttpURLConnection.HTTP_FORBIDDEN).getConnection();
        MetricsConnection.connection_unauthorized(server).method(HTTPRequestMethod.OPTIONS).header("accept", "application/json")
                        .expectedResponseCode(HttpURLConnection.HTTP_FORBIDDEN).getConnection();

        //check that opening connection to /metrics with default authentication with invalid credentials returns HTTP 401 ERROR
        MetricsConnection.connection_invalidUser(server).expectedResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED).getConnection();
        MetricsConnection.connection_invalidUser(server).method(HTTPRequestMethod.OPTIONS).header("accept", "application/json")
                        .expectedResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED).getConnection();
    }
}
