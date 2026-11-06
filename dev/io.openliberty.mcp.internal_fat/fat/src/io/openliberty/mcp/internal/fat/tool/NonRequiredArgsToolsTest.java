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

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.tool.nonRequiredArgsApp.NonRequiredArgsTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class NonRequiredArgsToolsTest {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "nonRequiredArgsToolsTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();
    
    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(NonRequiredArgsTools.class.getPackage());
        serverLifecycle.deployDropinApp(war);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testNonRequiredArgFalseMissingArgs() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "3",
                            "method": "tools/call",
                            "params": {
                              "name": "toolArgRequiredFalse",
                              "arguments": {}
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello toolArgRequiredFalse"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testNonRequiredArgFalseProvidedArgs() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "3",
                            "method": "tools/call",
                            "params": {
                              "name": "toolArgRequiredFalse",
                              "arguments": {
                                "input" : "Awesome"
                              }
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello from: Awesome"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testDefaultStringArgsProvided() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "3",
                            "method": "tools/call",
                            "params": {
                              "name": "toolArgDefaultValueSetString",
                              "arguments": {
                                "input" : "Awesome"
                              }
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello from: Awesome"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testDefaultStringArgsNotProvided() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "3",
                            "method": "tools/call",
                            "params": {
                              "name": "toolArgDefaultValueSetString",
                              "arguments": {}
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello from: The Default Value"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolArgWithOptionalValue() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "3",
                            "method": "tools/call",
                            "params": {
                              "name": "toolArgWithOptionalValue",
                              "arguments": {}
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello World"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolArgWithOptionalValueArgumentsPresent() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "3",
                            "method": "tools/call",
                            "params": {
                              "name": "toolArgWithOptionalValue",
                              "arguments": {
                              "input" : "Awesome"
                              }
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello from: Awesome"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

}
