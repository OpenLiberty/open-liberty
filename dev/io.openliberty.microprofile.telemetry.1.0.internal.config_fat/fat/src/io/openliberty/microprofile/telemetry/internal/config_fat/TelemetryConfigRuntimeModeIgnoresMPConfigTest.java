/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.config_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal.config_fat.apps.telemetry.LoggingServlet;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

/**
 * This test verifies that a property set in META-INF/microprofile-config.properties
 * is ignored even if there is no corresponding property in the system properties or env
 */
@RunWith(FATRunner.class)
public class TelemetryConfigRuntimeModeIgnoresMPConfigTest extends FATServletClient {

    public static final String APP_NAME = "TelemetryServletTestApp";
    public static final String SERVER_NAME = "Telemetry10IgnoreMPConfig";

    @TestServlets({
                    @TestServlet(contextRoot = APP_NAME, servlet = LoggingServlet.class)
    })

    @Server(SERVER_NAME)
    public static LibertyServer server;

    //This test is for the runtime mode, introduced in OTel 2.0
    @ClassRule
    public static RepeatTests r = TelemetryActions.telemetry21andLatest20Repeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {

        server.addEnvVar("OTEL_SDK_DISABLED", "false");
        server.addEnvVar("OTEL_TRACES_EXPORTER", "logging");

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClasses(LoggingServlet.class)
                        //If this nonsense config is implemented line 86 will fail.
                        .addAsResource(new StringAsset("otel.attribute.value.length.limit=1"),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testConsole() throws Exception {
        //Above mark because this is logged during startup
        //Checks for a warning message because server.xml conflicts with the system properties and env variables.
        assertNotNull(server.waitForStringInLogUsingMark("CWMOT5006W"));

        server.setMarkToEndOfLog();

        runTest(server, APP_NAME + "/LoggingServlet", "testLoggingExporter");

        //The exporter should not give this error message
        assertNull(server.verifyStringNotInLogUsingMark("Failed to export spans", 1000));

        //Checks for span output in logs
        assertNotNull(server.waitForStringInLogUsingMark("io.opentelemetry.exporter.logging.LoggingSpanExporter"));
        assertNotNull(server.waitForStringInLogUsingMark("'testSpan' : .* \\[tracer: logging-exporter-test:1.0.0\\]"));
        assertNotNull(server.waitForStringInLogUsingMark("GET /TelemetryServletTestApp/LoggingServle.*url.query=testMethod=testLoggingExporter"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMOT5006W");
    }
}