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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;

@RunWith(FATRunner.class)
public class DynamicMcpPathUpdateUsingServerXMLIncludesTest extends FATServletClient {

    private static final String INCLUDE_FILE = "server-dynamic-app.xml";

    @Server("mcp-server-dynamic-xml-includes")
    public static LibertyServer server;

    private static final String APP_NAME = "dynamicMcpPathUpdateTest";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage());
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                                          .addAsModule(war);
        ShrinkHelper.exportAppToServer(server, ear, new DeployOptions[] {SERVER_ONLY, DISABLE_VALIDATION});
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            server.deleteFileFromLibertyServerRoot(INCLUDE_FILE);
        } finally {
            server.stopServer();
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
    public void testMcpEndpointBecomesAvailableWhenIncludeFileIsDroppedIn() throws Exception {

        String includedEndpoint = "/dynamic-mcp-updated";

        // Step 1: server has no application configured (the optional include is absent),
        //         so the MCP endpoint should not be reachable
        assertEndpointNotFound(includedEndpoint);

        // Step 2: drop server-dynamic-app.xml into the server config directory so
        //         the optional include is picked up and triggers a config update
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot(INCLUDE_FILE);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // Step 3: verify the endpoint defined in server-dynamic-app.xml is now live
        String sessionId = initializeSession(includedEndpoint);

        String toolResponse = toolsList(includedEndpoint, sessionId);
        assertNotNull("Expected tool/list response", toolResponse);

        deleteSession(includedEndpoint, sessionId);
    }
}
