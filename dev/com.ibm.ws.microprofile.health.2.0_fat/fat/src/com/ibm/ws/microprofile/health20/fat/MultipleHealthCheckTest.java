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

import java.io.BufferedReader;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

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
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class MultipleHealthCheckTest {

    public static final String APP_NAME = "MultipleHealthCheckApp";

    private final String HEALTH_ENDPOINT = "/health";
    private final String READY_ENDPOINT = "/health/ready";
    private final String LIVE_ENDPOINT = "/health/live";
    private final String INVALID_ENDPOINT = "/foo";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503;
    private final int NOT_FOUND_RESPONSE_CODE = 404;

    final static String SERVER_NAME = "MultipleHealthCheck";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction()
                                    .withID("mpHealth-3.0")
                                    .addFeature("mpHealth-3.0")
                                    .removeFeature("mpHealth-2.0")
                                    .forServers(SERVER_NAME));

    @Server(SERVER_NAME)
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.microprofile.health20.multiple.health.checks.app");
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWMH0053W", "CWMMH0052W", "CWMMH0053W", "SRVE0190E");
    }

    @Test
    public void testFailureLivenessCheck() throws Exception {
        log("testLivenessCheck", "Testing the /health/live endpoint");
        HttpURLConnection conLive = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, LIVE_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conLive.getURL().toString(), FAILED_RESPONSE_CODE, conLive.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conLive);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals("The size of the JSON Liveness health check was not 2.", 2, checks.size());
        assertTrue("The health check name did not exist in JSON object.", checkIfHealthCheckNameExists(checks, "failed-liveness-check"));
        assertEquals("The status of the Liveness health check was not DOWN.", jsonResponse.getString("status"), "DOWN");
    }

    @Test
    public void testFailureCDIProducerLivenessCheck() throws Exception {
        log("testCDIProducerLivenessCheck", "Testing the /health/live endpoint");
        HttpURLConnection conLive = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, LIVE_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conLive.getURL().toString(), FAILED_RESPONSE_CODE, conLive.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conLive);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals("The size of the JSON CDIProducer Liveness health check was not 2.", 2, checks.size());
        assertTrue("The health check name did not exist in JSON object.", checkIfHealthCheckNameExists(checks, "failed-cdi-producer-liveness-check"));
        assertEquals("The status of the CDIProducer Liveness health check was not DOWN.", jsonResponse.getString("status"), "DOWN");
    }

    @Test
    public void testSuccessReadinessCheck() throws Exception {
        log("testReadinessCheck", "Testing the /health/ready endpoint");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conReady.getURL().toString(), SUCCESS_RESPONSE_CODE, conReady.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals("The size of the JSON Readiness health check was not 2.", 2, checks.size());
        assertTrue("The health check name did not exist in JSON object.", checkIfHealthCheckNameExists(checks, "successful-readiness-check"));
        assertEquals("The status of the Readiness health check was not UP.", jsonResponse.getString("status"), "UP");
    }

    @Test
    public void testSuccessCDIProducerReadinessCheck() throws Exception {
        log("testCDIProducerReadinessCheck", "Testing the /health/ready endpoint");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conReady.getURL().toString(), SUCCESS_RESPONSE_CODE, conReady.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals("The size of the JSON CDIProducer Readiness health check was not 2.", 2, checks.size());
        assertTrue("The health check name did not exist in JSON object.", checkIfHealthCheckNameExists(checks, "successful-cdi-producer-readiness-check"));
        assertEquals("The status of the CDIProducer Readiness health check was not UP.", jsonResponse.getString("status"), "UP");
    }

    @Test
    public void testDeprecatedHealthCheck() throws Exception {
        log("testHealthCheck", "Testing the /health endpoint");
        HttpURLConnection conHealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, HEALTH_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conHealth.getURL().toString(), FAILED_RESPONSE_CODE, conHealth.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conHealth);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals("The size of the JSON overall health check was not 4.", 4, checks.size());
        assertEquals("The status of the overall health check was not DOWN.", jsonResponse.getString("status"), "DOWN");
    }

    @Test
    public void testInvalidHealthEndpoint() throws Exception {
        log("testInvalidHealthEndpoint", "Testing the /health/foo endpoint");
        HttpURLConnection conHealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, HEALTH_ENDPOINT + INVALID_ENDPOINT);
        assertEquals("The Response Code was not 404 for the following endpoint: " + conHealth.getURL().toString(), NOT_FOUND_RESPONSE_CODE, conHealth.getResponseCode());

        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT + INVALID_ENDPOINT);
        assertEquals("The Response Code was not 404 for the following endpoint: " + conReady.getURL().toString(), NOT_FOUND_RESPONSE_CODE, conReady.getResponseCode());

        HttpURLConnection conLive = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, LIVE_ENDPOINT + INVALID_ENDPOINT);
        assertEquals("The Response Code was not 404 for the following endpoint: " + conLive.getURL().toString(), NOT_FOUND_RESPONSE_CODE, conLive.getResponseCode());
    }

    /**
     * Returns true if the expectedName, is found within JsonArray checks.
     */
    private boolean checkIfHealthCheckNameExists(JsonArray checks, String expectedName) {
        for (int i = 0; i < checks.size(); i++) {
            if (checks.getJsonObject(i).getString("name").equals(expectedName))
                return true;
        }
        return false;
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
        Log.info(MultipleHealthCheckTest.class, method, msg);
    }
}
