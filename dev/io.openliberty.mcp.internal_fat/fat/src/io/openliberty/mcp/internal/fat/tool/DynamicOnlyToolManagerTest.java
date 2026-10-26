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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

import java.io.StringReader;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.dynamicOnlyToolManagerApp.DynamicOnlyStartupBean;
import io.openliberty.mcp.internal.fat.tool.noMcpApp.NonMcpServlet;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Regression test for issue #36103.
 * <p>
 * Verifies that the MCP servlet is initialized and the {@code /mcp} endpoint is
 * accessible when an application registers tools <em>only</em> via
 * {@link io.openliberty.mcp.tools.ToolManager} at startup and contains no
 * {@code @Tool}-annotated methods.
 */
@RunWith(FATRunner.class)
public class DynamicOnlyToolManagerTest extends FATServletClient {

    private static final String APP_NAME = "DynamicOnlyToolManagerTest";
    private static final String NO_MCP_APP_NAME = "NonMcpApp";
    private static final String FILE_NOT_FOUND_ERROR_MESSAGE = "SRVE0190E: File not found: /mcp";

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = McpClient.builder(server, APP_NAME).build();

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(DynamicOnlyStartupBean.class.getPackage());

        WebArchive nonMcpApp = ShrinkWrap.create(WebArchive.class, NO_MCP_APP_NAME + ".war")
                                         .addPackage(NonMcpServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, app, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, nonMcpApp, SERVER_ONLY);

        server.startServer();

        // Verify the MCP endpoint started for the dynamic-only app
        assertNotNull("MCP endpoint must be logged for the dynamic-only app",
                      server.waitForStringInLog("MCP server endpoint: .*/DynamicOnlyToolManagerTest/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(FILE_NOT_FOUND_ERROR_MESSAGE);
    }

    /**
     * Verifies that a tool registered only via {@code ToolManager} at startup
     * appears in {@code tools/list}.
     * <p>
     * Before the fix, the MCP servlet would not initialize because
     * {@code hasTools()} returned {@code false} at {@code onStartup()} time
     * (no {@code @Tool} methods were present), so this call would fail with a 404.
     */
    @Test
    public void testDynamicOnlyToolAppearsInToolsList() throws Exception {
        Map<String, JsonObject> tools = getTools();
        assertThat("dynamic-only-tool must appear in tools/list when registered only via ToolManager",
                   tools, hasKey("dynamic-only-tool"));
    }

    /**
     * Verifies that a tool registered only via {@code ToolManager} at startup
     * can be called successfully.
     */
    @Test
    public void testDynamicOnlyToolCall() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "dynamic-only-tool",
                            "arguments": {}
                          }
                        }
                        """;
        String expected = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "dynamic-only-tool response"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expected, client.callMCP(request), STRICT);
    }

    /**
     * Verifies that a CDI-enabled web app with no MCP tools and no {@code ToolManager}
     * injection does NOT get an MCP servlet installed — its {@code /mcp} endpoint must
     * return 404.
     * <p>
     * This ensures the fix for issue #36103 does not regress to installing the MCP servlet
     * in every CDI-enabled web module regardless of MCP participation.
     */
    @Test
    public void testNonMcpAppHasNoMcpEndpoint() throws Exception {
        new HttpRequest(server, "/" + NO_MCP_APP_NAME + "/mcp")
                        .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                        .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                        .method("POST")
                        .expectCode(404)
                        .run(String.class);
        assertNotNull(server.waitForStringInLogUsingMark(FILE_NOT_FOUND_ERROR_MESSAGE, server.getDefaultLogFile()));
    }

    private Map<String, JsonObject> getTools() throws Exception {
        JsonObject response;
        try (JsonReader parser = Json.createReader(new StringReader(client.listAllTools()))) {
            response = parser.readObject();
        }
        return response.getJsonObject("result")
                       .getJsonArray("tools")
                       .stream()
                       .map(JsonObject.class::cast)
                       .collect(Collectors.toMap(o -> o.getString("name"), Function.identity()));
    }
}
