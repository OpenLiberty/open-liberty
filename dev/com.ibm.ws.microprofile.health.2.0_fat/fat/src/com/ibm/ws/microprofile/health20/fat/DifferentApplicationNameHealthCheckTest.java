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
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class DifferentApplicationNameHealthCheckTest {
    public static final String APP_NAME = "DifferentAppNameHealthCheckApp";

    private final String HEALTH_ENDPOINT = "/health";
    private final String READY_ENDPOINT = "/health/ready";
    private final String LIVE_ENDPOINT = "/health/live";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503;

    final static String SERVER_NAME = "DifferentAppNameHealthCheck";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction("mpHealth-2.0", "mpHealth-3.0")
                                    .withID("mpHealth-3.0")
                                    .forServers(SERVER_NAME));

    @Server(SERVER_NAME)
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.microprofile.health20.different.app.name.health.checks.app");
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWKE1102W", "CWWKE1105W", "CWMH0052W", "CWMH0053W", "CWMMH0052W", "CWMMH0053W");
    }

    @Test
    public void testSuccessLivenessCheckWithDifferentApplicationName() throws Exception {
        log("testSuccessLivenessCheckWithDifferentApplicationName", "Testing the /health/live endpoint");
        HttpURLConnection conLive = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, LIVE_ENDPOINT);
        assertEquals(SUCCESS_RESPONSE_CODE, conLive.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conLive);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals(1, checks.size());
        assertTrue("The health check name did not exist in JSON object.", checkIfHealthCheckNameExists(checks, "successful-simple-liveness-check"));
        assertEquals(jsonResponse.getString("status"), "UP");
    }

    @Test
    public void testFailedReadinessCheckWithDifferentApplicationName() throws Exception {
        log("testFailedReadinessCheckWithDifferentApplicationName", "Testing the /health/ready endpoint");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        assertEquals(FAILED_RESPONSE_CODE, conReady.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals(1, checks.size());
        assertTrue("The health check name did not exist in JSON object.", checkIfHealthCheckNameExists(checks, "failed-simple-readiness-check"));
        assertEquals(jsonResponse.getString("status"), "DOWN");
    }

    @Test
    @SkipForRepeat("mpHealth-3.0")
    public void testDeprecatedHealthCheckWithDifferentApplicationName() throws Exception {
        log("testDeprecatedHealthCheckWithDifferentApplicationName", "Testing the /health endpoint");
        HttpURLConnection conHealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, HEALTH_ENDPOINT);
        assertEquals(FAILED_RESPONSE_CODE, conHealth.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conHealth);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertEquals(3, checks.size());
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
     * Returns true if the expectedName, is found within JsonArray checks.
     */
    private boolean checkIfHealthCheckNameExists(JsonArray checks, String expectedName) {
        for (int i = 0; i < checks.size(); i++) {
            if (checks.getJsonObject(i).getString("name").equals(expectedName))
                return true;
        }
        return false;
    }

    /**
     * Helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(DifferentApplicationNameHealthCheckTest.class, method, msg);
    }
}