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
package io.openliberty.microprofile.health30.fat;

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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class DefaultOverallReadinessStatusUpAppStartupTest {

    private static final String[] EXPECTED_FAILURES = { "CWWKE1102W", "CWWKE1105W", "CWMMH0052W", "CWMMH0053W" };

    public static final String APP_NAME = "DelayedHealthCheckApp";
    private static final String MESSAGE_LOG = "logs/messages.log";

    private final String HEALTH_ENDPOINT = "/health";
    private final String READY_ENDPOINT = "/health/ready";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503; // Response when port is open but Application is not ready

    final static String SERVER_NAME = "DefaultReadinessOverallStatusHealthCheck";

    final static String INVALID_SERVER_NAME = "InvalidDefaultReadinessOverallStatusProperty";

    @Server(SERVER_NAME)
    public static LibertyServer server1;

    @Server(INVALID_SERVER_NAME)
    public static LibertyServer server2;

    public void setupClass(LibertyServer server, String testName) throws Exception {
        log("setupClass", testName + " - Deploying the Delayed App into the apps directory and starting the server.");

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "io.openliberty.microprofile.health30.delayed.health.check.app");
        ShrinkHelper.exportAppToServer(server, app, DeployOptions.DISABLE_VALIDATION);

        if (!server.isStarted())
            server.startServer(false, false);

        server.waitForStringInLog("CWWKT0016I: Web application available.*DelayedHealthCheckApp*");
    }

    @After
    public void cleanUp() throws Exception {
        log("cleanUp", " - Stopping the server, if servers are started.");

        if ((server1 != null) && (server1.isStarted()))
            server1.stopServer(EXPECTED_FAILURES);

        if ((server2 != null) && (server2.isStarted()))
            server2.stopServer(EXPECTED_FAILURES);
    }

    @Test
    public void testDefaultReadinessOverallStatusUpAtStartUpSingleApp() throws Exception {
        setupClass(server1, "testDefaultReadinessOverallStatusUpAtStartUpSingleApp");
        log("testDefaultReadinessOverallStatusUpAtStartUpSingleApp", "Testing the /health/ready endpoint, before application has started.");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conReady.getURL().toString(), SUCCESS_RESPONSE_CODE, conReady.getResponseCode());

        log("testDefaultReadinessOverallStatusUpAtStartUpSingleApp", "Testing the /health endpoint, before application has started.");
        HttpURLConnection conHealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, HEALTH_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conHealth.getURL().toString(), SUCCESS_RESPONSE_CODE, conHealth.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the Readiness health check was not UP.", jsonResponse.getString("status"), "UP");

        server1.setMarkToEndOfLog();

        List<String> lines = server1.findStringsInFileInLibertyServerRoot("CWMMH0053W:", MESSAGE_LOG);
        assertEquals("The CWMMH0053W warning should not appear in messages.log", 0, lines.size());

        String line = server1.waitForStringInLogUsingMark("(CWWKZ0001I: Application DelayedHealthCheckApp started)+", 60000);
        log("testDefaultReadinessOverallStatusUpAtStartUpSingleApp", "Application Started message found: " + line);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);

        log("testDefaultReadinessOverallStatusUpAtStartUpSingleApp", "Testing the /health/ready endpoint, after application has started.");
        HttpURLConnection conReady2 = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, READY_ENDPOINT);
        assertEquals("The Response Code was not the user-defined 503 for the following endpoint: " + conReady2.getURL().toString(), FAILED_RESPONSE_CODE,
                     conReady2.getResponseCode());

        JsonObject jsonResponse2 = getJSONPayload(conReady2);
        JsonArray checks2 = (JsonArray) jsonResponse2.get("checks");
        assertEquals("The size of the JSON Readiness health check was not 1.", 1, checks2.size());
        assertEquals("The status of the Readiness health check was not DOWN.", jsonResponse2.getString("status"), "DOWN");
    }

    @Test
    public void testInvalidDefaultReadinessOverallStatusProperty() throws Exception {
        // Set the invalid value for the MpConfig property. e.g. "mp.health.default.readiness.empty.response=UPs", it should be default behaviour (DOWN)
        setupClass(server2, "testInvalidDefaultReadinessOverallStatusProperty");
        log("testInvalidDefaultReadinessOverallStatusProperty", "Testing the /health/ready endpoint, before application has started.");
        HttpURLConnection conReady = HttpUtils.getHttpConnectionWithAnyResponseCode(server2, READY_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conReady.getURL().toString(), FAILED_RESPONSE_CODE, conReady.getResponseCode());

        log("testInvalidDefaultReadinessOverallStatusProperty", "Testing the /health endpoint, before application has started.");
        HttpURLConnection conHealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server2, HEALTH_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conHealth.getURL().toString(), FAILED_RESPONSE_CODE, conHealth.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conReady);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the Readiness health check was not DOWN.", jsonResponse.getString("status"), "DOWN");

        server2.setMarkToEndOfLog();

        List<String> lines = server2.findStringsInFileInLibertyServerRoot("CWMMH0053W:", MESSAGE_LOG);
        assertEquals("The CWMMH0053W warning did not appear in messages.log", 1, lines.size());

        String line = server2.waitForStringInLogUsingMark("(CWWKZ0001I: Application DelayedHealthCheckApp started)+", 60000);
        log("testInvalidDefaultReadinessOverallStatusProperty", "Application Started message found: " + line);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);

        log("testInvalidDefaultReadinessOverallStatusProperty", "Testing the /health/ready endpoint, after application has started.");
        HttpURLConnection conReady2 = HttpUtils.getHttpConnectionWithAnyResponseCode(server2, READY_ENDPOINT);

        JsonObject jsonResponse2 = getJSONPayload(conReady2);
        JsonArray checks2 = (JsonArray) jsonResponse2.get("checks");
        assertEquals("The size of the JSON Readiness health check was not 1.", 1, checks2.size());
        assertEquals("The status of the Readiness health check was not DOWN.", jsonResponse2.getString("status"), "DOWN");
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
        Log.info(DefaultOverallReadinessStatusUpAppStartupTest.class, method, msg);
    }
}
