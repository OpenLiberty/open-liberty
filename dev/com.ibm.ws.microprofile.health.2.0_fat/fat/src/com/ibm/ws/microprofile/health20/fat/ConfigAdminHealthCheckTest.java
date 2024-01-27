/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.health20.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
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
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
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
    private static final String MESSAGE_LOG = "logs/messages.log";

    private static final String[] EXPECTED_FAILURES = { "CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWMH0053W", "CWMMH0052W", "CWMMH0053W", "CWWKE1106W", "CWWKE1107W" };

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
    private final String APP_ENDPOINT = "/DelayedHealthCheckApp/DelayedServlet";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503; // Response when port is open but Application is not ready

    public static final int APP_STARTUP_TIMEOUT = 120 * 1000;

    private static enum HealthCheck {
        LIVE, READY, HEALTH;
    }

    private static enum Status {
        SUCCESS, FAILURE;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction()
                                    .withID("mpHealth-3.0")
                                    .addFeature("mpHealth-3.0")
                                    .removeFeature("mpHealth-2.0")
                                    .forServers(SERVER_NAME))
                    .andWith(new FeatureReplacementAction()
                                    .withID("mpHealth-3.1")
                                    .addFeature("mpHealth-3.1")
                                    .removeFeature("mpHealth-3.0")
                                    .forServers(SERVER_NAME));

    @Server(SERVER_NAME)
    public static LibertyServer server1;

    @Server(SERVER_NAME2)
    public static LibertyServer server2;

    @Before
    public void setUp() throws Exception {
        server1.deleteAllDropinApplications();
    }

    @After
    public void cleanUp() throws Exception {
        if (server1.isStarted()) {
            server1.stopServer(EXPECTED_FAILURES);
        } else if (server2.isStarted()) {
            server2.stopServer(EXPECTED_FAILURES);
        }

        server1.removeAllInstalledAppsForValidation();
        server2.removeAllInstalledAppsForValidation();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Once the tests and repeated tests are completed, ensure the server
        // is fully stopped, in order to avoid conflicts with succeeding tests.
        server1.stopServer(EXPECTED_FAILURES);
        server2.stopServer(EXPECTED_FAILURES);
    }

    @Test
    public void testMatchingAppNamesDropinsTest() throws Exception {
        log("testMatchingAppNamesDropinsTest", "Deploying the ConfigAdmin App into the dropins directory.");
        loadServerAndApplication(server1, APP_NAME, "com.ibm.ws.microprofile.health20.config.admin.dropins.checks.app", false);

        //Hitting health endpoint to trigger configAdmin app registration.
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        getJSONPayload(conReady);

        String configAdminLine = server1.waitForStringInTrace(": configAdminAppName = ConfigAdminDropinsCheckApp");
        String stateMapLine = server1.waitForStringInTrace(": appName = ConfigAdminDropinsCheckApp,");

        assertNotNull("App was not detected by ConfigAdmin.", configAdminLine);
        assertNotNull("App was not detected by appTracker.", stateMapLine);

    }

    @Test
    public void testAppDetectionDropinsTest() throws Exception {

        log("testAppDetectionDropinsTest", "Starting the server and dynamically adding " + APP_NAME2);

        loadServerAndApplication(server1, APP_NAME, "com.ibm.ws.microprofile.health20.config.admin.dropins.checks.app", true);

        server1.waitForStringInLog("CWWKT0016I: Web application available");

        //Hitting health endpoint to trigger configAdmin app registration.
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        getJSONPayload(conReady);

        String configAdminLine = server1.waitForStringInTrace(": configAdminAppName = ConfigAdminDropinsCheckApp");
        String stateMapLine = server1.waitForStringInTrace(": appName = ConfigAdminDropinsCheckApp,");

        assertNull("App was detected by ConfigAdmin.", configAdminLine);
        assertNotNull("App was not detected by appTracker.", stateMapLine);

    }

    @Test
    public void testAppDetectionServerXml() throws Exception {
        log("testMatchingAppNamesDropinsTest", "Deploying the ConfigAdmin App into the apps directory.");

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME2, "com.ibm.ws.microprofile.health20.config.admin.xml.checks.app");
        ShrinkHelper.exportAppToServer(server2, app);

        if (!server2.isStarted())
            server2.startServer();

        server2.waitForStringInLog("CWWKT0016I: Web application available");

        //Hitting health endpoint to trigger configAdmin app registration.
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        getJSONPayload(conReady);

        String configAdminLine = server2.waitForStringInTrace(": configAdminAppName = ConfigAdminXmlCheckApp");
        String stateMapLine = server2.waitForStringInTrace(": appName = ConfigAdminXmlCheckApp,");

        assertNotNull("App was not detected by ConfigAdmin.", configAdminLine);
        assertNotNull("App was not detected by appTracker.", stateMapLine);

    }

    @Test
    public void testWrongAppNameServerXml() throws Exception {
        log("testMatchingAppNamesDropinsTest", "Deploying the ConfigAdmin App into the apps directory.");

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME2, "com.ibm.ws.microprofile.health20.config.admin.xml.checks.app");
        ShrinkHelper.exportAppToServer(server2, app);

        if (!server2.isStarted())
            server2.startServer();

        server2.waitForStringInLog("CWWKT0016I: Web application available");

        //Hitting health endpoint to trigger configAdmin app registration.
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        getJSONPayload(conReady);

        String configAdminLine = server2.waitForStringInTrace(": configAdminAppName = WrongAppNameCheckApp");
        String stateMapLine = server2.waitForStringInTrace(": appName = WrongAppNameCheckApp,");

        assertNull("App was not detected by ConfigAdmin.", configAdminLine);
        assertNull("App was not detected by appTracker.", stateMapLine);

    }

    @Test
    public void testReadinessEndpointOnServerStart() throws Exception {
        log("testReadinessEndpointOnServerStart", "Begin execution of testReadinessEndpointOnServerStart");

        class StartServerOnThread extends Thread {
            @Override
            public void run() {
                try {
                    WebArchive war1 = ShrinkHelper.buildDefaultApp(DELAYED_APP_NAME, "com.ibm.ws.microprofile.health20.delayed.health.check.app");
                    WebArchive war2 = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.microprofile.health20.config.admin.dropins.checks.app");
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
                            log("testReadinessEndpointOnServerStart", "Helllooooo ######");
                            List<String> lines = server1.findStringsInFileInLibertyServerRoot("Application MultiWarApps started", MESSAGE_LOG);
                            if (lines.size() == 0) {
                                log("testReadinessEndpointOnServerStart", "Waiting for Application to start.");
                                String line = server1.waitForStringInLog("Application MultiWarApps started", 70000);
                                log("testReadinessEndpointOnServerStart", "Application started. Line Found : " + line);
                                assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);
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
        loadServerAndApplications(server, Arrays.asList(appName), Arrays.asList(packageName), isDynamicallyLoaded);
    }

    private void loadServerAndApplications(LibertyServer server, List<String> appNames, List<String> packageNames, boolean isDynamicallyLoaded) throws Exception {
        if (isDynamicallyLoaded) {
            startServer(server, appNames.contains(SERVER_NAME2));
        }

        for (int i = 0; i < appNames.size(); i++) {
            log("loadApplications", "Adding " + appNames.get(i) + " to dropins");
            addApplication(server, appNames.get(i), packageNames.get(i));
        }

        if (!isDynamicallyLoaded) {
            startServer(server, appNames.contains(APP_NAME));
        }

        for (int i = 0; i < appNames.size(); i++) {
            waitForApplication(server, appNames.get(i));
        }
    }

    private void addApplication(LibertyServer server, String appName, String packageName) throws Exception {
        log("addApplication", "Adding " + appName + " to the server");
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, packageName);
        ShrinkHelper.exportDropinAppToServer(server, app);
    }

    private void waitForApplication(LibertyServer server, String appName) {
        log("waitForApplication", "Waiting for " + appName + " to start");
        server.waitForStringInLog("CWWKZ0001I.* " + appName, APP_STARTUP_TIMEOUT);
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
