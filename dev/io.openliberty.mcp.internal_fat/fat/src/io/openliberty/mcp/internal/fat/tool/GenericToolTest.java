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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.genericToolApp.ChildConcreteTool;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class GenericToolTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/genericToolTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "genericToolTest.war")
                                   .addPackage(ChildConcreteTool.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/toolTest/mcp"

    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testToolList() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/list"
                        }
                        """;

        String response = client.callMCP(request);

        String expectedString = """
                            {
                            "id": 1,
                            "jsonrpc": "2.0",
                            "result": {
                                "tools": [
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
                                            "properties":{
                                                "returnList":{
                                                    "type": "array",
                                                    "items": {
                                                    "type": "array",
                                                    "items": {
                                                        "type": "string"
                                                    }
                                                   }
                                               }
                                            }
                                            ,
                                            "description": "Returns list of  object",
                                            "required":["returnList"]
                                        },
                                        "title": "adds generic to generic Array"
                                    }
                                ]
                            }
                        }
                            """;
        JSONAssert.assertEquals(expectedString, response, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    public void testChildConcreteClass() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "addGenericToGenericArray",
                            "arguments": {
                                            "generic": "OpenLiberty",
                                            "generic list 1": ["Hello", "World", "IBM", "Liberty"],
                                            "generic list 2": [["Hello", "World"], ["IBM", "Liberty"]]
                                    }
                          }
                        }
                        """;
        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);
        // Strict Mode tests
        String expectedResponseString = """
                            {
                            "id": 2,
                            "jsonrpc": "2.0",
                            "result": {
                                "content": [
                                    {
                                        "text": "{\\\"returnList\\\":[[\\\"Hello\\\",\\\"World\\\"],[\\\"IBM\\\",\\\"Liberty\\\"]]}",
                                        "type": "text"
                                    }
                                ],
                                "isError": false,
                                "structuredContent": {
                                "returnList":[
                                    [
                                        "Hello",
                                        "World"
                                    ],
                                    [
                                        "IBM",
                                        "Liberty"
                                    ]
                                ]}
                            }
                        }
                                                                                    """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }
}