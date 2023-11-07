/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.LoggingServlet;

@RunWith(FATRunner.class)
public class TelemetryLoggingExporterTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10ConfigEnv";
    public static final String APP_NAME = "TelemetryApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.allMPRepeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClasses(LoggingServlet.class)
                        //Use logging exporter
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=logging"),
                        "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testConsole() throws Exception {
        server.setMarkToEndOfLog();

        runTest(server, APP_NAME + "/LoggingServlet", "testLoggingExporter");

        //The exporter should not give this error message
        assertNull(server.verifyStringNotInLogUsingMark("Failed to export spans", 1000));

        //Checks for span output in logs
        assertNotNull(server.waitForStringInLogUsingMark("io.opentelemetry.exporter.logging.LoggingSpanExporter"));
        assertNotNull(server.waitForStringInLogUsingMark("'testSpan' : .* \\[tracer: logging-exporter-test:1.0.0\\]"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
