/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal_fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class TelemetryDropinsTest extends FATServletClient {

    public static final String APP_NAME = "MpTelemetryLogApp";
    public static final String SERVER_NAME = "TelemetryDropins";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests rt = TelemetryActions.latestTelemetry20Repeats();

    @BeforeClass
    public static void initialSetup() throws Exception {
        server.saveServerConfiguration();
    }

    @Before
    public void testSetup() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();
    }

    @After
    public void testTearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

        server.restoreServerConfiguration();
    }

    /*
     * Test an application exported to dropins with an application SDK and ensure only application logs are bridged.
     */
    @Test
    public void testTelemetryDropinAppSDK() throws Exception {
        server.startServer();

        // Wait for server startup message.
        server.waitForStringInLog("CWWKF0011I", 15000, server.getConsoleLogFile());

        WebArchive app = ShrinkWrap
                        .create(WebArchive.class, "MpTelemetryLogApp.war")
                        .addPackage(
                                    "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp")
                        .addAsManifestResource(new File("publish/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);

        // Wait for application start up message.
        server.waitForStringInLog("CWWKZ0001I", 15000, server.getConsoleLogFile());

        String runtimeLine = server.waitForStringInLog("Runtime OTEL instance is being configured", 10000, server.getConsoleLogFile());
        TestUtils.runApp(server, "logServlet");
        String appLine = server.waitForStringInLog("finest trace", 10000, server.getConsoleLogFile());

        Map<String, String> appAttributeMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.type", "liberty_trace");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        //Ensure that runtime logs are not bridged.
        assertNull("Returning otel instance message was incorrectly bridged.", runtimeLine);

        //Ensure app logs are bridged.
        assertNotNull("App Trace message could not be found.", appLine);
        assertTrue("MPTelemetry did not log the correct message", appLine.contains("finest trace"));
        assertTrue("MPTelemetry did not log the correct log level", appLine.contains("TRACE"));
        TestUtils.checkJsonMessage(appLine, appAttributeMap);
    }

    /*
     * Test an application exported to dropins with a Runtime SDK and ensure both application and runtime logs are bridged.
     */
    @Test
    public void testTelemetryDropinRuntimeSDK() throws Exception {
        server.addEnvVar("OTEL_LOGS_EXPORTER", "logging");
        server.addEnvVar("OTEL_SDK_DISABLED", "false");
        server.startServer();

        // Wait for server startup message.
        server.waitForStringInLog("CWWKF0011I", 15000, server.getConsoleLogFile());

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");
        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);

        // Wait for application start up message.
        server.waitForStringInLog("CWWKZ0001I", 15000, server.getConsoleLogFile());

        String runtimeLine = server.waitForStringInLog("Returning io.openliberty.microprofile.telemetry.runtime OTEL instance.", 5000, server.getConsoleLogFile());

        TestUtils.runApp(server, "logServlet");
        String appLine = server.waitForStringInLog("finest trace", server.getConsoleLogFile());

        Map<String, String> runtimeAttributeMap = new HashMap<String, String>() {
            {
                put("io.openliberty.type", "liberty_trace");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemtryLifecycleManagerImpl");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        Map<String, String> appAttributeMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.type", "liberty_trace");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        //Ensure runtime logs are bridged.
        assertNotNull("Returning otel instance log could not be found.", runtimeLine);
        assertTrue("MPTelemetry did not log the correct log level", runtimeLine.contains("TRACE"));
        assertTrue("MPTelemetry did not log the correct message", runtimeLine.contains("Returning io.openliberty.microprofile.telemetry.runtime OTEL instance."));
        TestUtils.checkJsonMessage(runtimeLine, runtimeAttributeMap);

        //Ensure app logs are bridged.
        assertNotNull("App Trace message could not be found.", appLine);
        assertTrue("MPTelemetry did not log the correct message", appLine.contains("finest trace"));
        assertTrue("MPTelemetry did not log the correct log level", appLine.contains("TRACE"));
        TestUtils.checkJsonMessage(appLine, appAttributeMap);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}