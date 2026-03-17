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
import static org.junit.Assert.assertNotNull;

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
import io.openliberty.mcp.internal.fat.tool.nonRequiredArgsApp.NonRequiredArgsTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class NonRequiredArgsToolsTest {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/nonRequiredArgsToolsTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "nonRequiredArgsToolsTest.war")
                                   .addPackage(NonRequiredArgsTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
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
