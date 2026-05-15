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
import com.ibm.websphere.simplicity.config.MPHealthElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
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
    private static final String ENV_VAR_NO_FILE_HEALTH_CHECK_SERVER_NAME = "EnableEndpointsEnvVarNoFileHealthCheckServer";
    private static final String INVALID_VALUE_SERVER_NAME = "EnableEndpointsInvalidValueServer";
    private static final String ENV_VAR_INVALID_VALUE_SERVER_NAME = "EnableEndpointsEnvVarInvalidValueServer";
    private static final String DYNAMIC_ENABLE_SERVER_NAME = "EnableEndpointsDynamicEnableServer";
    private static final String APP_NAME = "FileHealthCheckApp";
    private static final String[] IGNORED_FAILURES = { "CWMMH01013W", "CWMMH01014W", "CWMMH0052W", "CWMMH0054W", "CWMMH0053W", "CWMMH0050E", "CWWKG0011W", "CWWKG0081E", "CWWKG0083W" };
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
    public static LibertyServer disabledEndpointsServer;

    @Server(NO_FILE_HEALTH_CHECK_SERVER_NAME)
    public static LibertyServer noFileHealthCheckServer;

    @Server(ENV_VAR_SERVER_NAME)
    public static LibertyServer envVarServer;

    @Server(ENV_VAR_NO_FILE_HEALTH_CHECK_SERVER_NAME)
    public static LibertyServer envVarNoFileHealthCheckServer;

    @Server(INVALID_VALUE_SERVER_NAME)
    public static LibertyServer invalidValueServer;

    @Server(ENV_VAR_INVALID_VALUE_SERVER_NAME)
    public static LibertyServer envVarInvalidValueServer;

    @Server(DYNAMIC_ENABLE_SERVER_NAME)
    public static LibertyServer dynamicEnableServer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Deploy the test application to all servers
        ShrinkHelper.defaultDropinApp(disabledEndpointsServer, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");
        ShrinkHelper.defaultDropinApp(noFileHealthCheckServer, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");
        ShrinkHelper.defaultDropinApp(envVarServer, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");
        ShrinkHelper.defaultDropinApp(envVarNoFileHealthCheckServer, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");
        ShrinkHelper.defaultDropinApp(invalidValueServer, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");
        ShrinkHelper.defaultDropinApp(envVarInvalidValueServer, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");
        ShrinkHelper.defaultDropinApp(dynamicEnableServer, APP_NAME, "io.openliberty.microprofile.health.file.healthcheck.app");

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
        stopAllServers();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // Servers are already stopped by @After method after each test
        // No additional cleanup needed
    }

    private static void stopAllServers() throws Exception {
        stopServerIfStarted(disabledEndpointsServer);
        stopServerIfStarted(noFileHealthCheckServer);
        stopServerIfStarted(envVarServer);
        stopServerIfStarted(envVarNoFileHealthCheckServer);
        stopServerIfStarted(invalidValueServer);
        stopServerIfStarted(envVarInvalidValueServer);
        stopServerIfStarted(dynamicEnableServer);
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
        startServerAndWait(disabledEndpointsServer);

        File serverRootDir = new File(disabledEndpointsServer.getServerRoot());
        Log.info(getClass(), "testEnableEndpointsFalse", "Server root: " + serverRootDir.getAbsolutePath());

        verifyHealthFilesCreated(serverRootDir);
        verifyEndpointsAndWAB(disabledEndpointsServer, true, false);
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

    /**
     * Test warning message when MP_HEALTH_ENABLE_ENDPOINTS=false via ENV var but file-based health checks are not enabled.
     *
     * Verifies:
     * 1. Server starts successfully with MP_HEALTH_ENABLE_ENDPOINTS=false environment variable but no checkInterval/startupCheckInterval
     * 2. Warning message CWMMH01013W appears in logs
     * 3. HTTP health endpoints still work (return 200/503, not 404) since file-based health checks are disabled
     * 4. WAB initialization messages appear (endpoints are enabled despite ENV var)
     */
    @Test
    public void testEnableEndpointsViaEnvVarWithoutFileHealthCheck() throws Exception {
        // Set environment variable before starting server
        envVarNoFileHealthCheckServer.addEnvVar("MP_HEALTH_ENABLE_ENDPOINTS", "false");

        startServerAndWait(envVarNoFileHealthCheckServer);

        assertNotNull("Warning CWMMH01013W should appear when MP_HEALTH_ENABLE_ENDPOINTS=false without file-based health checks",
                      envVarNoFileHealthCheckServer.waitForStringInLog("CWMMH01013W"));

        verifyEndpointsAndWAB(envVarNoFileHealthCheckServer, false, true);
    }

    /**
     * Test invalid value for enableEndpoints server config attribute.
     *
     * Verifies:
     * 1. Server starts with invalid value "invalid" for enableEndpoints attribute
     * 2. Configuration warning CWWKG0011W appears in logs (invalid value)
     * 3. Configuration error CWWKG0081E appears in logs (validation failed)
     * 4. Server defaults to true (endpoints enabled)
     * 5. File-based health checks work (files created and updated)
     * 6. HTTP health endpoints work (return 200/503, not 404)
     * 7. WAB initialization messages appear (endpoints are enabled)
     */
    @Test
    public void testEnableEndpointsInvalidValue() throws Exception {
        startServerAndWait(invalidValueServer);

        // Verify configuration error messages appear
        assertNotNull("Warning CWWKG0011W should appear for invalid enableEndpoints value",
                      invalidValueServer.waitForStringInLog("CWWKG0011W"));
        assertNotNull("Error CWWKG0081E should appear for invalid enableEndpoints value",
                      invalidValueServer.waitForStringInLog("CWWKG0081E"));

        // Verify server defaults to true (endpoints enabled)
        File serverRootDir = new File(invalidValueServer.getServerRoot());
        Log.info(getClass(), "testEnableEndpointsInvalidValue", "Server root: " + serverRootDir.getAbsolutePath());

        verifyHealthFilesCreated(serverRootDir);
        verifyEndpointsAndWAB(invalidValueServer, false, true);
    }

    /**
     * Test invalid environment variable value for MP_HEALTH_ENABLE_ENDPOINTS.
     *
     * Verifies:
     * 1. Warning message CWMMH01014W appears for invalid ENV variable value
     * 2. Server defaults to true (endpoints enabled) when ENV var is invalid
     * 3. File-based health checks work correctly
     */
    @Test
    public void testEnableEndpointsEnvVarInvalidValue() throws Exception {
        // Set invalid environment variable value before starting server
        envVarInvalidValueServer.addEnvVar("MP_HEALTH_ENABLE_ENDPOINTS", "invalid_value");

        startServerAndWait(envVarInvalidValueServer);

        // Verify warning message appears for invalid ENV variable value
        assertNotNull("Warning CWMMH01014W should appear for invalid MP_HEALTH_ENABLE_ENDPOINTS value",
                      envVarInvalidValueServer.waitForStringInLog("CWMMH01014W"));

        // Verify server defaults to true (endpoints enabled)
        File serverRootDir = new File(envVarInvalidValueServer.getServerRoot());
        Log.info(getClass(), "testEnableEndpointsEnvVarInvalidValue", "Server root: " + serverRootDir.getAbsolutePath());

        verifyHealthFilesCreated(serverRootDir);
        verifyEndpointsAndWAB(envVarInvalidValueServer, false, true);
    }

    /**
     * Test dynamic configuration update: enable to disable.
     *
     * Verifies:
     * 1. Server starts with enableEndpoints=true (endpoints enabled)
     * 2. Dynamically update to enableEndpoints=false
     * 3. Endpoints become disabled (return 404)
     * 4. File-based health checks continue to work
     */
    @Test
    public void testDynamicUpdateEnableToDisable() throws Exception {
        startServerAndWait(dynamicEnableServer);

        File serverRootDir = new File(dynamicEnableServer.getServerRoot());
        Log.info(getClass(), "testDynamicUpdateEnableToDisable", "Server root: " + serverRootDir.getAbsolutePath());

        // Verify initial state: endpoints enabled
        verifyHealthFilesCreated(serverRootDir);
        verifyEndpointsAndWAB(dynamicEnableServer, false, true);

        // Dynamically update configuration to disable endpoints
        dynamicEnableServer.setMarkToEndOfLog();
        ServerConfiguration config = dynamicEnableServer.getServerConfiguration();
        MPHealthElement mpHealth = config.getMPHealthElement();
        mpHealth.setEnableEndpoints("false");
        dynamicEnableServer.updateServerConfiguration(config);
        dynamicEnableServer.waitForConfigUpdateInLogUsingMark(null);

        // Verify endpoints are now disabled
        verifyEndpointsAndWAB(dynamicEnableServer, true, false);
    }

    /**
     * Test dynamic configuration update: disable to enable.
     *
     * Verifies:
     * 1. Server starts with enableEndpoints=false (endpoints disabled)
     * 2. Dynamically update to enableEndpoints=true
     * 3. Endpoints become enabled (return 200/503)
     * 4. File-based health checks continue to work
     */
    @Test
    public void testDynamicUpdateDisableToEnable() throws Exception {
        // Start with enableEndpoints=false (using the disabledEndpointsServer configuration)
        startServerAndWait(disabledEndpointsServer);

        File serverRootDir = new File(disabledEndpointsServer.getServerRoot());
        Log.info(getClass(), "testDynamicUpdateDisableToEnable", "Server root: " + serverRootDir.getAbsolutePath());

        // Verify initial state: endpoints disabled
        verifyHealthFilesCreated(serverRootDir);
        verifyEndpointsAndWAB(disabledEndpointsServer, true, false);

        // Dynamically update configuration to enable endpoints
        disabledEndpointsServer.setMarkToEndOfLog();
        ServerConfiguration config = disabledEndpointsServer.getServerConfiguration();
        MPHealthElement mpHealth = config.getMPHealthElement();
        mpHealth.setEnableEndpoints("true");
        disabledEndpointsServer.updateServerConfiguration(config);
        disabledEndpointsServer.waitForConfigUpdateInLogUsingMark(null);

        // Verify endpoints are now enabled
        verifyEndpointsAndWAB(disabledEndpointsServer, false, true);
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
