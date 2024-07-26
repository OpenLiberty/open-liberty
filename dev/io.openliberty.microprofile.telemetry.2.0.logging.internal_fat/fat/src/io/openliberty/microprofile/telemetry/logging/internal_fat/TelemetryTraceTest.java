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
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

/**
 * HTTP request tracing tests
 */
@RunWith(FATRunner.class)
public class TelemetryTraceTest extends FATServletClient {

    public static final String APP_NAME = "TelemetryServletTestApp";
    public static final String SERVER_NAME = "TelemetryTraceNoApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void initialSetup() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.startServer();
    }

    /**
     * Ensures trace logs are bridged and all attributes are present.
     */
    @Test
    public void testTelemetryTrace() throws Exception {
        String line = server.waitForStringInLog("Returning io.openliberty.microprofile.telemetry.runtime OTEL instance.", server.getConsoleLogFile());

        assertNotNull("Returning otel instance log could not be found.", line);
        assertTrue("MPTelemetry did not log the correct log level", line.contains("TRACE"));
        assertTrue("MPTelemetry did not log the correct message", line.contains("Returning io.openliberty.microprofile.telemetry.runtime OTEL instance."));
        assertTrue("MPTelemetry did not log server module field",
                   line.contains("io.openliberty.module=\"io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfoFactoryImpl\""));
        assertTrue("MPTelemetry did not log server sequence field", line.contains("io.openliberty.sequence=\""));
        assertTrue("MPTelemetry did not log server type field", line.contains("io.openliberty.type=\"liberty_trace\""));
        assertTrue("MPTelemetry did not log server threadID field", line.contains("thread.id"));
        assertTrue("MPTelemetry did not log server thread name field", line.contains("thread.name"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}