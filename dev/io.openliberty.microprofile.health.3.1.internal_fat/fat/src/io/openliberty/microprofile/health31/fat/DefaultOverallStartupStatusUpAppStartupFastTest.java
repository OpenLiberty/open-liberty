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

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
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
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

@RunWith(FATRunner.class)
/*
 * This is similar to DefaultOverallStartupStatusUpAppStartupTest. But is "faster'.
 * this mocks a late startup by allowing server to start first and then deploying the app into
 * the dropins.
 */
public class DefaultOverallStartupStatusUpAppStartupFastTest {

    private static final String[] EXPECTED_FAILURES = { "CWWKE1102W", "CWWKE1105W", "CWMMH0052W", "CWMMH0054W", "SRVE0302E" };

    public static final String APP_NAME = "DelayedHealthCheckAppFast";
    private static final String MESSAGE_LOG = "logs/messages.log";

    private final String HEALTH_ENDPOINT = "/health";
    private final String STARTED_ENDPOINT = "/health/started";

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503; // Response when port is open but Application has not started yet.

    final static String SERVER_NAME = "DefaultStartupOverallStatusHealthCheckFast";

    final static String INVALID_SERVER_NAME = "InvalidDefaultStartupOverallStatusPropertyFast";

    @Server(SERVER_NAME)
    public static LibertyServer server1;

    @Server(INVALID_SERVER_NAME)
    public static LibertyServer server2;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS,
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

        if ((server2 != null) && (server2.isStarted()))
            server2.stopServer(EXPECTED_FAILURES);
    }

    /*
     * Sets the MpConfig property. e.g. "mp.health.default.startup.empty.response=UP", the started endpoint should return UP even if the application is still starting up.
     */
    @Test
    public void testDefaultStartupOverallStatusUpAtStartUpSingleApp() throws Exception {
        setupClass(server1, "testDefaultStartupOverallStatusUpAtStartUpSingleApp");
        deployApp(server1, "testDefaultStartupOverallStatusUpAtStartUpSingleApp");
        log("testDefaultStartupOverallStatusUpAtStartUpSingleApp", "Testing the /health/started endpoint, before application has started.");
        HttpURLConnection conStarted = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, STARTED_ENDPOINT);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conStarted.getURL().toString(), SUCCESS_RESPONSE_CODE, conStarted.getResponseCode());

        log("testDefaultStartupOverallStatusUpAtStartUpSingleApp",
            "Testing the /health endpoint, before application has started, it should be DOWN since the Readiness check status will still be DOWN.");
        HttpURLConnection conHealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, HEALTH_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conHealth.getURL().toString(), FAILED_RESPONSE_CODE, conHealth.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conStarted);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the Startup health check was not UP.", jsonResponse.getString("status"), "UP");

        List<String> lines = server1.findStringsInFileInLibertyServerRoot("CWMMH0054W:", MESSAGE_LOG);
        assertEquals("The CWMMH0054W warning should not appear in messages.log", 0, lines.size());

        String line = server1.waitForStringInLogUsingMark("(CWWKZ0001I: Application " + APP_NAME + " started)+", 30000);
        log("testDefaultStartupOverallStatusUpAtStartUpSingleApp", "Application Started message found: " + line);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);

        log("testDefaultStartupOverallStatusUpAtStartUpSingleApp", "Testing the /health/started endpoint, after application has started.");
        HttpURLConnection conStarted2 = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, STARTED_ENDPOINT);
        assertEquals("The Response Code was not the 200 for the following endpoint: " + conStarted2.getURL().toString(), SUCCESS_RESPONSE_CODE,
                     conStarted2.getResponseCode());

        JsonObject jsonResponse2 = getJSONPayload(conStarted2);
        JsonArray checks2 = (JsonArray) jsonResponse2.get("checks");
        assertEquals("The size of the JSON Startup health check was not 1.", 1, checks2.size());
        assertEquals("The status of the Startup health check was not UP for the user-defined health checks.", jsonResponse2.getString("status"), "UP");
    }

    /*
     * Set the invalid value for the MpConfig property. e.g. "mp.health.default.startup.empty.response=UPs", it should be default behaviour (DOWN)
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidDefaultStartupOverallStatusProperty() throws Exception {
        setupClass(server2, "testInvalidDefaultStartupOverallStatusProperty");
        deployApp(server2, "testInvalidDefaultStartupOverallStatusProperty");
        log("testInvalidDefaultStartupOverallStatusProperty", "Testing the /health/started endpoint, before application has started.");
        HttpURLConnection conStarted = HttpUtils.getHttpConnectionWithAnyResponseCode(server2, STARTED_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conStarted.getURL().toString(), FAILED_RESPONSE_CODE, conStarted.getResponseCode());

        log("testInvalidDefaultStartupsOverallStatusProperty", "Testing the /health endpoint, before application has started.");
        HttpURLConnection conHealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server2, HEALTH_ENDPOINT);
        assertEquals("The Response Code was not 503 for the following endpoint: " + conHealth.getURL().toString(), FAILED_RESPONSE_CODE, conHealth.getResponseCode());

        JsonObject jsonResponse = getJSONPayload(conStarted);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the Startup health check was not DOWN.", jsonResponse.getString("status"), "DOWN");

        server2.setMarkToEndOfLog();

        List<String> lines = server2.findStringsInFileInLibertyServerRoot("CWMMH0054W:", MESSAGE_LOG);
        assertEquals("The CWMMH0054W warning did not appear in messages.log", 1, lines.size());

        String line = server2.waitForStringInLogUsingMark("(CWWKZ0001I: Application " + APP_NAME + " started)+", 90000);
        log("testInvalidDefaultStartupOverallStatusProperty", "Application Started message found: " + line);
        assertNotNull("The CWWKZ0001I Application started message did not appear in messages.log", line);

        log("testInvalidDefaultStartupOverallStatusProperty", "Testing the /health/started endpoint, after application has started.");
        HttpURLConnection conStarted2 = HttpUtils.getHttpConnectionWithAnyResponseCode(server2, STARTED_ENDPOINT);

        JsonObject jsonResponse2 = getJSONPayload(conStarted2);
        JsonArray checks2 = (JsonArray) jsonResponse2.get("checks");
        assertEquals("The size of the JSON Startup health check was not 1.", 1, checks2.size());
        assertEquals("The status of the Startup health check was not UP for user-defined health checks.", jsonResponse2.getString("status"), "UP");
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
        Log.info(DefaultOverallStartupStatusUpAppStartupFastTest.class, method, msg);
    }
}
