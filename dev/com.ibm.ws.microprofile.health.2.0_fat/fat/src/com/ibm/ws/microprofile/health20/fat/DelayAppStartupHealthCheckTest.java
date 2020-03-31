/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class DelayAppStartupHealthCheckTest {

    private static final String[] EXPECTED_FAILURES = { "CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWMH0053W" };

    public static final String APP_NAME = "DelayedHealthCheckApp";
    private static final String MESSAGE_LOG = "logs/messages.log";

    private final String HEALTH_ENDPOINT = "/health";
    private final String READY_ENDPOINT = "/health/ready";
    private final String LIVE_ENDPOINT = "/health/live";
    private final String APP_ENDPOINT = "/DelayedHealthCheckApp/DelayedServlet";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503; // Response when port is open but Application is not ready

    @Server("DelayedHealthCheck")
    public static LibertyServer server1;

    @Before
    public void deployApplicationAndStartServer() throws Exception {
        log("deployApplicatonIntoDropins", "Deploying the Delayed App into the dropins directory.");

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.microprofile.health20.delayed.health.check.app");
        ShrinkHelper.exportAppToServer(server1, app);

        if (!server1.isStarted())
            server1.startServer();

        server1.waitForStringInLog("CWWKT0016I: Web application available.*DelayedHealthCheckApp*");
    }

    @After
    public void cleanUp() throws Exception {
        server1.stopServer(EXPECTED_FAILURES);
    }

    @Test
    public void testReadinessEndpointOnServerStart() throws Exception {
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

        // Need to ensure the server is not finish starting when readiness endpoint is hit so start the server on a separate thread
        // Note: this does not guarantee that we hit the endpoint during server startup, but it is highly likely that it will
        StartServerOnThread startServerThread = new StartServerOnThread();
        log("testReadinessEndpointOnServerStart", "Starting DelayedHealthCheck server on separate thread.");
        startServerThread.start();

        try {
            HttpURLConnection conReady = null;
            int responseCode = -1;
            boolean connectionExceptionEncountered = false;
            boolean first_time = true;
            boolean app_ready = false;
            long start_time = System.currentTimeMillis();
            long time_out = 180000; // 180000ms = 3min

            // Repeatedly hit the readiness endpoint until a response of 200 is received
            while (!app_ready) {
                try {
                    conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
                    responseCode = conReady.getResponseCode();
                } catch (ConnectException e) {
                    if (e.getMessage().contains("Connection refused")) {
                        connectionExceptionEncountered = true;
                    }
                } catch (SocketTimeoutException exception) {
                    log("testReadinessEndpointOnServerStart", "Encountered a SocketTimeoutException. Retrying connection.");
                    continue;
                }

                // We need to ensure we get a connection refused in the case of the server not finished starting up
                // We expect a connection refused as the ports are not open until server is fully started
                if (first_time) {
                    log("testReadinessEndpointOnServerStart", "Testing the /health/ready endpoint as the server is still starting up.");
                    String failure_message = "The connection was not refused as required, but instead completed with response code: " + responseCode +
                                             " This is likely due to a rare timing issue where the server starts faster than we can hit the readiness endpoint."
                                             + "In that case there are no issues with the feature and this failure may be disregarded.";
                    assertTrue(failure_message, conReady == null && connectionExceptionEncountered);
                    first_time = false;
                } else {
                    if (responseCode == 200) {
                        app_ready = true;
                    } else if (System.currentTimeMillis() - start_time > time_out) {
                        throw new TimeoutException("Timed out waiting for server and app to be ready. Timeout set to " + time_out + "ms.");
                    }
                }

            }
        } catch (Exception e) {
            startServerThread.join();
            fail("Encountered an issue while Testing the /health/ready endpoint as the server and/or application(s) are starting up ---> " + e);
        }

        // Access an application endpoint to verify the application is actually ready
        log("testReadinessEndpointOnServerStart", "Testing an application endpoint, after server and application has started.");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, APP_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conReady.getURL().toString(), SUCCESS_RESPONSE_CODE,
                     conReady.getResponseCode());
        HttpUtils.findStringInUrl(server1, APP_ENDPOINT, "Testing Delayed Servlet initialization.");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testDelayedAppStartUpReadinessCheck() throws Exception {
        log("testDelayedAppStartUpHealthCheck", "Testing the /health/ready endpoint, before application has started.");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conReady.getURL().toString(), FAILED_RESPONSE_CODE, conReady.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the Readiness health check was not DOWN.", jsonResponse.getString("status"), "DOWN");

        server1.setMarkToEndOfLog();

        List<String> lines = server1.findStringsInFileInLibertyServerRoot("CWMH0053W:", MESSAGE_LOG);
        assertEquals("The CWMH0053W warning did not appear in messages.log", 1, lines.size());

        String line = server1.waitForStringInLogUsingMark("(CWWKZ0001I: Application DelayedHealthCheckApp started)+", 60000);
        log("testDelayedAppStartUpHealthCheck", "Application Started message found: " + line);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);

        log("testDelayedAppStartUpHealthCheck", "Testing the /health/ready endpoint, after application has started.");
        HttpURLConnection conReady2 = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conReady2.getURL().toString(), SUCCESS_RESPONSE_CODE,
                     conReady2.getResponseCode());

        JsonObject jsonResponse2 = getJSONPayload(conReady2);
        JsonArray checks2 = (JsonArray) jsonResponse2.get("checks");
        assertEquals("The size of the JSON Readiness health check was not 1.", 1, checks2.size());
        assertEquals("The status of the Readiness health check was not UP.", jsonResponse2.getString("status"), "UP");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testDelayedAppStartUpLivenessCheck() throws Exception {
        log("testDelayedAppStartUpLivenessCheck", "Testing the /health/live endpoint, before application has started.");
        HttpURLConnection conLive = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, LIVE_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conLive.getURL().toString(), SUCCESS_RESPONSE_CODE, conLive.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conLive);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the Liveness health check was not UP.", jsonResponse.getString("status"), "UP");

        server1.setMarkToEndOfLog();

        List<String> lines = server1.findStringsInFileInLibertyServerRoot("CWMH0053W:", MESSAGE_LOG);
        assertEquals("The CWMH0053W warning did not appear in messages.log", 0, lines.size());

        String line = server1.waitForStringInLogUsingMark("(CWWKZ0001I: Application DelayedHealthCheckApp started)+", 60000);
        log("testDelayedAppStartUpHealthCheck", "Application Started message found: " + line);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);

        log("testDelayedAppStartUpLivenessCheck", "Testing the /health/live endpoint, after application has started.");
        HttpURLConnection conLive2 = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, LIVE_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conLive2.getURL().toString(), SUCCESS_RESPONSE_CODE, conLive2.getResponseCode());

        JsonObject jsonResponse2 = getJSONPayload(conLive2);
        JsonArray checks2 = (JsonArray) jsonResponse2.get("checks");
        assertEquals("The size of the JSON Liveness health check was not 1.", 1, checks2.size());
        assertEquals("The status of the Liveness health check was not UP.", jsonResponse2.getString("status"), "UP");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testDelayedAppStartUpHealthCheck() throws Exception {
        log("testDelayedAppStartUpHealthCheck", "Testing the /health endpoint, before application has started.");
        HttpURLConnection conHealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, HEALTH_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conHealth.getURL().toString(), FAILED_RESPONSE_CODE, conHealth.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conHealth);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the overall health check was not DOWN.", jsonResponse.getString("status"), "DOWN");

        server1.setMarkToEndOfLog();

        List<String> lines = server1.findStringsInFileInLibertyServerRoot("CWMH0053W:", MESSAGE_LOG);
        assertEquals("The CWMH0053W warning did not appear in messages.log", 1, lines.size());

        String line = server1.waitForStringInLogUsingMark("(CWWKZ0001I: Application DelayedHealthCheckApp started)+", 60000);
        log("testDelayedAppStartUpHealthCheck", "Application Started message found: " + line);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);

        log("testDelayedAppStartUpHealthCheck", "Testing the /health endpoint, after application has started.");
        HttpURLConnection conHealth2 = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, HEALTH_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conHealth2.getURL().toString(), SUCCESS_RESPONSE_CODE, conHealth2.getResponseCode());

        JsonObject jsonResponse2 = getJSONPayload(conHealth2);
        JsonArray checks2 = (JsonArray) jsonResponse2.get("checks");
        assertEquals("The size of the JSON overall health check was not 2.", 2, checks2.size());
        assertEquals("The status of the overall health check was not UP.", jsonResponse2.getString("status"), "UP");
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
        Log.info(DelayAppStartupHealthCheckTest.class, method, msg);
    }
}
