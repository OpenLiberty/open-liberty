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
import static org.junit.Assert.assertNull;

import java.util.regex.Pattern;

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
import io.openliberty.mcp.internal.fat.tool.defaultValueApp.DefaultValueApp;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class DefaultValueTest extends FATServletClient {
    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/defaultValueTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "defaultValueTest.war")
                                   .addPackage(DefaultValueApp.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(
                          "CWMCM0034W", // Converter implementation without type parameter
                          "CWMCM0035W" // Converter implementation returns a generic type
        );
    }

    @Test
    public void testToolCallWithToolArgStringDefaultValue() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgStringDefaultValue",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "Jupiter"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithToolArgIntDefaultValue() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgIntDefaultValue",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "2025"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithToolArgCustomTypeDefaultValue() throws Exception {
        String request = """
                          {
                          "id": 2,
                          "jsonrpc": "2.0",
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgCustomTypeDefaultValue",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id": 2,
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
        assertNotNull((server.waitForStringInLog(Pattern.quote("[PriorityCityConverter] City converter with HIGHER priority used"))));
        assertNull(server.waitForStringInLog(Pattern.quote("[CityConverter] City converter with LOWER priority used"), 3000));
    }

    @Test
    public void testToolCallWithToolArgCustomTypeDefaultValueAndInheritedConverter() throws Exception {
        String request = """
                          {
                          "id": 2,
                          "jsonrpc": "2.0",
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgCustomTypeDefaultValueWithInheritedConverter",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id": 2,
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"number\\\":5,\\\"street\\\":\\\"London Road\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithTwoToolArgsWithOneDefaultValue() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testMultipleToolArgsOneDefaultValue",
                            "arguments": {
                              "year": "2000"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "Planet Jupiter was created in the year 2000"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithDependentBeanCustomConverter() throws Exception {
        String request = """
                          {
                          "id": 2,
                          "jsonrpc": "2.0",
                          "method": "tools/call",
                          "params": {
                            "name": "testDependentBeanCustomConverter",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id": 2,
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"age\\\":25,\\\"name\\\":\\\"Joe\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
        assertNull(server.waitForStringInLog(Pattern.quote("[PersonConverterDependentBean] PreDestroy called"), 3000));
    }

    @Test
    public void testWarningsForInvalidCustomConverters() throws Exception {
        assertNotNull(server.waitForStringInLog(Pattern.quote("CWMCM0034W: The class io.openliberty.mcp.internal.fat.tool.defaultValueApp.DefaultValueApp$InvalidNoParameterTypeCustomConverter DefaultValueConverter implementation must specify a type parameter.")));
        assertNotNull(server.waitForStringInLog(Pattern.quote("CWMCM0035W: The class io.openliberty.mcp.internal.fat.tool.defaultValueApp.DefaultValueApp$InvalidGenericConverter DefaultValueConverter implementation converts to a generic type parameter instead of a concrete type.")));
    }

}
