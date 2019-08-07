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

import java.io.BufferedReader;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class MultipleHealthCheckTest {

    public static final String APP_NAME = "MultipleHealthCheckApp";

    private final String HEALTH_ENDPOINT = "/health";
    private final String READY_ENDPOINT = "/health/ready";
    private final String LIVE_ENDPOINT = "/health/live";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503;

    @Server("MultipleHealthCheck")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.microprofile.health20.multiple.health.checks.app");
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWMH0053W");
    }

    @Test
    public void testFailureLivenessCheck() throws Exception {
        log("testLivenessCheck", "Testing the /health/live endpoint");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, LIVE_ENDPOINT);
        assertEquals(FAILED_RESPONSE_CODE, conReady.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals(2, checks.size());
        assertEquals(true, checks.getJsonObject(0).getString("name").equals("failed-liveness-check") ||
                           checks.getJsonObject(1).getString("name").equals("failed-liveness-check"));
        assertEquals(jsonResponse.getString("status"), "DOWN");
    }

    @Test
    public void testFailureCDIProducerLivenessCheck() throws Exception {
        log("testLivenessCheck", "Testing the /health/live endpoint");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, LIVE_ENDPOINT);
        assertEquals(FAILED_RESPONSE_CODE, conReady.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals(2, checks.size());
        assertEquals(true, checks.getJsonObject(0).getString("name").equals("failed-cdi-producer-liveness-check") ||
                           checks.getJsonObject(1).getString("name").equals("failed-cdi-producer-liveness-check"));
        assertEquals(jsonResponse.getString("status"), "DOWN");
    }

    @Test
    public void testSuccessReadinessCheck() throws Exception {
        log("testReadinessCheck", "Testing the /health/ready endpoint");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        assertEquals(SUCCESS_RESPONSE_CODE, conReady.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals(2, checks.size());
        assertEquals(true, checks.getJsonObject(0).getString("name").equals("successful-readiness-check") ||
                           checks.getJsonObject(1).getString("name").equals("successful-readiness-check"));
        assertEquals(jsonResponse.getString("status"), "UP");
    }

    @Test
    public void testSuccessCDIProducerReadinessCheck() throws Exception {
        log("testReadinessCheck", "Testing the /health/ready endpoint");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        assertEquals(SUCCESS_RESPONSE_CODE, conReady.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals(2, checks.size());
        assertEquals(true, checks.getJsonObject(0).getString("name").equals("successful-cdi-producer-readiness-check") ||
                           checks.getJsonObject(1).getString("name").equals("successful-cdi-producer-readiness-check"));
        assertEquals(jsonResponse.getString("status"), "UP");
    }

    @Test
    public void testDeprecatedHealthCheck() throws Exception {
        log("testHealthCheck", "Testing the /health endpoint");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, HEALTH_ENDPOINT);
        assertEquals(FAILED_RESPONSE_CODE, conReady.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals(4, checks.size());
        assertEquals(jsonResponse.getString("status"), "DOWN");
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
