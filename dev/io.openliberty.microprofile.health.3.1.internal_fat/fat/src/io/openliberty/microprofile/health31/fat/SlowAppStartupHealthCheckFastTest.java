/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

@RunWith(FATRunner.class)
public class SlowAppStartupHealthCheckFastTest {

    private static final String[] EXPECTED_FAILURES = { "CWWKE1102W", "CWWKE1105W", "CWMMH0052W", "CWMMH0054W", "SRVE0302E" };

    public static final String APP_NAME = "DelayedHealthCheckAppFast";
    private static final String MESSAGE_LOG = "logs/messages.log";

    private final String STARTED_ENDPOINT = "/health/started";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503; // Response when port is open but Application is not started

    private final String APP_ENDPOINT = "/" + APP_NAME + "/DelayedServlet";
    final static String SERVER_NAME = "SlowAppStartupHealthCheckFast";

    @Server(SERVER_NAME)
    public static LibertyServer server1;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
                                                             MicroProfileActions.MP70_EE10, // mpHealth-4.0 LITE
                                                             MicroProfileActions.MP70_EE11, // mpHealth-4.0 FULL
                                                             HealthActions.MP41_MPHEALTH40, // mpHealth-4.0 FULL w/ MP41 EE8
                                                             HealthActions.MP14_MPHEALTH40, // mpHealth-4.0 FULL w/ MP14 EE7
                                                             MicroProfileActions.MP41); // mpHealth-3.1 FULL

    public void setupClass(LibertyServer server, String testName) throws Exception {
        log("setupClass", testName + "Starting the server.");

        if (!server.isStarted())
            server.startServer(false, false);

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
    }

    private void deployApp(LibertyServer server, String testName) throws Exception {
        log("deployApp", testName + " - Deploying the Delayed App into the apps directory");
        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "io.openliberty.microprofile.health31.delayed.health.check.fast.app");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY);

        String line = server.waitForStringInLogUsingMark("CWWKT0016I: Web application available.*" + APP_NAME + "*");
        log("deployApp - " + testName, "Web Application available message found?: " + line);
        assertNotNull("The CWWKT0016I Web Application available message did not appear in messages.log", line);
    }

    @After
    public void cleanUp() throws Exception {
        log("cleanUp", " - Stopping the server, if servers are started.");

        if ((server1 != null) && (server1.isStarted()))
            server1.stopServer(EXPECTED_FAILURES);

        boolean flag = server1.removeDropinsApplications(APP_NAME + ".war");
        log("cleanUp", " - Removed the app? [" + flag + "]");
    }

    /*
     * Test the Startup end point, as soon as the server starts. The startup end point will be continuously polled, until it returns a
     * 200 response code, once all the deployed applications have started. This test mimics how the Kubernetes Startup probe work.
     */
    @Test
    public void testStartupEndpointOnServerStart() throws Exception {
        setupClass(server1, "testStartupEndpointOnServerStart");
        deployApp(server1, "testStartupEndpointOnServerStart");
        log("testReadinessEndpointOnServerStart", "Begin execution of testReadinessEndpointOnServerStart");
        server1.setMarkToEndOfLog();
        server1.stopServer(EXPECTED_FAILURES);

        class StartServerOnThread extends Thread {
            @Override
            public void run() {
                try {
                    server1.startServer();
                } catch (Exception e) {
                    assertTrue("Failure to start server on a seperate thread.", server1.isStarted());
                }
            }
        }

        StartServerOnThread startServerThread;
        HttpURLConnection conStarted = null;
        int num_of_attempts = 0;
        int max_num_of_attempts = 5;
        int responseCode = -1;
        long start_time = System.currentTimeMillis();
        long time_out = 240000; // 240000ms = 4min
        boolean connectionExceptionEncountered = false;
        boolean first_time = true;
        boolean app_started = false;
        boolean repeat = true;
        boolean runTest = true;

        while (repeat) {
            Assume.assumeTrue(runTest); // Skip the test, if runTest is false.

            num_of_attempts += 1;

            // Need to ensure the server is not finish starting when startup endpoint is hit so start the server on a separate thread
            // Note: this does not guarantee that we hit the endpoint during server startup, but it is highly likely that it will
            startServerThread = new StartServerOnThread();
            log("testStartupEndpointOnServerStart", "Starting DelayedHealthCheck server on separate thread.");
            startServerThread.start();

            try {
                conStarted = null;
                responseCode = -1;
                connectionExceptionEncountered = false;
                first_time = true;
                app_started = false;
                start_time = System.currentTimeMillis();

                // Repeatedly hit the readiness endpoint until a response of 200 is received
                while (!app_started) {
                    try {
                        conStarted = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, STARTED_ENDPOINT);
                        responseCode = conStarted.getResponseCode();
                    } catch (ConnectException ce) {
                        if (ce.getMessage().contains("Connection refused")) {
                            connectionExceptionEncountered = true;
                        }
                    } catch (SocketTimeoutException ste) {
                        log("testStartupEndpointOnServerStart", "Encountered a SocketTimeoutException. Retrying connection. Exception: " + ste.getMessage());
                        continue;
                    } catch (SocketException se) {
                        log("testStartupEndpointOnServerStart", "Encountered a SocketException. Retrying connection. Exception: " + se.getMessage());
                        continue;
                    }

                    // We need to ensure we get a connection refused in the case of the server not finished starting up
                    // We expect a connection refused as the ports are not open until server is fully started
                    if (first_time) {
                        log("testStartupEndpointOnServerStart", "Testing the /health/started endpoint as the server is still starting up.");
                        String message = "The connection was not refused as required, but instead completed with response code: " + responseCode +
                                         " This is likely due to a rare timing issue where the server starts faster than we can hit the startup endpoint.";

                        if (conStarted == null && connectionExceptionEncountered) {
                            first_time = false;
                        } else {
                            if (num_of_attempts == max_num_of_attempts) {
                                log("testStartupEndpointOnServerStart",
                                    message + " Skipping test case due to multiple failed attempts in hitting the startup endpoint faster than the server can start.");
                                startServerThread.join();
                                runTest = false; // Skip the test.
                                break;
                            }

                            log("testStartupEndpointOnServerStart", message + " At this point the test will be re-run. Number of current attempts ---> " + num_of_attempts);
                            startServerThread.join();
                            cleanUp();
                            break; // We repeat the test case
                        }
                    } else {
                        if (responseCode == 200) {
                            log("testStartupEndpointOnServerStart", "The /health/started endpoint response code was 200.");
                            app_started = true;
                            repeat = false;
                            startServerThread.join();
                        } else if (System.currentTimeMillis() - start_time > time_out) {
                            List<String> lines = server1.findStringsInFileInLibertyServerRoot("(CWWKZ0001I: Application " + APP_NAME + " started)+", MESSAGE_LOG);
                            if (lines.size() == 0) {
                                log("testStartupEndpointOnServerStart", "Waiting for Application to start.");
                                String line = server1.waitForStringInLog("(CWWKZ0001I: Application " + APP_NAME + " started)+", time_out);
                                log("testStartupEndpointOnServerStart", "Application started. Line Found : " + line);
                                assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);
                            } else {
                                log("testReadinessEndpointOnServerStart", "Application started but timeout still reached.");
                                throw new TimeoutException("Timed out waiting for server and app to be started. Timeout set to " + time_out + "ms.");
                            }
                        }
                    }

                }
            } catch (Exception e) {
                startServerThread.join();
                fail("Encountered an issue while Testing the /health/started endpoint as the server and/or application(s) are starting up ---> " + e);
            }

        }

        log("testStartupEndpointOnServerStart", "Waiting for Application to start message, after Health check reports 200.");
        String line = server1.waitForStringInLog("(CWWKZ0001I: Application " + APP_NAME + " started)+", 60000);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);
        log("testSlowAppStartUpHealthCheck", "Application Started message found: " + line);

        // Access an application endpoint to verify the application is actually started
        log("testStartupEndpointOnServerStart", "Testing an application endpoint, after server and application has started.");
        conStarted = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, APP_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conStarted.getURL().toString(), SUCCESS_RESPONSE_CODE,
                     conStarted.getResponseCode());
        HttpUtils.findStringInUrl(server1, APP_ENDPOINT, "Testing Delayed Servlet initialization.");
    }

    /*
     * Tests the Startup endpoint with a slow starting application which ensures that the startup health check returns DOWN, when the application
     * is still starting up, and should return the status of the user-defined startup health checks.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSlowAppStartUpHealthCheck() throws Exception {
        setupClass(server1, "testSlowAppStartUpHealthCheck");
        deployApp(server1, "testStartupEndpointOnServerStart");
        log("testSlowAppStartUpHealthCheck", "Testing the /health/started endpoint, before application has started.");
        HttpURLConnection conStarted = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, STARTED_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conStarted.getURL().toString(), FAILED_RESPONSE_CODE, conStarted.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conStarted);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the Startup health check was not DOWN.", jsonResponse.getString("status"), "DOWN");

        List<String> lines = server1.findStringsInFileInLibertyServerRoot("CWMMH0054W:", MESSAGE_LOG);
        assertEquals("The CWMMH0054W warning did not appear in messages.log", 1, lines.size());

        String line = server1.waitForStringInLogUsingMark("(CWWKZ0001I: Application " + APP_NAME + " started)+", 60000);
        log("testSlowAppStartUpHealthCheck", "Application Started message found: " + line);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);

        log("testSlowAppStartUpHealthCheck", "Testing the /health/started endpoint, after application has started.");
        HttpURLConnection conStarted2 = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, STARTED_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conStarted2.getURL().toString(), SUCCESS_RESPONSE_CODE,
                     conStarted2.getResponseCode());

        JsonObject jsonResponse2 = getJSONPayload(conStarted2);
        JsonArray checks2 = (JsonArray) jsonResponse2.get("checks");
        assertEquals("The size of the JSON Readiness health check was not 1.", 1, checks2.size());
        assertEquals("The status of the Startup health check was not UP.", jsonResponse2.getString("status"), "UP");
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
        Log.info(SlowAppStartupHealthCheckFastTest.class, method, msg);
    }
}
