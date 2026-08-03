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
import static io.openliberty.checkpoint.fat.mp.FATSuite.emptyEnvVariable;
import static io.openliberty.checkpoint.fat.mp.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.mp.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
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
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class MPHealthTestFileBasedEnableEndpoints extends FATServletClient {

    @Server("checkpointMPHealthFileBasedEnableEndpointsFalse")
    public static LibertyServer serverEnableEndpointsFalse;

    @Server("checkpointMPHealthFileBasedEnableEndpointsTrue")
    public static LibertyServer serverEnableEndpointsTrue;

    /*
     * Single server shared by both ENV var tests, matching the MPHealthTestFileBasedConfig pattern.
     * Using one server ensures OSGi static state (isOneAppStarted) is fully reset between tests
     * via stopServer/restoreServerConfiguration, so both tests get a clean restore callback.
     */
    @Server("checkpointMPHealthFileBasedEnvVarFalse")
    public static LibertyServer serverEnvVar;

    private static final String APP_NAME = "mphealthup";
    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String[] HEALTH_ENDPOINTS = { "/health", "/health/ready", "/health/live", "/health/started" };
    private static final String[] ENDPOINT_NAMES = { "Health", "Ready", "Live", "Started" };

    private LibertyServer currentServer;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.MPHealthFileBasedRepeat("checkpointMPHealthFileBasedEnvVarFalse");

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(serverEnableEndpointsFalse, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(serverEnableEndpointsFalse, APP_NAME);

        ShrinkHelper.defaultApp(serverEnableEndpointsTrue, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(serverEnableEndpointsTrue, APP_NAME);

        ShrinkHelper.defaultApp(serverEnvVar, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(serverEnvVar, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        TestMethod testMethod = getTestMethod(TestMethod.class, testName);
        currentServer = getServerForTest(testMethod);
        currentServer.saveServerConfiguration();
        currentServer.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                                    server -> {
                                        configureAndTestBeforeRestore(testMethod);
                                    });
        currentServer.setConsoleLogName(getTestMethod(TestMethod.class, testName) + ".log");
        currentServer.startServer(true, false); // Do not validate apps since we have a delayed startup.
    }

    private LibertyServer getServerForTest(TestMethod testMethod) {
        switch (testMethod) {
            case testEnableEndpointsFalseWithFileBasedHealthChecks:
                return serverEnableEndpointsFalse;
            case testEnableEndpointsTrueWithFileBasedHealthChecks:
                return serverEnableEndpointsTrue;
            case testEnableEndpointsEnvVarFalseWithFileBasedHealthChecks:
            case testEnableEndpointsEnvVarTrueWithFileBasedHealthChecks:
                return serverEnvVar;
            default:
                return serverEnableEndpointsFalse;
        }
    }

    /**
     * Test that endpoints are disabled (enableEndpoints=false in server.xml) after checkpoint restore
     * when file-based health checks are enabled.
     *
     * Expect after restore:
     * [X] /health dir
     * [X] Started
     * [X] Ready
     * [X] Live
     * HTTP endpoints: 404 (disabled)
     */
    @Test
    public void testEnableEndpointsFalseWithFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);

        List<String> lines = currentServer.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Test that HTTP endpoints are disabled (return 404)");

        /*
         * The objective of this test is to verify enableEndpoints=false via server.xml.
         * File creation is already covered by MPHealthTestFileBasedConfig.
         *
         * Expect HTTP endpoints: 404 (disabled)
         */
        verifyEndpointsDisabled(currentServer);
    }

    /**
     * Test that endpoints are enabled (enableEndpoints=true in server.xml) after checkpoint restore.
     */
    @Test
    public void testEnableEndpointsTrueWithFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);

        List<String> lines = currentServer.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Test that HTTP endpoints are enabled (return 200/503)");

        /*
         * The objective of this test is to verify enableEndpoints=true via server.xml.
         * File creation is already covered by MPHealthTestFileBasedConfig.
         *
         * Expect HTTP endpoints: 200/503 (enabled)
         */
        verifyEndpointsEnabled(currentServer);
    }

    /**
     * Test that endpoints are disabled (MP_HEALTH_ENABLE_ENDPOINTS=false via ENV var) after checkpoint restore
     * when file-based health checks are enabled via ENV vars.
     *
     * Expect after restore:
     * [X] /health dir
     * [X] Started
     * [X] Ready
     * [X] Live
     * HTTP endpoints: 404 (disabled)
     */
    @Test
    public void testEnableEndpointsEnvVarFalseWithFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);

        List<String> lines = currentServer.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Test that HTTP endpoints are disabled via ENV var (return 404)");

        /*
         * The objective of this test is to verify enableEndpoints=false via ENV var.
         * File creation is already covered by MPHealthTestFileBasedConfig.
         *
         * Expect HTTP endpoints: 404 (disabled)
         */
        verifyEndpointsDisabled(currentServer);
    }

    /**
     * Test that endpoints are enabled (MP_HEALTH_ENABLE_ENDPOINTS=true via ENV var) after checkpoint restore.
     */
    @Test
    public void testEnableEndpointsEnvVarTrueWithFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);

        List<String> lines = currentServer.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Test that HTTP endpoints are enabled via ENV var (return 200/503)");

        /*
         * The objective of this test is to verify enableEndpoints=true via ENV var.
         * File creation is already covered by MPHealthTestFileBasedConfig.
         *
         * Expect HTTP endpoints: 200/503 (enabled)
         */
        verifyEndpointsEnabled(currentServer);
    }

    private void configureAndTestBeforeRestore(TestMethod testMethod) {
        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring during restore: " + testMethod);
            switch (testMethod) {
                case testEnableEndpointsEnvVarFalseWithFileBasedHealthChecks:
                    Log.info(getClass(), testName.getMethodName(), "Adding server environment values for test: " + testMethod);
                    Map<String, String> configFalse = new HashMap<>();
                    configFalse.put("MP_HEALTH_ENABLE_ENDPOINTS", "false");
                    configureEnvVariable(currentServer, configFalse);
                    break;
                case testEnableEndpointsEnvVarTrueWithFileBasedHealthChecks:
                    Log.info(getClass(), testName.getMethodName(), "Adding server environment values for test: " + testMethod);
                    Map<String, String> configTrue = new HashMap<>();
                    configTrue.put("MP_HEALTH_ENABLE_ENDPOINTS", "true");
                    configureEnvVariable(currentServer, configTrue);
                    break;
                default:
                    /*
                     * Make sure server.env has no values in it.
                     */
                    emptyEnvVariable(currentServer);
                    Log.info(getClass(), testName.getMethodName(), "No configuration change required for test: " + testMethod);
                    break;
            }
        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }

        Log.info(getClass(), getTestMethodNameOnly(testName), "Testing that health files do not exist before restore");

        /*
         * This is a test before a restore.
         * All servers have checkInterval set in server.xml so health dir is created at checkpoint.
         * The individual health files are not created until after restore.
         *
         * Expect:
         * [X] /health dir
         * [ ] Started / Ready / Live
         */

        String serverRoot = currentServer.getServerRoot();

        assertTrue("/health dir should exist at checkpoint", new File(serverRoot, "health").exists());
    }

    private void verifyEndpointsDisabled(LibertyServer server) throws Exception {
        for (int i = 0; i < HEALTH_ENDPOINTS.length; i++) {
            HttpURLConnection conn = HttpUtils.getHttpConnectionWithAnyResponseCode(server, HEALTH_ENDPOINTS[i]);
            int responseCode = conn.getResponseCode();
            assertEquals(ENDPOINT_NAMES[i] + " endpoint should return 404 when disabled",
                         HttpURLConnection.HTTP_NOT_FOUND, responseCode);
        }
    }

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
        try {
            if (currentServer != null && currentServer.isStarted()) {
                currentServer.stopServer("CWMMH0052W", "CWMMH0053W", "CWMMH0054W");
            }
        } finally {
            if (currentServer != null) {
                currentServer.restoreServerConfiguration();
            }
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
