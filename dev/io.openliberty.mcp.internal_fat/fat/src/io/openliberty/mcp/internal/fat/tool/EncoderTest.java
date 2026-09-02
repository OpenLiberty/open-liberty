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
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
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
        server.stopServer(
          "CWMCM0019E" // encoder excepetion
        ); 
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

    /**
     * Test that @Priority annotation is NOT inherited from superclass.
     * This verifies the fix where priority is now captured from ProcessManagedBean.getAnnotatedBeanClass()
     * instead of walking the superclass hierarchy at runtime.
     *
     *
     * The encoder with highest priority (BaseEncoderWithPriority) should be used.
     */
    @Test
    public void testPriorityNotInheritedFromSuperclass() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testPriorityNotInherited",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // Should use BaseEncoderWithPriority (priority 200), not SubclassEncoderNoPriority
        // which would incorrectly inherit priority 200 with the old buggy implementation
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"message\\\":\\\"Encoded by BaseEncoderWithPriority (priority 50)\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testCdiBasePriorityHighest() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testCdiBasePriorityHighest",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);

        // The CDI base encoder has @Priority(400), while the subclass has @Priority(50),
        // so the base encoder should be selected.
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"message\\\":\\\"Encoded by CdiBaseEncoderWithHighestPriority (priority 400)\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    /**
     * Tests that a ToolResponseEncoder registered for interface IShape is selected
     * when the tool return type is a concrete class (Shape) that directly implements IShape.
     * The encoder output reflects the runtime class name of the returned instance.
     */
    @Test
    public void testGetTypeExactMatch() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testGetTypeExactMatch",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"encoded by ShapeEncoder: Shape"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    /**
     * Tests that a ToolResponseEncoder registered for interface IShape is selected
     * when the tool return type is a different implementing record (Circle).
     * This validates the getType().isInstance(result) dispatch semantics.
     * The encoder output reflects the concrete runtime class name (Circle).
     */
    @Test
    public void testGetTypeSubtypeMatch() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testGetTypeSubtypeMatch",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"encoded by ShapeEncoder: Circle"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    /**
     * Tests that when a ContentEncoder throws an exception, CWMCM0019E is logged
     * (not the generic CWMCM0010E "tool method threw unexpected exception").
     */
    @Test
    @AllowedFFDC("io.openliberty.mcp.internal.fat.tool.encoderToolApp.EncoderTools$SimulatedEncoderFailureException")
    public void testThrowingContentEncoderLogsCorrectError() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testThrowingContentEncoder",
                            "arguments": {}
                          }
                        }
                        """;

        client.callMCP(request);

        assertTrue("Expected CWMCM0019E to be logged when ContentEncoder throws",
                   !server.findStringsInLogs("CWMCM0019E.*ThrowingContentEncoder.*testThrowingContentEncoder").isEmpty());

        assertTrue("Expected CWMCM0010E NOT to be logged for an encoder exception",
                   server.findStringsInLogs("CWMCM0010E.*testThrowingContentEncoder").isEmpty());
    }

    /**
     * Tests that when a ToolResponseEncoder throws an exception, CWMCM0019E is logged
     * (not the generic CWMCM0010E "tool method threw unexpected exception").
     */
    @Test
    @AllowedFFDC("io.openliberty.mcp.internal.fat.tool.encoderToolApp.EncoderTools$SimulatedEncoderFailureException")
    public void testThrowingToolResponseEncoderLogsCorrectError() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testThrowingToolResponseEncoder",
                            "arguments": {}
                          }
                        }
                        """;

        client.callMCP(request);

        assertTrue("Expected CWMCM0019E to be logged when ToolResponseEncoder throws",
                   !server.findStringsInLogs("CWMCM0019E.*ThrowingToolResponseEncoder.*testThrowingToolResponseEncoder").isEmpty());

        assertTrue("Expected CWMCM0010E NOT to be logged for an encoder exception",
                   server.findStringsInLogs("CWMCM0010E.*testThrowingToolResponseEncoder").isEmpty());
    }
}
