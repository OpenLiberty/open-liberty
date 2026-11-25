/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static io.openliberty.mcp.internal.fat.suite.McpAsyncServerSuite.serverLifecycle;
import static io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher.toolResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

import com.ibm.websphere.simplicity.config.Mcp;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

@RunWith(FATRunner.class)
public class AsyncToolsTest extends FATServletClient {

    // Server is managed by McpAsyncServerSuite — do NOT add @Server or copy the field here.

    private static final String APP_NAME = "asyncToolsTest";
    private static final String SHORT_TIMEOUT_APP_NAME = "asyncToolsTestShortTimeout";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    @Rule
    public McpClient shortTimeoutClient = McpClient.builder(serverLifecycle.getServer(), "/" + SHORT_TIMEOUT_APP_NAME).build();

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(serverLifecycle.getServer(), "/" + APP_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of("Method call caused runtime exception. This is expected if the input was 'throw error'"));
        serverLifecycle.getServer().setMarkToEndOfLog();

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(AsyncTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

        serverLifecycle.deployDropinApp(war);

        WebArchive shortTimeoutWar = ShrinkWrap.create(WebArchive.class, SHORT_TIMEOUT_APP_NAME + ".war")
                                               .addPackage(AsyncTools.class.getPackage())
                                               .addPackage(ToolStatus.class.getPackage());

        serverLifecycle.deployWithConfiguration(shortTimeoutWar, app -> {
            Mcp mcp = new Mcp();
            mcp.setAsyncTimeout("1s");
            app.getMcps().add(mcp);
        });

        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/asyncToolsTest/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployWithConfiguration(SHORT_TIMEOUT_APP_NAME);
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testCompletionStageThatCompletesImmediately() throws Exception {
        assertThat(client.callTool("asyncEcho", Map.of("input", "Async Hello")), toolResponse().withTextContent("Async Hello: (async)"));
    }

    @Test
    public void testCompletionStageThatCompletesLater() throws Exception {
        assertThat(client.callTool("asyncDelayedEcho", Map.of("input", "Async Hello")), toolResponse().withTextContent("Async Hello: (async)"));
    }

    @Test
    public void testCompletionStageThatFailsImmediately() throws Exception {
        assertThat(client.callTool("asyncEcho", Map.of("input", "throw error")),
                   toolResponse().withError("An internal server error occurred while running the tool."));
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("Method call caused runtime exception", serverLifecycle.getServer().getDefaultLogFile()));
    }

    @Test
    public void testCompletionStageThatFailsLater() throws Exception {
        assertThat(client.callTool("asyncDelayedEcho", Map.of("input", "throw error")),
                   toolResponse().withError("An internal server error occurred while running the tool."));
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("Method call caused runtime exception", serverLifecycle.getServer().getDefaultLogFile()));
    }

    @Test
    public void testAsyncEchoWithInvalidParamsException() throws Exception {
        assertThat(client.callTool("asyncEcho"),
                   toolResponse().withError("The method expected the following arguments but did not receive them: [input]."));
    }

    @Test
    public void testAsyncToolReturningSingleObject() throws Exception {
        JSONObject expectedJson = new JSONObject()
                                                  .put("country", "England")
                                                  .put("isCapital", false)
                                                  .put("name", "Manchester")
                                                  .put("population", 8000);

        assertThat(client.callTool("asyncObjectTool", Map.of("name", "Manchester")),
                   toolResponse().withTextContent("{\"country\":\"England\",\"isCapital\":false,\"name\":\"Manchester\",\"population\":8000}")
                                 .withStructuredContent(expectedJson));
    }

    @Test(expected = Exception.class)
    @Mode(TestMode.FULL)
    public void testCompletionStageThatNeverCompletes() throws Exception {
        shortTimeoutClient.callTool("asyncToolThatNeverCompletes", Map.of("input", "Hello"));
    }

    @Test
    public void testContentEncoderThatReturnsCompletionStage() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testContentEncoderCompletionStage"),
                   toolResponse().withTextContent("{\"age\":32,\"fistName\":\"Jon\",\"lastName\":\"Encoded by PersonContentEncoder\"}"));
    }

    @Test
    public void testContentEncoderCompletionStageWithListEncoding() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testContentEncoderEncodingACompletionStageContainingAList"),
                   toolResponse().withTextContents("{\"age\":32,\"fistName\":\"Jon\",\"lastName\":\"Encoded by PersonContentEncoder\"}",
                                                   "{\"age\":22,\"fistName\":\"Jane\",\"lastName\":\"Encoded by PersonContentEncoder\"}"));
    }

    @Test
    public void testAsyncObjectToolHasExpectedOutputSchema() throws Exception {
        String response = client.callMCP("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "method": "tools/list"
                            }
                        """);

        JSONObject root = (JSONObject) JSONParser.parseJSON(response);
        JSONArray tools = root.getJSONObject("result").getJSONArray("tools");

        JSONObject asyncObjectTool = null;

        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            if ("asyncObjectTool".equals(tool.getString("name"))) {
                asyncObjectTool = tool;
                break;
            }
        }

        assertNotNull("Tool 'asyncObjectTool' should be present in tool list", asyncObjectTool);

        String actualToolJson = asyncObjectTool.toString();

        String expectedToolJson = """
                                                    {
                          "name": "asyncObjectTool",
                          "title": "Async asyncObjectTool",
                          "description": "A tool to return an object of cities asynchronously",
                          "inputSchema": {
                            "type": "object",
                            "properties": {
                              "name": {
                                "type": "string",
                                "description": "name of your city"
                              }
                            },
                            "required": ["name"]
                          },
                          "outputSchema": {
                            "type": "object",
                            "properties": {
                              "country": {"type": "string"},
                              "isCapital": {"type": "boolean"},
                              "name": {"type": "string"},
                              "population": {"type": "integer"}
                            },
                            "required": ["name", "country", "population", "isCapital"]
                          }
                        }
                         """;

        JSONAssert.assertEquals(expectedToolJson, actualToolJson, true);
    }

    @Test
    public void testAsyncToolsWithoutStructuredContentHaveNoOutputSchema() throws Exception {
        String response = client.callMCP("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "method": "tools/list"
                            }
                        """);

        JSONObject root = (JSONObject) JSONParser.parseJSON(response);
        JSONArray tools = root.getJSONObject("result").getJSONArray("tools");

        List<String> toolsThatShouldNotHaveOutputSchema = List.of(
                                                                  "asyncStringTool",
                                                                  "asyncContentTool",
                                                                  "asyncListContentTool",
                                                                  "asyncObjectToolWithoutStructuredContent");

        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            String name = tool.getString("name");

            if (toolsThatShouldNotHaveOutputSchema.contains(name)) {
                assertFalse("Tool '" + name + "' should NOT have an output schema", tool.has("outputSchema"));
            }
        }
    }

    @Test
    public void testAsyncToolsWithBasicOutputTypesShouldNotHaveOutputSchema() throws Exception {
        String response = client.callMCP("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "method": "tools/list"
                            }
                        """);

        JSONObject root = (JSONObject) JSONParser.parseJSON(response);
        JSONArray tools = root.getJSONObject("result").getJSONArray("tools");

        // Tools that SHOULD NOT have an output schema
        List<String> toolsWithoutSchema = List.of(
                                                  "asyncEcho",
                                                  "asyncDelayedEcho",
                                                  "asyncCancellationTool",
                                                  "asyncToolThatNeverCompletes");

        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            String name = tool.getString("name");

            if (toolsWithoutSchema.contains(name)) {
                assertFalse(
                            "Tool '" + name + "' should NOT have an output schema",
                            tool.has("outputSchema"));
            }
        }
    }
}