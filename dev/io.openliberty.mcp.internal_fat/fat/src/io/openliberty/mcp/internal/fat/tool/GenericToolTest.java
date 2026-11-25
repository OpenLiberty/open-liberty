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

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher.toolResponse;
import static io.openliberty.mcp.internal.fat.utils.ToolsListResponseMatcher.toolsList;
import static org.hamcrest.MatcherAssert.assertThat;
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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.genericToolApp.ChildConcreteTool;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolResponse;

/**
 *
 */
@RunWith(FATRunner.class)
public class GenericToolTest extends FATServletClient {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "genericToolTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();
    
    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(ChildConcreteTool.class.getPackage());
        serverLifecycle.deployDropinApp(war);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testToolList() throws Exception {
        JSONArray expectedTools = new JSONArray("""
                        [
                          {
                            "description": "adds person to Generic Array, returns nothing",
                            "inputSchema": {
                              "type": "object",
                              "properties": {
                                "generic list 2": {
                                  "type": "array",
                                  "items": {
                                    "type": "array",
                                    "items": {
                                      "type": "string"
                                    }
                                  },
                                  "description": "List of generics 1 "
                                },
                                "generic": {
                                  "type": "string",
                                  "description": "Generic object"
                                },
                                "generic list 1": {
                                  "type": "array",
                                  "items": {
                                    "type": "string"
                                  },
                                  "description": "List of generics 1"
                                }
                              },
                              "required": [
                                "generic list 2",
                                "generic",
                                "generic list 1"
                              ]
                            },
                            "name": "addGenericToGenericArray",
                            "outputSchema": {
                              "type": "object",
                              "properties": {
                                "returnList": {
                                  "type": "array",
                                  "items": {
                                    "type": "array",
                                    "items": {
                                      "type": "string"
                                    }
                                  }
                                }
                              },
                              "description": "Returns list of  object",
                              "required": ["returnList"]
                            },
                            "title": "adds generic to generic Array"
                          }
                        ]
                        """);

        assertThat(client.call(client.request("tools/list", null)),
                   toolsList().withTools(expectedTools));
    }

    @Test
    public void testChildConcreteClass() throws Exception {
        ToolResponse response = client.callTool("addGenericToGenericArray",
                                                Map.of("generic", "OpenLiberty",
                                                       "generic list 1", List.of("Hello", "World", "IBM", "Liberty"),
                                                       "generic list 2", List.of(List.of("Hello", "World"), List.of("IBM", "Liberty"))));

        String expectedReturnList = "{\"returnList\":[[\"Hello\",\"World\"],[\"IBM\",\"Liberty\"]]}";
        assertThat(response, toolResponse().withTextContent(expectedReturnList)
                                           .withStructuredContent(new JSONObject(expectedReturnList)));
    }
}