/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health31.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.exception.TopologyException;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ConfigAdminHealthCheckTest {

    final static String SERVER_NAME = "ConfigAdminDropinsCheck";
    final static String SERVER_NAME2 = "ConfigAdminXmlCheck";
    final static String SERVER_NAME3 = "ConfigAdminWrongAppCheck";
    final static String FAILS_TO_START_SERVER_NAME = "FailedConfigAdminApplicationStateHealthCheck";

    private static final String MESSAGE_LOG = "logs/messages.log";

    private static final String[] EXPECTED_FAILURES = { "CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWMH0053W", "CWMMH0052W", "CWMMH0053W", "CWWKE1106W", "CWWKE1107W" };
    private static final String[] FAILS_TO_START_EXPECTED_FAILURES = { "CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWM*H0053W", "CWMMH0052W", "CWMMH0053W", "CWWKZ0060E",
                                                                       "CWWKZ0002E", "CWWKE1106W", "CWWKE1107W" };

    public static final String MULTIPLE_APP_NAME = "MultipleHealthCheckApp";
    public static final String DIFFERENT_APP_NAME = "DifferentApplicationNameHealthCheckApp";
    public static final String DELAYED_APP_NAME = "DelayedHealthCheckApp";
    public static final String FAILS_TO_START_APP_NAME = "FailsToStartHealthCheckApp";
    public static final String SUCCESSFUL_APP_NAME = "SuccessfulHealthCheckApp";

    public static final String APP_NAME = "ConfigAdminDropinsCheckApp";
    public static final String APP_NAME2 = "ConfigAdminXmlCheckApp";

    private final String HEALTH_ENDPOINT = "/health";
    private final String READY_ENDPOINT = "/health/ready";
    private final String LIVE_ENDPOINT = "/health/live";
    private final String STARTED_ENDPOINT = "/health/started";
    private final String APP_ENDPOINT = "/DelayedHealthCheckApp/DelayedServlet";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503; // Response when port is open but Application is not ready

    public static final int APP_STARTUP_TIMEOUT = 120 * 1000;

    private static enum HealthCheck {
        LIVE, READY, STARTED, HEALTH;
    }

    private static enum Status {
        SUCCESS, FAILURE;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction()
                                    .withID("mpHealth-3.0")
                                    .addFeature("mpHealth-3.0")
                                    .removeFeature("mpHealth-3.1")
                                    .forServers(SERVER_NAME))
                    .andWith(new FeatureReplacementAction()
                                    .withID("mpHealth-2.0")
                                    .addFeature("mpHealth-2.0")
                                    .removeFeature("mpHealth-3.0")
                                    .forServers(SERVER_NAME));

    @Server(SERVER_NAME)
    public static LibertyServer server1;

    @Server(SERVER_NAME2)
    public static LibertyServer server2;

    @Server(FAILS_TO_START_SERVER_NAME)
    public static LibertyServer server3;

    @Before
    public void setUp() throws Exception {
        server1.deleteAllDropinApplications();
        server2.deleteAllDropinApplications();
        server3.deleteAllDropinApplications();
    }

    @After
    public void cleanUp() throws Exception {
        if (server1.isStarted()) {
            server1.stopServer(EXPECTED_FAILURES);
        } else if (server2.isStarted()) {
            server2.stopServer(EXPECTED_FAILURES);
        } else if (server3.isStarted()) {
            server3.stopServer(FAILS_TO_START_EXPECTED_FAILURES);
        }

        server1.removeAllInstalledAppsForValidation();
        server2.removeAllInstalledAppsForValidation();
        server3.removeAllInstalledAppsForValidation();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Once the tests and repeated tests are completed, ensure the server
        // is fully stopped, in order to avoid conflicts with succeeding tests.
        server1.stopServer(EXPECTED_FAILURES);
        server2.stopServer(EXPECTED_FAILURES);
        server3.stopServer(FAILS_TO_START_EXPECTED_FAILURES);
    }

    /*
     * This test will start a server with the applications already loaded inside the dropins folder.
     * It will confirm that both the configAdmin and appTracker detect the application.
     */
    @Test
    public void testMatchingAppNamesDropinsTest() throws Exception {
        log("testMatchingAppNamesDropinsTest", "Deploying the ConfigAdmin App into the dropins directory.");
        loadServerAndApplication(server1, APP_NAME, "io.openliberty.microprofile.health31.config.admin.dropins.checks.app", false);

        //Hitting health endpoint to trigger configAdmin app registration.
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        getJSONPayload(conReady);

        String configAdminLine = server1.waitForStringInTrace(" configAdminAppName = ConfigAdminDropinsCheckApp");
        String stateMapLine = server1.waitForStringInTrace(": appName = ConfigAdminDropinsCheckApp");

        assertNotNull("App was not detected by ConfigAdmin.", configAdminLine);
        assertNotNull("App was not detected by appTracker.", stateMapLine);

    }

    /*
     * This test will start a server without the applications preload in the dropins folder.
     * It will then dynamically load the applications in the dropins folder.
     * It will confirm that the configAdmin does not detect the application and that the appTracker does detect the application.
     */
    @Test
    public void testAppDetectionDropinsTest() throws Exception {

        log("testAppDetectionDropinsTest", "Starting the server and dynamically adding " + APP_NAME2);

        loadServerAndApplication(server1, APP_NAME, "io.openliberty.microprofile.health31.config.admin.dropins.checks.app", true);

        server1.waitForStringInLog("CWWKT0016I: Web application available");

        //Hitting health endpoint to trigger configAdmin app registration.
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        getJSONPayload(conReady);

        String configAdminLine = server1.waitForStringInTrace("configAdminAppName = ConfigAdminDropinsCheckApp", 10000);
        String stateMapLine = server1.waitForStringInTrace(": appName = ConfigAdminDropinsCheckApp");

        assertNull("App was detected by ConfigAdmin.", configAdminLine);
        assertNotNull("App was not detected by appTracker.", stateMapLine);

    }

    /*
     * This test will start a server with the application configured in the server.xml.
     * It will confirm that both the configAdmin and appTracker detect the application.
     */
    @Test
    public void testAppDetectionServerXml() throws Exception {
        log("testMatchingAppNamesDropinsTest", "Deploying the ConfigAdmin App into the apps directory.");

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME2, "io.openliberty.microprofile.health31.config.admin.xml.checks.app");
        ShrinkHelper.exportAppToServer(server2, app);

        if (!server2.isStarted())
            server2.startServer();

        server2.waitForStringInLog("CWWKT0016I: Web application available");

        //Hitting health endpoint to trigger configAdmin app registration.
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        getJSONPayload(conReady);

        String configAdminLine = server2.waitForStringInTrace("configAdminAppName = ConfigAdminXmlCheckApp");
        String stateMapLine = server2.waitForStringInTrace(": appName = ConfigAdminXmlCheckApp,");

        assertNotNull("App was not detected by ConfigAdmin.", configAdminLine);
        assertNotNull("App was not detected by appTracker.", stateMapLine);

    }

    /*
     * This test will start a server with the server.xml pointing at the wrong application.
     * It will confirm that both the configAdmin and appTracker do not detect the application.
     */
    @Test
    @SkipForRepeat({ "mpHealth-2.0", "mpHealth-3.0" })
    public void testWrongAppNameServerXml() throws Exception {
        log("testMatchingAppNamesDropinsTest", "Deploying the ConfigAdmin App into the apps directory.");

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME2, "io.openliberty.microprofile.health31.config.admin.xml.checks.app");
        ShrinkHelper.exportAppToServer(server2, app);

        if (!server2.isStarted())
            server2.startServer();

        server2.waitForStringInLog("CWWKT0016I: Web application available");

        //Hitting health endpoint to trigger configAdmin app registration.
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        getJSONPayload(conReady);

        String configAdminLine = server2.waitForStringInTrace("configAdminAppName = WrongAppNameCheckApp", 10000);
        String stateMapLine = server2.waitForStringInTrace(": appName = WrongAppNameCheckApp,", 10000);

        assertNull("App was not detected by ConfigAdmin.", configAdminLine);
        assertNull("App was not detected by appTracker.", stateMapLine);

    }

//Test will be enabled in a future update.
//    @Test
//    @SkipForRepeat({ "mpHealth-3.0", "mpHealth-3.1" })
//    @ExpectedFFDC({ "com.ibm.ws.container.service.state.StateChangeException",
//                    "com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterException",
//                    "org.jboss.weld.exceptions.DeploymentException" })
//    public void testFailsToStartApplicationHealthCheck() throws Exception {
//        log("testFailsToStartApplicationHealthCheckTest", "Pre-loading FailsToStartHealthCheckApp and starting the server");
//        loadServerAndApplication(server3, FAILS_TO_START_APP_NAME, "io.openliberty.microprofile.health31.fails.to.start.health.check.app", false);
//
//        log("testFailsToStartApplicationHealthCheckTest", "Testing health check endpoints after FailsToStartHealthCheckApp has been loaded");
//        expectHealthCheck(server3, HealthCheck.LIVE, Status.SUCCESS, 0);
//        expectFailsToStartApplicationNotStartedMessage(false);
//
//        expectHealthCheck(server3, HealthCheck.READY, Status.FAILURE, 0);
//        expectFailsToStartApplicationNotStartedMessage(true);
//
//        expectHealthCheck(server3, HealthCheck.HEALTH, Status.FAILURE, 0);
//        expectFailsToStartApplicationNotStartedMessage(true);
//
//        expectHealthCheck(server3, HealthCheck.STARTED, Status.FAILURE, 0);
//        expectFailsToStartApplicationNotStartedMessage(true);
//
//        String configAdminLine = server3.waitForStringInTrace("configAdminAppName = FailsToStartHealthCheckApp");
//        String stateMapLine = server3.waitForStringInTrace(": appName = FailsToStartHealthCheckApp");
//
//        assertNotNull("App was not detected by ConfigAdmin.", configAdminLine);
//        assertNotNull("App was not detected by appTracker.", stateMapLine);
//    }

    /*
     * This test will start a server with an ear file that contains two war files, one slow and one fast application.
     * The readiness endpoint will be continuously polled until it returns a 200 response code.
     * It will also confirm that both the configAdmin and appTracker detect the application, even with a delayed application.
     */
    @Test
    @SkipForRepeat({ "mpHealth-2.0", "mpHealth-3.0" })
    public void testReadinessEndpointOnServerStart() throws Exception {
        log("testReadinessEndpointOnServerStart", "Begin execution of testReadinessEndpointOnServerStart");

        class StartServerOnThread extends Thread {
            @Override
            public void run() {
                try {
                    WebArchive war1 = ShrinkHelper.buildDefaultApp(DELAYED_APP_NAME, "io.openliberty.microprofile.health31.delayed.health.check.app");
                    WebArchive war2 = ShrinkHelper.buildDefaultApp(APP_NAME, "io.openliberty.microprofile.health31.config.admin.dropins.checks.app");
                    EnterpriseArchive testEar = ShrinkWrap.create(EnterpriseArchive.class, "MultiWarApps.ear");
                    testEar.addAsModule(war2);
                    testEar.addAsModule(war1);

                    ShrinkHelper.exportDropinAppToServer(server1, testEar);
                    server1.startServer();
                } catch (Exception e) {
                    assertTrue("Failure to start server on a seperate thread.", server1.isStarted());
                }
            }
        }

        StartServerOnThread startServerThread;
        HttpURLConnection conReady = null;
        int num_of_attempts = 0;
        int max_num_of_attempts = 5;
        int responseCode = -1;
        long start_time = System.currentTimeMillis();
        long time_out = 180000; // 180000ms = 3min
        boolean connectionExceptionEncountered = false;
        boolean first_time = true;
        boolean app_ready = false;
        boolean repeat = true;
        boolean runTest = true;

        while (repeat) {
            Assume.assumeTrue(runTest); // Skip the test, if runTest is false.

            num_of_attempts += 1;

            // Need to ensure the server is not finish starting when readiness endpoint is hit so start the server on a separate thread
            // Note: this does not guarantee that we hit the endpoint during server startup, but it is highly likely that it will
            startServerThread = new StartServerOnThread();
            log("testReadinessEndpointOnServerStart", "Starting MultiWarApps server on separate thread.");
            startServerThread.start();

            try {
                conReady = null;
                responseCode = -1;
                connectionExceptionEncountered = false;
                first_time = true;
                app_ready = false;
                start_time = System.currentTimeMillis();

                // Repeatedly hit the readiness endpoint until a response of 200 is received
                while (!app_ready) {
                    try {
                        conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
                        responseCode = conReady.getResponseCode();
                    } catch (ConnectException ce) {
                        if (ce.getMessage().contains("Connection refused")) {
                            connectionExceptionEncountered = true;
                        }
                    } catch (SocketTimeoutException ste) {
                        log("testReadinessEndpointOnServerStart", "Encountered a SocketTimeoutException. Retrying connection. Exception: " + ste.getMessage());
                        continue;
                    } catch (SocketException se) {
                        log("testReadinessEndpointOnServerStart", "Encountered a SocketException. Retrying connection. Exception: " + se.getMessage());
                        continue;
                    }

                    // We need to ensure we get a connection refused in the case of the server not finished starting up
                    // We expect a connection refused as the ports are not open until server is fully started
                    if (first_time) {
                        log("testReadinessEndpointOnServerStart", "Testing the /health/ready endpoint as the server is still starting up.");
                        String message = "The connection was not refused as required, but instead completed with response code: " + responseCode +
                                         " This is likely due to a rare timing issue where the server starts faster than we can hit the readiness endpoint.";

                        if (conReady == null && connectionExceptionEncountered) {
                            first_time = false;
                        } else {
                            if (num_of_attempts == max_num_of_attempts) {
                                log("testReadinessEndpointOnServerStart",
                                    message + " Skipping test case due to multiple failed attempts in hitting the readiness endpoint faster than the server can start.");
                                startServerThread.join();
                                runTest = false; // Skip the test.
                                break;
                            }

                            log("testReadinessEndpointOnServerStart", message + " At this point the test will be re-run. Number of current attempts ---> " + num_of_attempts);
                            startServerThread.join();
                            cleanUp();
                            break; // We repeat the test case
                        }
                    } else {
                        if (responseCode == 200) {
                            log("testReadinessEndpointOnServerStart", "The /health/ready endpoint response code was 200.");
                            app_ready = true;
                            repeat = false;
                            startServerThread.join();
                        } else if (System.currentTimeMillis() - start_time > time_out) {
                            List<String> lines = server1.findStringsInFileInLibertyServerRoot("Application MultiWarApps started", MESSAGE_LOG);
                            if (lines.size() == 0) {
                                log("testReadinessEndpointOnServerStart", "Waiting for Application to start.");
                                String line = server1.waitForStringInLog("Application MultiWarApps started", 70000);
                                log("testReadinessEndpointOnServerStart", "Application started. Line Found : " + line);
                                assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);

                                String configAdminLine = server1.waitForStringInTrace("configAdminAppName = MultiWarApps");

                                assertNotNull("App was not detected by ConfigAdmin.", configAdminLine);
                            } else {
                                log("testReadinessEndpointOnServerStart", "Application started but timeout still reached.");
                                throw new TimeoutException("Timed out waiting for server and app to be ready. Timeout set to " + time_out + "ms.");
                            }
                        }
                    }

                }
            } catch (Exception e) {
                startServerThread.join();
                fail("Encountered an issue while Testing the /health/ready endpoint as the server and/or application(s) are starting up ---> " + e);
            }

        }

        log("testReadinessEndpointOnServerStart", "Waiting for Application to start message, after Health check reports 200.");
        String line = server1.waitForStringInLog("(CWWKZ0001I: Application MultiWarApps started)+", 60000);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);
        log("testReadinessEndpointOnServerStart", "Application Started message found: " + line);

        // Access an application endpoint to verify the application is actually ready
        log("testReadinessEndpointOnServerStart", "Testing an application endpoint, after server and application has started.");
        conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, APP_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conReady.getURL().toString(), SUCCESS_RESPONSE_CODE,
                     conReady.getResponseCode());
    }

    /**
     * Helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(ConfigAdminHealthCheckTest.class, method, msg);
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

    private void loadServerAndApplication(LibertyServer server, String appName, String packageName, boolean isDynamicallyLoaded) throws Exception {
        loadServerAndApplications(server, appName, packageName, isDynamicallyLoaded);
    }

    private void loadServerAndApplications(LibertyServer server, String appName, String packageName, boolean isDynamicallyLoaded) throws Exception {
        if (isDynamicallyLoaded) {
            startServer(server, true);
        }

        log("loadApplications", "Adding " + appName + " to dropins");
        addApplication(server, appName, packageName);

        if (!isDynamicallyLoaded) {
            startServer(server, true);
        }

        waitForApplication(server, appName);

    }

    private void addApplication(LibertyServer server, String appName, String packageName) throws Exception {
        log("addApplication", "Adding " + appName + " to the server");
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, packageName);

        if (appName.equals(FAILS_TO_START_APP_NAME)) {
            File libsDir = new File("lib/LibertyFATTestFiles/libs");
            for (File file : libsDir.listFiles()) {
                server.copyFileToLibertyServerRoot(file.getParent(), "kafkaLib", file.getName());
            }
            //Don't validate that FAILS_TO_START_APP_NAME starts correctly.
            ShrinkHelper.exportAppToServer(server, app, DeployOptions.DISABLE_VALIDATION);
        } else {
            ShrinkHelper.exportDropinAppToServer(server, app);
        }
    }

    private void waitForApplication(LibertyServer server, String appName) {
        log("waitForApplication", "Waiting for " + appName + " to start");
        server.waitForStringInLog("CWWKZ0001I.* " + appName, APP_STARTUP_TIMEOUT);
    }

    public void expectHealthCheck(LibertyServer server, HealthCheck expectedHealthCheck, Status expectedStatus, int expectedChecks) throws Exception {
        log("expectHealthCheck", "Testing Health Check endpoint " + expectedHealthCheck.toString() + " ...");
        HttpURLConnection con = null;
        int numOfAttempts = 0;
        int maxAttempts = 5;

        while (numOfAttempts < maxAttempts) {
            try {
                if (expectedHealthCheck == HealthCheck.LIVE) {
                    con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, LIVE_ENDPOINT);
                    break;
                } else if (expectedHealthCheck == HealthCheck.READY) {
                    con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, READY_ENDPOINT);
                    break;
                } else if (expectedHealthCheck == HealthCheck.STARTED) {
                    con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, STARTED_ENDPOINT);
                    break;
                } else {
                    con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, HEALTH_ENDPOINT);
                    break;
                }
            } catch (SocketTimeoutException ste) {
                log("expectHealthCheck", "Encountered a SocketTimeoutException. Retrying connection. Exception: " + ste.getMessage());
                numOfAttempts++;
                continue;
            } catch (SocketException se) {
                log("expectHealthCheck", "Encountered a SocketException. Retrying connection. Exception: " + se.getMessage());
                numOfAttempts++;
                continue;
            } catch (Exception e) {
                fail("Encountered an issue while testing the " + expectedHealthCheck.toString() + " health endpoint ---> " + e.getMessage());
            }
        }

        if (numOfAttempts == maxAttempts) {
            log("expectHealthCheck",
                "Reached maximum number of attempts, skipping connection test for " + expectedHealthCheck.toString()
                                     + " endpoint as the connection could not be established, due to a timing issue, that causes the connection to not be available.");
            return;
        }

        JsonObject jsonResponse = getJSONPayload(con);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        if (expectedStatus == Status.SUCCESS) {
            assertEquals("The response code of the health check was not " + SUCCESS_RESPONSE_CODE + ".", SUCCESS_RESPONSE_CODE, con.getResponseCode());
            assertEquals("The status of the health check was not UP.", "UP", jsonResponse.getString("status"));
        } else {
            assertEquals("The response code of the health check was not " + FAILED_RESPONSE_CODE + ".", FAILED_RESPONSE_CODE, con.getResponseCode());
            assertEquals("The status of the health check was not DOWN.", "DOWN", jsonResponse.getString("status"));
        }

        assertEquals("The number of expected checks was not " + expectedChecks + "." + checks.toString(), expectedChecks, checks.size());

    }

    private void expectFailsToStartApplicationNotStartedMessage(boolean expectMessage) throws Exception {
        if (expectMessage) {
            List<String> notStartedMessages = server3.findStringsInLogs("CWM*H0053W");
            assertTrue("The CWM*H0053W message for " + FAILS_TO_START_APP_NAME + " was not found in the logs.",
                       notStartedMessages.size() == 1 && notStartedMessages.get(0).contains(FAILS_TO_START_APP_NAME));
        } else {
            assertEquals("The CWM*H0053W message for " + FAILS_TO_START_APP_NAME + " was found in the logs.",
                         0, server3.findStringsInLogs("CWM*H0053W").size());
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
}
