/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat.mp;

import static io.openliberty.checkpoint.fat.mp.FATSuite.configureEnvVariable;
import static io.openliberty.checkpoint.fat.mp.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.mp.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.health.internal_fat.shared.HealthFileUtils;

/**
 * Test enableEndpoints configuration with checkpoint/restore and file-based health checks.
 * 
 * Verifies that:
 * 1. enableEndpoints=false disables HTTP endpoints after restore while file-based health checks work
 * 2. enableEndpoints=true enables HTTP endpoints after restore with file-based health checks
 * 3. MP_HEALTH_ENABLE_ENDPOINTS=false ENV var disables endpoints after restore
 * 4. MP_HEALTH_ENABLE_ENDPOINTS=true ENV var enables endpoints after restore
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class MPHealthTestFileBasedEnableEndpoints extends FATServletClient {

    @Server("checkpointMPHealthFileBasedEnableEndpointsFalse")
    public static LibertyServer serverEnableEndpointsFalse;

    @Server("checkpointMPHealthFileBasedEnableEndpointsTrue")
    public static LibertyServer serverEnableEndpointsTrue;

    @Server("checkpointMPHealthFileBasedEnvVarFalse")
    public static LibertyServer serverEnvVarFalse;

    @Server("checkpointMPHealthFileBasedEnvVarTrue")
    public static LibertyServer serverEnvVarTrue;

    private static final String APP_NAME = "mphealthup";
    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String[] HEALTH_ENDPOINTS = { "/health", "/health/ready", "/health/live", "/health/started" };
    private static final String[] ENDPOINT_NAMES = { "Health", "Ready", "Live", "Started" };

    // WAB initialization message pattern to verify endpoints are NOT initialized when disabled
    private static final String WAB_PATTERN = "(Loading Web Module: health|" +
                                              "Web Module health has been bound to|" +
                                              "Web application available.*health|" +
                                              "HealthCheckReadinessServlet.*Initialization successful|" +
                                              "HealthCheckServlet.*Initialization successful|" +
                                              "HealthCheckStartupServlet.*Initialization successful|" +
                                              "HealthCheckLivenessServlet.*Initialization successful)";

    public TestMethod testMethod;
    private LibertyServer currentServer;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.MPHealthFileBasedRepeat(FeatureReplacementAction.ALL_SERVERS);

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        // Deploy the mphealthup app to all servers
        ShrinkHelper.defaultApp(serverEnableEndpointsFalse, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(serverEnableEndpointsFalse, APP_NAME);

        ShrinkHelper.defaultApp(serverEnableEndpointsTrue, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(serverEnableEndpointsTrue, APP_NAME);

        ShrinkHelper.defaultApp(serverEnvVarFalse, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(serverEnvVarFalse, APP_NAME);

        ShrinkHelper.defaultApp(serverEnvVarTrue, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(serverEnvVarTrue, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);

        // Select the appropriate server based on test method
        currentServer = getServerForTest();

        currentServer.setCheckpoint(getCheckpointPhase(), true,
                                     server -> {
                                         configureAndTestBeforeRestore();
                                     });
        currentServer.setConsoleLogName(getTestMethod(TestMethod.class, testName) + ".log");
        currentServer.startServer(true, false); // Do not validate apps since we have a delayed startup.
    }

    private LibertyServer getServerForTest() {
        switch (testMethod) {
            case testEnableEndpointsFalseWithFileBasedHealthChecks:
                return serverEnableEndpointsFalse;
            case testEnableEndpointsTrueWithFileBasedHealthChecks:
                return serverEnableEndpointsTrue;
            case testEnableEndpointsEnvVarFalseWithFileBasedHealthChecks:
                return serverEnvVarFalse;
            case testEnableEndpointsEnvVarTrueWithFileBasedHealthChecks:
                return serverEnvVarTrue;
            default:
                return serverEnableEndpointsFalse;
        }
    }

    private CheckpointPhase getCheckpointPhase() {
        // All tests use AFTER_APP_START phase
        return CheckpointPhase.AFTER_APP_START;
    }

    /**
     * Test that endpoints are disabled (enableEndpoints=false) after checkpoint restore
     * when file-based health checks are enabled.
     * 
     * Expected behavior:
     * - Before restore: No health files, no endpoints
     * - After restore: Health files created, endpoints return 404 (disabled)
     */
    @Test
    public void testEnableEndpointsFalseWithFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);
        String serverRoot = currentServer.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        // Ensure application has started
        List<String> lines = currentServer.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Verifying that file-based health check files are present after restore");

        // Verify health files are created (file-based health checks work)
        assertTrue("All health check files should be created", HealthFileUtils.isFilesCreated(serverRootDirFile));
        assertTrue("Health directory should exist", HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        assertTrue("Started file should exist", HealthFileUtils.getStartFile(serverRootDirFile).exists());
        assertTrue("Live file should exist", HealthFileUtils.getLiveFile(serverRootDirFile).exists());
        assertTrue("Ready file should exist", HealthFileUtils.getReadyFile(serverRootDirFile).exists());

        Log.info(getClass(), name, "Verifying that HTTP endpoints are disabled (return 404)");

        // Verify endpoints are disabled (return 404)
        verifyEndpointsDisabled(currentServer);

        // Verify WAB messages don't appear (endpoints disabled)
        String wabMessage = currentServer.waitForStringInLog(WAB_PATTERN, 5000);
        assertNull("WAB messages should NOT appear when endpoints disabled", wabMessage);
    }

    /**
     * Test that endpoints are enabled (enableEndpoints=true) after checkpoint restore
     * when file-based health checks are enabled.
     * 
     * Expected behavior:
     * - Before restore: No health files, no endpoints
     * - After restore: Health files created, endpoints return 200/503 (enabled)
     */
    @Test
    public void testEnableEndpointsTrueWithFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);
        String serverRoot = currentServer.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        // Ensure application has started
        List<String> lines = currentServer.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Verifying that file-based health check files are present after restore");

        // Verify health files are created (file-based health checks work)
        assertTrue("All health check files should be created", HealthFileUtils.isFilesCreated(serverRootDirFile));
        assertTrue("Health directory should exist", HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        assertTrue("Started file should exist", HealthFileUtils.getStartFile(serverRootDirFile).exists());
        assertTrue("Live file should exist", HealthFileUtils.getLiveFile(serverRootDirFile).exists());
        assertTrue("Ready file should exist", HealthFileUtils.getReadyFile(serverRootDirFile).exists());

        Log.info(getClass(), name, "Verifying that HTTP endpoints are enabled (return 200/503)");

        // Verify endpoints are enabled (return 200 or 503, not 404)
        verifyEndpointsEnabled(currentServer);
    }

    /**
     * Test that endpoints are disabled (ENV: MP_HEALTH_ENABLE_ENDPOINTS=false) after checkpoint restore
     * when file-based health checks are enabled.
     * 
     * Expected behavior:
     * - Before restore: No health files, no endpoints
     * - After restore: Health files created, endpoints return 404 (disabled)
     */
    @Test
    public void testEnableEndpointsEnvVarFalseWithFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);
        String serverRoot = currentServer.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        // Ensure application has started
        List<String> lines = currentServer.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Verifying that file-based health check files are present after restore");

        // Verify health files are created (file-based health checks work)
        assertTrue("All health check files should be created", HealthFileUtils.isFilesCreated(serverRootDirFile));
        assertTrue("Health directory should exist", HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        assertTrue("Started file should exist", HealthFileUtils.getStartFile(serverRootDirFile).exists());
        assertTrue("Live file should exist", HealthFileUtils.getLiveFile(serverRootDirFile).exists());
        assertTrue("Ready file should exist", HealthFileUtils.getReadyFile(serverRootDirFile).exists());

        Log.info(getClass(), name, "Verifying that HTTP endpoints are disabled via ENV var (return 404)");

        // Verify endpoints are disabled (return 404)
        verifyEndpointsDisabled(currentServer);

        // Verify WAB messages don't appear (endpoints disabled)
        String wabMessage = currentServer.waitForStringInLog(WAB_PATTERN, 5000);
        assertNull("WAB messages should NOT appear when endpoints disabled via ENV var", wabMessage);
    }

    /**
     * Test that endpoints are enabled (ENV: MP_HEALTH_ENABLE_ENDPOINTS=true) after checkpoint restore
     * when file-based health checks are enabled.
     * 
     * Expected behavior:
     * - Before restore: No health files, no endpoints
     * - After restore: Health files created, endpoints return 200/503 (enabled)
     */
    @Test
    public void testEnableEndpointsEnvVarTrueWithFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);
        String serverRoot = currentServer.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        // Ensure application has started
        List<String> lines = currentServer.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Verifying that file-based health check files are present after restore");

        // Verify health files are created (file-based health checks work)
        assertTrue("All health check files should be created", HealthFileUtils.isFilesCreated(serverRootDirFile));
        assertTrue("Health directory should exist", HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        assertTrue("Started file should exist", HealthFileUtils.getStartFile(serverRootDirFile).exists());
        assertTrue("Live file should exist", HealthFileUtils.getLiveFile(serverRootDirFile).exists());
        assertTrue("Ready file should exist", HealthFileUtils.getReadyFile(serverRootDirFile).exists());

        Log.info(getClass(), name, "Verifying that HTTP endpoints are enabled via ENV var (return 200/503)");

        // Verify endpoints are enabled (return 200 or 503, not 404)
        verifyEndpointsEnabled(currentServer);
    }

    /**
     * Configure environment variables and test state before restore.
     * This method runs during the checkpoint phase, before the restore happens.
     */
    private void configureAndTestBeforeRestore() {
        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring during restore: " + testMethod);

            switch (testMethod) {
                case testEnableEndpointsEnvVarFalseWithFileBasedHealthChecks:
                    Log.info(getClass(), testName.getMethodName(), "Setting MP_HEALTH_ENABLE_ENDPOINTS=false via ENV var");
                    Map<String, String> configFalse = new HashMap<>();
                    configFalse.put("MP_HEALTH_ENABLE_ENDPOINTS", "false");
                    configFalse.put("MP_HEALTH_CHECK_INTERVAL", "5s");
                    configFalse.put("MP_HEALTH_STARTUP_CHECK_INTERVAL", "1s");
                    configureEnvVariable(currentServer, configFalse);
                    break;

                case testEnableEndpointsEnvVarTrueWithFileBasedHealthChecks:
                    Log.info(getClass(), testName.getMethodName(), "Setting MP_HEALTH_ENABLE_ENDPOINTS=true via ENV var");
                    Map<String, String> configTrue = new HashMap<>();
                    configTrue.put("MP_HEALTH_ENABLE_ENDPOINTS", "true");
                    configTrue.put("MP_HEALTH_CHECK_INTERVAL", "5s");
                    configTrue.put("MP_HEALTH_STARTUP_CHECK_INTERVAL", "1s");
                    configureEnvVariable(currentServer, configTrue);
                    break;

                default:
                    Log.info(getClass(), testName.getMethodName(), "No ENV var configuration required for test: " + testMethod);
                    break;
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }

        Log.info(getClass(), getTestMethodNameOnly(testName), "Testing that health files do not exist before restore");

        /*
         * Before restore, we expect nothing to be created yet.
         * The checkpoint happens after app start, but file-based health checks
         * haven't run yet.
         * 
         * Expect:
         * [X] /health dir (created during checkpoint)
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         */

        String serverRoot = currentServer.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        assertTrue(HealthFileUtils.HEALTH_DIR_SHOULD_HAVE, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.STARTED_SHOULD_NOT_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.LIVE_SHOULD_NOT_HAVE, HealthFileUtils.getLiveFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.READY_SHOULD_NOT_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
    }

    /**
     * Verify that all health endpoints are disabled (return 404).
     */
    private void verifyEndpointsDisabled(LibertyServer server) throws Exception {
        for (int i = 0; i < HEALTH_ENDPOINTS.length; i++) {
            HttpURLConnection conn = HttpUtils.getHttpConnectionWithAnyResponseCode(server, HEALTH_ENDPOINTS[i]);
            int responseCode = conn.getResponseCode();
            assertEquals(ENDPOINT_NAMES[i] + " endpoint should return 404 when disabled",
                         HttpURLConnection.HTTP_NOT_FOUND, responseCode);
        }
    }

    /**
     * Verify that all health endpoints are enabled (return 200 or 503, not 404).
     */
    private void verifyEndpointsEnabled(LibertyServer server) throws Exception {
        for (int i = 0; i < HEALTH_ENDPOINTS.length; i++) {
            HttpURLConnection conn = HttpUtils.getHttpConnectionWithAnyResponseCode(server, HEALTH_ENDPOINTS[i]);
            int responseCode = conn.getResponseCode();
            assertTrue(ENDPOINT_NAMES[i] + " endpoint should NOT return 404, got: " + responseCode,
                       responseCode != HttpURLConnection.HTTP_NOT_FOUND);
            assertTrue(ENDPOINT_NAMES[i] + " endpoint should return 200 or 503, got: " + responseCode,
                       responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_UNAVAILABLE);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (currentServer != null && currentServer.isStarted()) {
            currentServer.stopServer("CWMMH0052W", "CWMMH0053W", "CWMMH0054W");
        }
    }

    static enum TestMethod {
        testEnableEndpointsFalseWithFileBasedHealthChecks,
        testEnableEndpointsTrueWithFileBasedHealthChecks,
        testEnableEndpointsEnvVarFalseWithFileBasedHealthChecks,
        testEnableEndpointsEnvVarTrueWithFileBasedHealthChecks,
        unknown
    }
}

