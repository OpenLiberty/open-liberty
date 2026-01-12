/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.ServletContainerInitializer;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

/**
 *
 */
@RunWith(FATRunner.class)
public class FailedApplicationStartTest {

    final static String SERVER_NAME_DROPINS = "ServletContainerInitializerFailsServerDropins";

    final static String SERVER_NAME_APP_DIR = "ServletContainerInitializerFailsServerAppDir";

    final static String APP_NAME = "StartedThenFailsApp";

    private final String HEALTH_ENDPOINT = "/health";

    private final String READY_ENDPOINT = "/health/ready";

    private final String LIVE_ENDPOINT = "/health/live";

    private final String STARTED_ENDPOINT = "/health/started";

    @Server(SERVER_NAME_DROPINS)
    public static LibertyServer serverDropins;

    @Server(SERVER_NAME_APP_DIR)
    public static LibertyServer serverAppDir;

    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS,
                                                             MicroProfileActions.MP70_EE10, // mpHealth-4.0 LITE
                                                             MicroProfileActions.MP70_EE11, // mpHealth-4.0 FULL
                                                             HealthActions.MP41_MPHEALTH40, //  mpHealth-4.0 FULL w/ MP41 EE8
                                                             HealthActions.MP14_MPHEALTH40, // mpHealth-4.0 FULL w/ MP14 EE7
                                                             MicroProfileActions.MP41, // mpHealth-3.1 FULL
                                                             MicroProfileActions.MP40, // mpHealth-3.0 FULL
                                                             MicroProfileActions.MP30); //mpHealth-2.0 FULL

    /**
     * Test that loads an app that throws a java.lang.Error in the servlet container intializer.
     * We expect to see a CWWKZ0012I because of it and that the health check (overall health check) reports DOWN.
     *
     * This test tests the app in dropins folder.
     *
     * @throws Exception
     */
    @ExpectedFFDC("java.lang.Error")
    @Test
    public void webModuleStartsandFailsTest_dropins() throws Exception {

        server = serverDropins;

        Assert.assertTrue(String.format("Server [%s] should not have been started", server), !server.isStarted());

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "io.openliberty.microprofile.health31.start.and.fails.app")
                        .addAsServiceProvider(ServletContainerInitializer.class.getName(),
                                              "io.openliberty.microprofile.health31.start.and.fails.app.AppServletContainerInitializer");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY);

        server.startServer();

        log("webModuleStartsandFailsTest", "Checking (and waiting) for CWWKZ0012I");
        String ret = server.waitForStringInLog("CWWKZ0012I");
        assertNotNull("We expected to find CWWKZ0012I, but was not found", ret);

        // Check /health -> expect that we report DOWN.
        HttpURLConnection connhealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server, HEALTH_ENDPOINT);
        JsonObject jsonResponse = getJSONPayload(connhealth);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the (overall) health check was epected to be DOWN, but we received UP.", jsonResponse.getString("status"), "DOWN");

        // Check /health/started -> expect that we report DOWN.
        // Must skip for mpHealth-2.0 and mpHealth-3.0, does not contain start health check
        if (!server.getServerConfiguration().getFeatureManager().getFeatures().contains("mpHealth-2.0") &&
            !server.getServerConfiguration().getFeatureManager().getFeatures().contains("mpHealth-3.0")) {
            connhealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server, STARTED_ENDPOINT);
            jsonResponse = getJSONPayload(connhealth);
            checks = (JsonArray) jsonResponse.get("checks");
            assertTrue("The JSON response was not empty.", checks.isEmpty());
            assertEquals("The status of the started health check was epected to be DOWN, but we received UP.", jsonResponse.getString("status"), "DOWN");
        } else {
            log("webModuleStartsandFailsTest_dropins", "mpHealth-2.0 detected, skipping on checking the /health/started endpoint");
        }

        // Check /health/ready -> expect that we report DOWN.
        connhealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server, READY_ENDPOINT);
        jsonResponse = getJSONPayload(connhealth);
        checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the ready health check was epected to be DOWN, but we received UP.", jsonResponse.getString("status"), "DOWN");

        // Check /health/live -> expect that we report UP.
        connhealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server, LIVE_ENDPOINT);
        jsonResponse = getJSONPayload(connhealth);
        checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the live health check was epected to be UP, but we received DOWN.", jsonResponse.getString("status"), "UP");

    }

    /**
     * Test that loads an app that throws a java.lang.Error in the servlet container intializer.
     * We expect to see a CWWKZ0012I because of it and that the health check (overall health check) reports DOWN.
     *
     * This test tests the app in apps folder.
     *
     * @throws Exception
     */
    @ExpectedFFDC("java.lang.Error")
    @Test
    public void webModuleStartsandFailsTest_apps() throws Exception {

        server = serverAppDir;

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "io.openliberty.microprofile.health31.start.and.fails.app")
                        .addAsServiceProvider(ServletContainerInitializer.class.getName(),
                                              "io.openliberty.microprofile.health31.start.and.fails.app.AppServletContainerInitializer");

        ShrinkHelper.exportAppToServer(server, app, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY);

        server.startServer();

        log("webModuleStartsandFailsTest", "Checking (and waiting) for CWWKZ0012I");
        String ret = server.waitForStringInLog("CWWKZ0012I");
        assertNotNull("We expected to find CWWKZ0012I, but was not found", ret);

        // Check /health -> expect that we report DOWN.
        HttpURLConnection connhealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server, HEALTH_ENDPOINT);
        JsonObject jsonResponse = getJSONPayload(connhealth);
        JsonArray checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the (overall) health check was epected to be DOWN, but we received UP.", jsonResponse.getString("status"), "DOWN");

        // Check /health/started -> expect that we report DOWN.
        // Must skip for mpHealth-2.0 and mpHealth-3.0, does not contain start health check
        if (!server.getServerConfiguration().getFeatureManager().getFeatures().contains("mpHealth-2.0") &&
            !server.getServerConfiguration().getFeatureManager().getFeatures().contains("mpHealth-3.0")) {
            connhealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server, STARTED_ENDPOINT);
            jsonResponse = getJSONPayload(connhealth);
            checks = (JsonArray) jsonResponse.get("checks");
            assertTrue("The JSON response was not empty.", checks.isEmpty());
            assertEquals("The status of the started health check was epected to be DOWN, but we received UP.", jsonResponse.getString("status"), "DOWN");
        } else {
            log("webModuleStartsandFailsTest_dropins", "mpHealth-2.0 detected, skipping on checking the /health/started endpoint");
        }

        // Check /health/ready -> expect that we report DOWN.
        connhealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server, READY_ENDPOINT);
        jsonResponse = getJSONPayload(connhealth);
        checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the ready health check was epected to be DOWN, but we received UP.", jsonResponse.getString("status"), "DOWN");

        // Check /health/live -> expect that we report UP.
        connhealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server, LIVE_ENDPOINT);
        jsonResponse = getJSONPayload(connhealth);
        checks = (JsonArray) jsonResponse.get("checks");
        assertTrue("The JSON response was not empty.", checks.isEmpty());
        assertEquals("The status of the live health check was epected to be UP, but we received DOWN.", jsonResponse.getString("status"), "UP");

    }

    @After
    public void after() throws Exception {
        /*
         * We expect:
         * CWWKZ0012I -> These tests are meant to fail so that the app does not start, we expect this Warning/Error.
         * CWMMH0053W -> MpHealth-3.x can throw CWMMH0053W (double M) if ready is DOWN for ANY endpoint (not just only when checking /ready)
         * CWMMH0053W -> MpHealth-2.x can throw CWMH0053W (single M) if ready is DOWN for ANY endpoint (not just only when checking /ready)
         * CWMMH0054W -> When querying /started, we expect this to be thrown since app did not start.
         */
        server.stopServer("CWWKZ0012I", "CWMMH0053W", "CWMH0053W", "CWMMH0054W");
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
        Log.info(ConfigAdminHealthCheckTest.class, method, msg);
    }
}
