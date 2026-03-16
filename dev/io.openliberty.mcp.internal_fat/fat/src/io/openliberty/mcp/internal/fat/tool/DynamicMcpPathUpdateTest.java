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
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.McpServer;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;

@RunWith(FATRunner.class)
public class DynamicMcpPathUpdateTest extends FATServletClient {
    @Server("mcp-server-dynamic")
    public static LibertyServer server;

    private static final String APP_NAME = "dynamicMcpPathUpdateTest";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage());
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);
        server.saveServerConfiguration();
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        } finally {
            server.stopServer(
                              "SRVE0190E" //File not found: /dynamic-mcp
            );
        }
    }

    private String initializeSession(String mcpEndpoint) throws Exception {
        String request = """
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
        HttpRequest httpRequest = new HttpRequest(server, "/" + APP_NAME + mcpEndpoint)
                                                                                       .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                       .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                                                       .jsonBody(request)
                                                                                       .method("POST")
                                                                                       .expectCode(200);
        httpRequest.run(String.class);

        String sessionId = httpRequest.getResponseHeader(MCP_SESSION_ID);
        assertNotNull("Expected a session ID in response", sessionId);
        return sessionId;
    }

    private String toolsList(String mcpEndpoint, String sessionId) throws Exception {
        String request = """
                           {
                           "jsonrpc": "2.0",
                           "id": "2",
                           "method": "tools/list"
                         }
                        """;
        return new HttpRequest(server, "/" + APP_NAME + mcpEndpoint)
                                                                    .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                    .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                                    .requestProp(MCP_SESSION_ID, sessionId)
                                                                    .jsonBody(request)
                                                                    .method("POST")
                                                                    .expectCode(200)
                                                                    .run(String.class);
    }

    private void deleteSession(String mcpEndpoint, String sessionId) throws Exception {
        new HttpRequest(server, "/" + APP_NAME + mcpEndpoint)
                                                             .requestProp(MCP_SESSION_ID, sessionId)
                                                             .method("DELETE")
                                                             .run(String.class);
    }

    private void assertEndpointNotFound(String mcpEndpoint) throws Exception {
        String request = """
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
        new HttpRequest(server, "/" + APP_NAME + mcpEndpoint)
                                                             .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                             .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                             .jsonBody(request)
                                                             .method("POST")
                                                             .expectCode(404)
                                                             .run(String.class);
    }

    @Test
    public void testAppRestartsWhenMcpServerPathcConfigChanges() throws Exception {

        String initialEndpoint = "/dynamic-mcp";
        String updatedEndpoint = "/dynamic-mcp-updated";

        //Initialize session
        String sessionId = initializeSession(initialEndpoint);

        //confirm endpoint is live with listTools call
        String toolResponse = toolsList(initialEndpoint, sessionId);
        assertNotNull("Expected tool/list response", toolResponse);

        //clean up session
        deleteSession(initialEndpoint, sessionId);

        //set the log mark before updating config
        server.setMarkToEndOfLog();

        //dynamically update the endpoint path to a new path
        ServerConfiguration config = server.getServerConfiguration();
        Application app = config.getApplications().getBy("location", APP_NAME + ".war");

        assertNotNull("Expected to find the application in server config", app);

        McpServer mcpServer = app.getMcpServers().get(0);
        mcpServer.setPath(updatedEndpoint);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        //initialize session
        String sessionId2 = initializeSession(updatedEndpoint);

        //confirm endpoint is live with listTools call
        String toolResponse2 = toolsList(updatedEndpoint, sessionId2);
        assertNotNull("Expected tool/list response", toolResponse2);

        //confirm old path is no longer available
        assertEndpointNotFound(initialEndpoint);

        //clean up session
        deleteSession(updatedEndpoint, sessionId2);
    }

}
