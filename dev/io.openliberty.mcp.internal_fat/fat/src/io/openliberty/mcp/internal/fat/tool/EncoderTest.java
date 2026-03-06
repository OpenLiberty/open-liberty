/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.encoderToolApp.EncoderTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class EncoderTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/encoderTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "encoderTest.war")
                                   .addPackage(EncoderTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/encoderTest/mcp"

    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testFallbackBuiltInJsonTextContentEncoder() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testDefaultEncoderResponse",
                            "arguments": {
                              "name": "Manchester"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"country\\\":\\\"England\\\",\\\"isCapital\\\":false,\\\"name\\\":\\\"Manchester\\\",\\\"population\\\":8000}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testContentEncoder() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testContentEncoder",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"age\\\":32,\\\"fistName\\\":\\\"Jon\\\",\\\"lastName\\\":\\\"Encoded by PersonContentEncoder\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testContentEncoderListEncoding() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testContentEncoderEncodingAList",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"age\\\":32,\\\"fistName\\\":\\\"Jon\\\",\\\"lastName\\\":\\\"Encoded by PersonContentEncoder\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEncoderPriority() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testEncoderPriority",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"hello\\\":\\\"Hello from HigerPriorityEncoder\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolResponseEncoderSuccess() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testToolResponseEncoder",
                            "arguments": {
                                   "isSuccessful": true
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"[{\\"country\\":\\"England\\",\\"isCapital\\":true,\\"name\\":\\"London\\",\\"population\\":18000},{\\"country\\":\\"England\\",\\"isCapital\\":false,\\"name\\":\\"Machester\\",\\"population\\":8000}]"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolResponseEncoderErrorResponse() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testToolResponseEncoder",
                            "arguments": {
                                   "isSuccessful": false
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"Database Query failed with error: Some SQL execution error"
                              }
                            ],
                            "isError": true
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testDependentBeanEncoder() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testDependantBeanEncoder",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"hello\\\":\\\"Hello from DependantBeanEncoder\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testRequestScopedBeanEncoder() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testRequestScopedBeanEncoder",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"hello\\\":\\\"Hello from RequestScopedBeanEncoder\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testSessionScopedBeanEncoder() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testSessionScopedBeanEncoder",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"hello\\\":\\\"Hello from SessionScopedBeanEncoder\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testSingletonBeanEncoderWillNOTBeDiscovered() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testSingletonBeanEncoder",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"hello\\\":\\\"Hello\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolResponseEncoderOverContentEncoderSuccess() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testToolResponseEncoderPriorityOverContentEncoder",
                            "arguments": {
                                   "isSuccessful": true
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\"content\\":\\"Encoded by RestResponseToolResponseEncoder\\",\\"isSuccessfull\\":true,\\"statusCode\\":200}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

}
