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

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class McpUrlPathTest extends FATServletClient {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "mcpUrlPathTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    private static final String REQUEST = """
                      {
                      "jsonrpc": "2.0",
                      "id": "2",
                      "method": "tools/call",
                      "params": {
                        "name": "echo",
                        "arguments": {
                          "input": "Hello"
                        }
                      }
                    }
                    """;
    private static final String EXPECTED_ERROR_MESSAGE = "SRVE0190E: File not found: /mcpUrlPathTest/mcp/something-else";

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        serverLifecycle.getServer().addIgnoredErrors(List.of(EXPECTED_ERROR_MESSAGE));
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackage(BasicTools.class.getPackage());
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testMcpEndpointWithTrailingForwardSlash() throws Exception {
        String response = client.callMCPRaw(REQUEST).mcpPath("/mcp/").run().body(); // url /mcp/ will be handled correctly
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);

    }

    @Test
    public void testMcpEndpointWithTrailingForwardSlashAndMoreTestAppended() throws Exception {

        int expectedErrorCode = 404;
        client.callMCPCustomized(REQUEST, "/mcpUrlPathTest/mcp/something-else", expectedErrorCode);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark(EXPECTED_ERROR_MESSAGE, serverLifecycle.getServer().getDefaultLogFile()));
    }

}
