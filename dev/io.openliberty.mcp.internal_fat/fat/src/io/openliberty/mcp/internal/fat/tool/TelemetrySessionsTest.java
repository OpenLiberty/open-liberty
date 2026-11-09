/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static io.openliberty.mcp.internal.fat.suite.McpTelemetryServerSuite.serverLifecycle;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.observability.telemetry.PullExporterAutoConfigurationCustomizerProvider;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

@RunWith(FATRunner.class)
public class TelemetrySessionsTest extends FATServletClient {

    // Server is managed by McpTelemetryServerSuite — do NOT add @Server or copy the field here.
    private final static String APP_NAME = "telemetryTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();
    
    private static final String BASIC_TOOL_REQUEST = """
                      {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/call",
                      "params": {
                        "name": "basicTool",
                        "arguments": {}
                      }
                    }
                    """;

    private static final String ADVANCED_TOOL_REQUEST = """
                      {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/call",
                      "params": {
                        "name": "advancedTool",
                        "arguments": {}
                      }
                    }
                    """;

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(PullExporterAutoConfigurationCustomizerProvider.class.getPackage())
                                   .addAsResource(new StringAsset("otel.sdk.disabled=false"),
                                                  "META-INF/microprofile-config.properties")
                                   .addAsServiceProvider(AutoConfigurationCustomizerProvider.class,
                                                         PullExporterAutoConfigurationCustomizerProvider.class);
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of("CWWKS9113E"));
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testSessionDurationMetrics() throws Exception {
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpSessionMetricServlet", "captureSessionDurationMetrics");
        // Perform some operations within the session
        client.callMCP(BASIC_TOOL_REQUEST);
        client.callMCP(ADVANCED_TOOL_REQUEST);

        // Delete the session to trigger sessionEnded metric recording
        client.deleteSession();

        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpSessionMetricServlet", "testSessionDurationMetrics");
    }

}
