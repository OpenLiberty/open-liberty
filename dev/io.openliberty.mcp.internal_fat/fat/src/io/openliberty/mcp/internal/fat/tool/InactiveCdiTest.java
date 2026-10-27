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
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.inactiveCdiApp.InactiveCdiTool;

@RunWith(FATRunner.class)
public class InactiveCdiTest extends FATServletClient {
    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "inactiveCdiTest";
    private static final String APPLICATION_NAME = APP_NAME;
    private static final String CDI_INACTIVE_TRACE_MESSAGE = "The MCP server endpoint for the application " + APPLICATION_NAME
                                                             + " is unavailable due to CDI being inactive.";
    private static final String FILE_NOT_FOUND_ERROR_MESSAGE = "SRVE0190E: File not found: /mcp";

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        serverLifecycle.getServer().addIgnoredErrors(List.of(FILE_NOT_FOUND_ERROR_MESSAGE));
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackage(InactiveCdiTool.class.getPackage());
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testMcpCallWithoutCDIReturnsNotFoundError() throws Exception {
        assertNotNull(serverLifecycle.getServer().waitForStringInTraceUsingMark(CDI_INACTIVE_TRACE_MESSAGE));
        String initializeRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {
                              "roots": {
                                "listChanged": true
                              },
                              "sampling": {},
                              "elicitation": {}
                            },
                            "clientInfo": {
                              "name": "FAT Test Client",
                              "title": "FAT Test Client",
                              "version": "1.0.0"
                            }
                          }
                        }
                        """;

        new HttpRequest(serverLifecycle.getServer(), "/inactiveCdiTest/mcp")
                                                                            .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                            .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                                            .jsonBody(initializeRequest)
                                                                            .method("POST")
                                                                            .expectCode(404)
                                                                            .run(String.class);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark(FILE_NOT_FOUND_ERROR_MESSAGE, serverLifecycle.getServer().getDefaultLogFile())); // expect a file not found error in the logs
    }
}
