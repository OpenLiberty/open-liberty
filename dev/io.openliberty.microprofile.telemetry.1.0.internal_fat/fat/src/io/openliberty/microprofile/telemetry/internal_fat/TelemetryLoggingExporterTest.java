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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;


import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.LoggingServlet;

@RunWith(FATRunner.class)
public class TelemetryLoggingExporterTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10ConfigEnv";
    public static final String APP_NAME = "TelemetryApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClasses(LoggingServlet.class);

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);

        server.addEnvVar("otel_sdk_disabled", "false");

        //Use logging exporter
        server.addEnvVar("otel_traces_exporter", "logging");

        server.startServer();
    }

    @Test
    public void testConsole() throws Exception {
        server.setMarkToEndOfLog();

        runTest(server, APP_NAME + "/LoggingServlet", "testLoggingExporter");

        //The exporter should not give this error message
        assertNull(server.verifyStringNotInLogUsingMark("Failed to export spans", 1000));

        //Checks for span output in logs
        assertFalse(server.waitForStringInLogUsingMark("io.opentelemetry.exporter.logging.LoggingSpanExporter").isEmpty());
        assertFalse(server.waitForStringInLogUsingMark("'testSpan' : .* \\[tracer: logging-exporter-test:1.0.0\\]").isEmpty());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
