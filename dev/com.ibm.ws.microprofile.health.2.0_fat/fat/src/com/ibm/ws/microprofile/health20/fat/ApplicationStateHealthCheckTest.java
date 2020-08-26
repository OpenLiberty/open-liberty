/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health20.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.KafkaContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.exception.TopologyException;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ApplicationStateHealthCheckTest {
    private static final String[] EXPECTED_FAILURES = { "CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWMH0053W", "CWMMH0052W", "CWMMH0053W" };
    private static final String[] FAILS_TO_START_EXPECTED_FAILURES = { "CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWM*H0053W", "CWMMH0052W", "CWMMH0053W", "CWWKZ0060E", "CWWKZ0002E" };

    public static final String MULTIPLE_APP_NAME = "MultipleHealthCheckApp";
    public static final String DIFFERENT_APP_NAME = "DifferentApplicationNameHealthCheckApp";
    public static final String DELAYED_APP_NAME = "DelayedHealthCheckApp";
    public static final String FAILS_TO_START_APP_NAME = "FailsToStartHealthCheckApp";
    public static final String SUCCESSFUL_APP_NAME = "SuccessfulHealthCheckApp";

    private final String HEALTH_ENDPOINT = "/health";
    private final String READY_ENDPOINT = "/health/ready";
    private final String LIVE_ENDPOINT = "/health/live";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503; // Response when port is open but Application is not ready

    public static final int APP_STARTUP_TIMEOUT = 120 * 1000;

    private static enum HealthCheck {
        LIVE, READY, HEALTH;
    }

    private static enum Status {
        SUCCESS, FAILURE;
    }

    public static KafkaContainer kafkaContainer = new KafkaContainer();

    final static String SERVER_NAME = "ApplicationStateHealthCheck";
    final static String FAILS_TO_START_SERVER_NAME = "FailedApplicationStateHealthCheck";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction()
                                    .withID("mpHealth-3.0")
                                    .addFeature("mpHealth-3.0")
                                    .removeFeature("mpHealth-2.0")
                                    .forServers(SERVER_NAME, FAILS_TO_START_SERVER_NAME));

    @Server(SERVER_NAME)
    public static LibertyServer server1;

    @Server(FAILS_TO_START_SERVER_NAME)
    public static LibertyServer server2;

    @Before
    public void setUp() throws Exception {
        server1.deleteAllDropinApplications();
        server2.deleteAllDropinApplications();
    }

    @After
    public void cleanUp() throws Exception {
        server1.removeAllInstalledAppsForValidation();
        server2.removeAllInstalledAppsForValidation();
        if (server1.isStarted()) {
            server1.stopServer(EXPECTED_FAILURES);
        }
        if (server2.isStarted()) {
            server2.stopServer(FAILS_TO_START_EXPECTED_FAILURES);
        }
        
    }

    /**
     * This test will first load an application that purposely fails to start.
     * It will then load a dropin that would like to reports UP on all health checks.
     * But since the pre-loaded app failed to start, readiness/overall reports DOWN and liveness remains unaltered.
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.ws.container.service.state.StateChangeException",
                    "com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterException",
                    "org.jboss.weld.exceptions.DeploymentException" })
    public void testFailsToStartApplicationHealthCheckTest() throws Exception {
        log("testFailsToStartApplicationHealthCheckTest", "Pre-loading FailsToStartHealthCheckApp and starting the server");
        loadServerAndApplication(server2, FAILS_TO_START_APP_NAME, "com.ibm.ws.microprofile.health20.fails.to.start.health.check.app", false);

        log("testFailsToStartApplicationHealthCheckTest", "Testing health check endpoints after FailsToStartHealthCheckApp has been loaded");
        expectHealthCheck(server2, HealthCheck.LIVE, Status.SUCCESS, 0);
        expectFailsToStartApplicationNotStartedMessage(false);

        expectHealthCheck(server2, HealthCheck.READY, Status.FAILURE, 0);
        expectFailsToStartApplicationNotStartedMessage(true);

        expectHealthCheck(server2, HealthCheck.HEALTH, Status.FAILURE, 0);
        expectFailsToStartApplicationNotStartedMessage(true);

        log("testFailsToStartApplicationHealthCheckTest", "Adding SuccessfulHealthCheckApp to dropins");
        addApplication(server2, SUCCESSFUL_APP_NAME, "com.ibm.ws.microprofile.health20.successful.health.checks.app");
        waitForApplication(server2, SUCCESSFUL_APP_NAME);

        log("testFailsToStartApplicationHealthCheckTest", "Testing health check endpoints after SuccessfulHealthCheckApp has been loaded");
        expectHealthCheck(server2, HealthCheck.LIVE, Status.SUCCESS, 1);
        expectHealthCheck(server2, HealthCheck.READY, Status.FAILURE, 1);
        expectHealthCheck(server2, HealthCheck.HEALTH, Status.FAILURE, 2);
    }

    /**
     * This test ensures that health checks adjust when dropins are loaded before server start.
     */
    @Test
    public void testPreLoadedApplicationsHealthCheckTest() throws Exception {
        log("testPreLoadedApplicationsHealthCheckTest", "Pre-loading " + SUCCESSFUL_APP_NAME + " and " + MULTIPLE_APP_NAME + " and starting the server");
        loadServerAndApplications(server1, Arrays.asList(SUCCESSFUL_APP_NAME, MULTIPLE_APP_NAME),
                                  Arrays.asList("com.ibm.ws.microprofile.health20.successful.health.checks.app",
                                                "com.ibm.ws.microprofile.health20.multiple.health.checks.app"),
                                  false);

        log("testPreLoadedApplicationsHealthCheckTest", "Testing health check endpoints after " + SUCCESSFUL_APP_NAME + " and " + MULTIPLE_APP_NAME + " have been loaded");
        expectHealthCheck(server1, HealthCheck.LIVE, Status.FAILURE, 3);
        expectHealthCheck(server1, HealthCheck.READY, Status.SUCCESS, 3);
        expectHealthCheck(server1, HealthCheck.HEALTH, Status.FAILURE, 6);
    }

    /**
     * This test ensures that health checks adjust when dropins are loaded after server start.
     * It also tests that readiness reports DOWN when an app is in the middle of starting up.
     * Implementation borrowed from DelayAppStartupHealthCheckTest.
     */
    @Test
    public void testDynamicallyLoadedApplicationsHealthCheckTest() throws Exception {
        log("testDynamicallyLoadedApplicationsHealthCheckTest", "Starting the server and dynamically adding " + SUCCESSFUL_APP_NAME);
        loadServerAndApplication(server1, SUCCESSFUL_APP_NAME, "com.ibm.ws.microprofile.health20.successful.health.checks.app", true);

        log("testDynamicallyLoadedApplicationsHealthCheckTest", "Testing health check endpoints after " + SUCCESSFUL_APP_NAME + " is dynamically deployed");
        expectHealthCheck(server1, HealthCheck.LIVE, Status.SUCCESS, 1);
        expectHealthCheck(server1, HealthCheck.READY, Status.SUCCESS, 1);
        expectHealthCheck(server1, HealthCheck.HEALTH, Status.SUCCESS, 2);

        log("testDynamicallyLoadedApplicationsHealthCheckTest", "Adding " + DELAYED_APP_NAME + " to dropins");
        addApplication(server1, DELAYED_APP_NAME, "com.ibm.ws.microprofile.health20.delayed.health.check.app");

        log("testDynamicallyLoadedApplicationsHealthCheckTest", "Testing for readiness DOWN while " + DELAYED_APP_NAME + " is starting.");
        try {
            HttpURLConnection conReady = null;
            int responseCode = -1;
            boolean first_time = true;
            boolean app_ready = false;
            long start_time = System.currentTimeMillis();
            long time_out = 180000; // 180000ms = 3min

            // Repeatedly hit the readiness endpoint until an UP response is received
            while (!app_ready) {
                conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
                responseCode = conReady.getResponseCode();

                // We need to ensure we get a connection refused in the case of the server not finished starting up
                // We expect a connection refused as the ports are not open until server is fully started
                if (first_time) {
                    log("testDynamicallyLoadedApplicationsHealthCheckTest", "Testing the /health/ready endpoint as DelayedHealthCheckApp is still starting up.");
                    String message = "The connection did not dip with a response code " + FAILED_RESPONSE_CODE + " as required, instead it received code " + responseCode +
                                     ". This is likely due to a rare timing issue where the server starts faster than we can hit the readiness endpoint.";
                    assertTrue(message, conReady != null && responseCode == FAILED_RESPONSE_CODE);
                    first_time = false;
                } else {
                    if (responseCode == SUCCESS_RESPONSE_CODE) {
                        app_ready = true;
                    } else if (System.currentTimeMillis() - start_time > time_out) {
                        throw new TimeoutException("Timed out waiting for server and app to be ready. Timeout set to " + time_out + "ms.");
                    }
                }

            }
        } catch (Exception e) {
            fail("Encountered an issue while Testing the /health/ready endpoint as the server and/or application(s) are starting up ---> " + e);
        }

        log("testDynamicallyLoadedApplicationsHealthCheckTest", "Testing health check endpoints after " + SUCCESSFUL_APP_NAME + " and " + DELAYED_APP_NAME + " have started");
        expectHealthCheck(server1, HealthCheck.LIVE, Status.SUCCESS, 2);
        expectHealthCheck(server1, HealthCheck.READY, Status.SUCCESS, 2);
        expectHealthCheck(server1, HealthCheck.HEALTH, Status.SUCCESS, 4);
    }

    private void expectFailsToStartApplicationNotStartedMessage(boolean expectMessage) throws Exception {
        if (expectMessage) {
            List<String> notStartedMessages = server2.findStringsInLogs("CWM*H0053W");
            assertTrue("The CWM*H0053W message for " + FAILS_TO_START_APP_NAME + " was not found in the logs.",
                       notStartedMessages.size() == 1 && notStartedMessages.get(0).contains(FAILS_TO_START_APP_NAME));
        } else {
            assertEquals("The CWM*H0053W message for " + FAILS_TO_START_APP_NAME + " was found in the logs.",
                         0, server2.findStringsInLogs("CWM*H0053W").size());
        }
    }

    private void loadServerAndApplication(LibertyServer server, String appName, String packageName, boolean isDynamicallyLoaded) throws Exception {
        loadServerAndApplications(server, Arrays.asList(appName), Arrays.asList(packageName), isDynamicallyLoaded);
    }

    private void loadServerAndApplications(LibertyServer server, List<String> appNames, List<String> packageNames, boolean isDynamicallyLoaded) throws Exception {
        if (isDynamicallyLoaded) {
            startServer(server, appNames.contains(FAILS_TO_START_APP_NAME));
        }

        for (int i = 0; i < appNames.size(); i++) {
            log("loadApplications", "Adding " + appNames.get(i) + " to dropins");
            addApplication(server, appNames.get(i), packageNames.get(i));
        }

        if (!isDynamicallyLoaded) {
            startServer(server, appNames.contains(FAILS_TO_START_APP_NAME));
        }

        for (int i = 0; i < appNames.size(); i++) {
            waitForApplication(server, appNames.get(i));
        }
    }

    private void addApplication(LibertyServer server, String appName, String packageName) throws Exception {
        log("addApplication", "Adding " + appName + " to the server");
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, packageName);
        if (appName.equals(FAILS_TO_START_APP_NAME)) {
            app = app.addAsManifestResource(ApplicationStateHealthCheckTest.class.getResource("permissions.xml"), "permissions.xml");
            File libsDir = new File("lib/LibertyFATTestFiles/libs");
            for (File file : libsDir.listFiles()) {
                server.copyFileToLibertyServerRoot(file.getParent(), "kafkaLib", file.getName());
            }
            ShrinkHelper.exportAppToServer(server, app);
        } else {
            ShrinkHelper.exportDropinAppToServer(server, app);
        }

    }

    private void waitForApplication(LibertyServer server, String appName) {
        if (appName.equals(FAILS_TO_START_APP_NAME)) {
            log("waitForApplication", "Waiting for expected app failure");
            server.waitForStringInLog("CWWKZ0012I.* " + FAILS_TO_START_APP_NAME, APP_STARTUP_TIMEOUT);
            log("waitForApplication", "Waiting for expected FFDC");
            server.waitForMultipleStringsInLog(3, "FFDC1015I");
        } else {
            log("waitForApplication", "Waiting for " + appName + " to start");
            server.waitForStringInLog("CWWKZ0001I.* " + appName, APP_STARTUP_TIMEOUT);
        }
    }

    private void startServer(LibertyServer server, boolean isFailsToStartApp) throws Exception {
        log("loadApplication", "Starting the server");
        if (isFailsToStartApp) {
            try {
                server.startServer();
            } catch (TopologyException e) {
            }
        } else {
            server.startServer();
        }
    }

    public void expectHealthCheck(LibertyServer server, HealthCheck expectedHealthCheck, Status expectedStatus, int expectedChecks) throws Exception {
        HttpURLConnection con;
        if (expectedHealthCheck == HealthCheck.LIVE) {
            con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, LIVE_ENDPOINT);
        } else if (expectedHealthCheck == HealthCheck.READY) {
            con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, READY_ENDPOINT);
        } else {
            con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, HEALTH_ENDPOINT);
        }

        JsonObject jsonResponse = getJSONPayload(con);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals("The number of expected checks was not " + expectedChecks + ".", expectedChecks, checks.size());

        if (expectedStatus == Status.SUCCESS) {
            assertEquals("The response code of the health check was not " + SUCCESS_RESPONSE_CODE + ".", SUCCESS_RESPONSE_CODE, con.getResponseCode());
            assertEquals("The status of the health check was not UP.", "UP", jsonResponse.getString("status"));
        } else {
            assertEquals("The response code of the health check was not " + FAILED_RESPONSE_CODE + ".", FAILED_RESPONSE_CODE, con.getResponseCode());
            assertEquals("The status of the health check was not DOWN.", "DOWN", jsonResponse.getString("status"));
        }
    }

    public JsonObject getJSONPayload(HttpURLConnection con) throws Exception {
        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));

        BufferedReader br = HttpUtils.getResponseBody(con, "UTF-8");
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();

        log("getJSONPayload", "Response: jsonResponse= " + jsonResponse.toString());
        assertNotNull("The contents of the health endpoint must not be null.", jsonResponse.getString("status"));

        return jsonResponse;
    }

    /**
     * Helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(ApplicationStateHealthCheckTest.class, method, msg);
    }
}