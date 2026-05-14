/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.file.healthcheck.endpoints.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.HttpURLConnection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.health.file.healthcheck.endpoints.fat.utils.HealthFileUtils;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

/**
 * Test for enableEndpoints configuration attribute.
 * Verifies that health endpoints can be disabled while file-based health checks continue to work.
 */
@RunWith(FATRunner.class)
public class EnableEndpointsTest {

    private static final String SERVER_NAME = "EnableEndpointsServer";
    private static final String NO_FILE_HEALTH_CHECK_SERVER_NAME = "EnableEndpointsNoFileHealthCheckServer";
    private static final String ENV_VAR_SERVER_NAME = "EnableEndpointsEnvVarServer";
    private static final String APP_NAME = "FileHealthCheckApp";
    private static final String[] IGNORED_FAILURES = { "CWMMH01013W", "CWMMH0052W", "CWMMH0054W", "CWMMH0053W", "CWMMH0050E" };
    private static final String[] HEALTH_ENDPOINTS = { "/health", "/health/ready", "/health/live", "/health/started" };
    private static final String[] ENDPOINT_NAMES = { "Health", "Ready", "Live", "Started" };

    // WAB initialization message patterns to verify are NOT present when endpoints disabled
    private static final String WAB_PATTERN = "(Loading Web Module: health|" +
                                              "Web Module health has been bound to|" +
                                              "Web application available.*health|" +
                                              "HealthCheckReadinessServlet.*Initialization successful|" +
                                              "HealthCheckServlet.*Initialization successful|" +
                                              "HealthCheckStartupServlet.*Initialization successful|" +
                                              "HealthCheckLivenessServlet.*Initialization successful)";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS,
                                                             MicroProfileActions.MP61, // mpHealth-4.0 w/ EE9
                                                             MicroProfileActions.MP70_EE10, // mpHealth-4.0 FULL EE10
                                                             MicroProfileActions.MP70_EE11, // mpHealth-4.0 FULL EE11
                                                             HealthActions.MP14_MPHEALTH40, // mpHealth-4.0 FULL EE7
                                                             HealthActions.MP41_MPHEALTH40); //mpHealth-4.0 FULL EE8

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @Server(NO_FILE_HEALTH_CHECK_SERVER_NAME)
    public static LibertyServer noFileHealthCheckServer;

    @Server(ENV_VAR_SERVER_NAME)
    public static LibertyServer envVarServer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Deploy the test application to all servers
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");
        ShrinkHelper.defaultDropinApp(noFileHealthCheckServer, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");
        ShrinkHelper.defaultDropinApp(envVarServer, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");

        Log.info(EnableEndpointsTest.class, "beforeClass", "Test application deployed to all servers");
    }

    /**
     * Start a server and wait for it to be ready.
     *
     * @param testServer The server to start
     */
    private void startServerAndWait(LibertyServer testServer) throws Exception {
        testServer.startServer();
        testServer.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server should be started: " + testServer.getServerName(), testServer.isStarted());
    }

    @After
    public void afterTest() throws Exception {
        stopServerIfStarted(server);
        stopServerIfStarted(noFileHealthCheckServer);
        stopServerIfStarted(envVarServer);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        stopServerIfStarted(server);
        stopServerIfStarted(noFileHealthCheckServer);
        stopServerIfStarted(envVarServer);
    }

    private static void stopServerIfStarted(LibertyServer testServer) throws Exception {
        if (testServer != null && testServer.isStarted()) {
            testServer.stopServer(IGNORED_FAILURES);
        }
    }

    /**
     * Test basic functionality of enableEndpoints=false configuration.
     *
     * Verifies:
     * 1. Server starts successfully with enableEndpoints=false
     * 2. File-based health checks still work (files created and updated)
     * 3. HTTP health endpoints return 404 (disabled)
     */
    @Test
    public void testEnableEndpointsFalse() throws Exception {
        startServerAndWait(server);

        File serverRootDir = new File(server.getServerRoot());
        Log.info(getClass(), "testEnableEndpointsFalse", "Server root: " + serverRootDir.getAbsolutePath());

        verifyHealthFilesCreated(serverRootDir);
        verifyEndpointsAndWAB(server, true, false);
    }

    /**
     * Test warning message when enableEndpoints=false but file-based health checks are not enabled.
     *
     * Verifies:
     * 1. Server starts successfully with enableEndpoints=false but no checkInterval/startupCheckInterval
     * 2. Warning message CWMMH01013W appears in logs
     * 3. HTTP health endpoints still work (return 200/503, not 404) since file-based health checks are disabled
     * 4. WAB initialization messages appear (endpoints are enabled despite enableEndpoints=false)
     */
    @Test
    public void testEnableEndpointsFalseWithoutFileHealthCheck() throws Exception {
        startServerAndWait(noFileHealthCheckServer);

        assertNotNull("Warning CWMMH01013W should appear when enableEndpoints=false without file-based health checks",
                      noFileHealthCheckServer.waitForStringInLog("CWMMH01013W"));

        verifyEndpointsAndWAB(noFileHealthCheckServer, false, true);
    }

    /**
     * Test enableEndpoints configuration via environment variable.
     *
     * Verifies:
     * 1. Server starts successfully with MP_HEALTH_ENABLE_ENDPOINTS=false environment variable
     * 2. File-based health checks still work (files created and updated)
     * 3. HTTP health endpoints return 404 (disabled)
     * 4. Environment variable is properly read and processed
     */
    @Test
    public void testEnableEndpointsViaEnvVar() throws Exception {
        // Set environment variable before starting server
        envVarServer.addEnvVar("MP_HEALTH_ENABLE_ENDPOINTS", "false");

        startServerAndWait(envVarServer);

        File serverRootDir = new File(envVarServer.getServerRoot());
        Log.info(getClass(), "testEnableEndpointsViaEnvVar", "Server root: " + serverRootDir.getAbsolutePath());

        verifyHealthFilesCreated(serverRootDir);
        verifyEndpointsAndWAB(envVarServer, true, false);
    }

    private void verifyHealthFilesCreated(File serverRootDir) throws InterruptedException {
        assertTrue("All health check files should be created", FATSuite.isFilesCreated(serverRootDir));
        assertTrue("Health directory should exist", HealthFileUtils.getHealthDirFile(serverRootDir).exists());
        assertTrue("Started file should exist", HealthFileUtils.getStartFile(serverRootDir).exists());
        assertTrue("Live file should exist", HealthFileUtils.getLiveFile(serverRootDir).exists());
        assertTrue("Ready file should exist", HealthFileUtils.getReadyFile(serverRootDir).exists());
    }

    /**
     * Verify endpoint status and WAB messages based on whether endpoints should be disabled.
     *
     * @param testServer        The server to test
     * @param expectDisabled    true if endpoints should be disabled (404), false if enabled (200/503)
     * @param expectWABMessages true if WAB messages should appear, false if they should not
     */
    private void verifyEndpointsAndWAB(LibertyServer testServer, boolean expectDisabled, boolean expectWABMessages) throws Exception {
        verifyEndpointsStatus(testServer, expectDisabled);

        String wabMessage = testServer.waitForStringInLog(WAB_PATTERN);
        if (expectWABMessages) {
            assertNotNull("WAB messages should appear when endpoints enabled", wabMessage);
        } else {
            assertNull("WAB messages should not appear when endpoints disabled", wabMessage);
        }
    }

    private void verifyEndpointsStatus(LibertyServer testServer, boolean expectDisabled) throws Exception {
        for (int i = 0; i < HEALTH_ENDPOINTS.length; i++) {
            int responseCode = HttpUtils.getHttpConnectionWithAnyResponseCode(testServer, HEALTH_ENDPOINTS[i]).getResponseCode();

            if (expectDisabled) {
                assertEquals(ENDPOINT_NAMES[i] + " endpoint should return 404 when disabled",
                             HttpURLConnection.HTTP_NOT_FOUND, responseCode);
            } else {
                assertTrue(ENDPOINT_NAMES[i] + " endpoint should not return 404, got: " + responseCode,
                           responseCode != HttpURLConnection.HTTP_NOT_FOUND);
                assertTrue(ENDPOINT_NAMES[i] + " endpoint should return 200 or 503, got: " + responseCode,
                           responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_UNAVAILABLE);
            }
        }
    }

}

// Made with Bob
