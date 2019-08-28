/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.util.List;

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
@Mode(TestMode.FULL)
public class DelayAppStartupHealthCheckTest {

    public static final String APP_NAME = "DelayedHealthCheckApp";
    private static final String MESSAGE_LOG = "logs/messages.log";

    private final String HEALTH_ENDPOINT = "/health";
    private final String READY_ENDPOINT = "/health/ready";
    private final String LIVE_ENDPOINT = "/health/live";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503;

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
        server1.stopServer("CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWMH0053W");
    }

    @Test
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
