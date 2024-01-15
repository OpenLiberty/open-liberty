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

import java.io.BufferedReader;
import java.io.File;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.exception.TopologyException;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ConfigAdminHealthCheckTest {

    final static String SERVER_NAME = "ConfigAdminDropinsCheck";
    final static String SERVER_NAME2 = "ConfigAdminXmlCheck";

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

    private final int SUCCESS_RESPONSE_CODE = 200;
    private final int FAILED_RESPONSE_CODE = 503; // Response when port is open but Application is not ready

    public static final int APP_STARTUP_TIMEOUT = 120 * 1000;

    private static enum HealthCheck {
        LIVE, READY, HEALTH;
    }

    private static enum Status {
        SUCCESS, FAILURE;
    }

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
        if (appName.equals(FAILS_TO_START_APP_NAME)) {
            app = app.addAsManifestResource(ConfigAdminHealthCheckTest.class.getResource("permissions.xml"), "permissions.xml");
            File libsDir = new File("lib/LibertyFATTestFiles/libs");
            for (File file : libsDir.listFiles()) {
                server.copyFileToLibertyServerRoot(file.getParent(), "kafkaLib", file.getName());
            }
            //Don't validate that FAILS_TO_START_APP_NAME starts correctly.
            ShrinkHelper.exportAppToServer(server, app, DeployOptions.DISABLE_VALIDATION);
        } else if (appName.equals(DELAYED_APP_NAME)) {
            //Don't wait for app to start because it sleeps for 60 seconds
            ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.DISABLE_VALIDATION);
            //But wait for the servlet to be up
            server.waitForStringInLog("CWWKT0016I:.*" + DELAYED_APP_NAME);
        } else {
            ShrinkHelper.exportDropinAppToServer(server, app);
        }

    }

    private void waitForApplication(LibertyServer server, String appName) {
        if (appName.equals(FAILS_TO_START_APP_NAME)) {
            log("waitForApplication", "Waiting for expected app failure");
            server.waitForStringInLog("CWWKZ0012I.* " + FAILS_TO_START_APP_NAME, APP_STARTUP_TIMEOUT);
            log("waitForApplication", "Waiting for expected FFDC");
            server.waitForMultipleStringsInLog(3, "FFDC1015I");
        } else {
            log("waitForApplication", "Waiting for " + appName + " to start");
            server.waitForStringInLog("CWWKZ0001I.* " + appName, APP_STARTUP_TIMEOUT);
        }
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
