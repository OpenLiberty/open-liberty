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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.AfterClass;
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

/**
 * HTTP request tracing tests
 */
@RunWith(FATRunner.class)
public class TelemetryTraceTest extends FATServletClient {

    public static final String SERVER_NAME = "TelemetryTrace";
    public static final String APP_NAME = "MpTelemetryLogApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    //This test will run on all mp 2.0 repeats to ensure we have some test coverage on all versions.
    //I chose this one because TelemetryTrace is core to this bucket
    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry20Repeats();

    @BeforeClass
    public static void testSetup() throws Exception {
        setupApp(server);
        server.startServer();
    }

    static void setupApp(LibertyServer s) throws Exception {
        ShrinkHelper.defaultApp(s, APP_NAME, new DeployOptions[] { DeployOptions.SERVER_ONLY }, "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");
    }

    /**
     * Ensures trace logs are bridged and all attributes are present. Also ensures that both runtime
     * and app trace logs are routed when the runtime OTel SDK instance is used.
     */
    @Test
    public void testTelemetryTrace() throws Exception {
        testTelemetryTrace(server, null);
    }

    static void testTelemetryTrace(LibertyServer s, Consumer<List<String>> consoleConsumer) throws Exception {
        String runtimeLine = s.waitForStringInLog("Returning io.openliberty.microprofile.telemetry.runtime OTEL instance.", s.getConsoleLogFile());

        if (consoleConsumer != null) {
            List<String> linesConsoleLog = s.findStringsInLogs(".*scopeInfo.*", s.getConsoleLogFile());
            consoleConsumer.accept(linesConsoleLog);
        }

        TestUtils.runApp(s, "logServlet");
        String appLine = s.waitForStringInLog("finest trace", s.getConsoleLogFile());

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

        assertNotNull("Returning otel instance log could not be found.", runtimeLine);
        assertTrue("MPTelemetry did not log the correct log level", runtimeLine.contains("TRACE"));
        assertTrue("MPTelemetry did not log the correct message", runtimeLine.contains("Returning io.openliberty.microprofile.telemetry.runtime OTEL instance."));
        TestUtils.checkJsonMessage(runtimeLine, runtimeAttributeMap);

        assertNotNull("App Trace message could not be found.", appLine);
        assertTrue("MPTelemetry did not log the correct message", appLine.contains("finest trace"));
        assertTrue("MPTelemetry did not log the correct log level", appLine.contains("TRACE"));
        TestUtils.checkJsonMessage(appLine, appAttributeMap);
    }

    /**
     * Checks for populated span and trace ID for application logs
     */
    @Test
    public void testTelemetryTraceSpanID() throws Exception {
        TestUtils.runApp(server, "logServlet");
        String line = server.waitForStringInLog("finest trace", server.getConsoleLogFile());

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

        assertNotNull("App Trace message could not be found.", line);
        assertTrue("MPTelemetry did not log the correct message", line.contains("finest trace"));
        assertTrue("MPTelemetry did not log the correct log level", line.contains("TRACE"));
        assertFalse("MPTelemetry did not populate the trace ID.", line.contains("00000000000000000000000000000000"));
        assertFalse("MPTelemetry did not populate the span ID.", line.contains("0000000000000000"));

        TestUtils.checkJsonMessage(line, appAttributeMap);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}