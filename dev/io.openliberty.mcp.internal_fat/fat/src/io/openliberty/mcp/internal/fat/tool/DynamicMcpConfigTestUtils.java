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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;

/**
 * Shared helpers for the tests that check how the {@code <mcp path="...">} element of an
 * application is picked up when the {@code mcp-1.0} feature and the application configuration
 * arrive at different times or in different files.
 */
final class DynamicMcpConfigTestUtils {

    static final String APP_NAME = "dynamicMcpPathUpdateTest";

    /** The path configured with {@code <mcp path="...">} in every server configuration these tests use. */
    static final String MCP_PATH = "/dynamic-mcp-updated";

    /** The serverInfo name configured with {@code <info name="...">} in every server configuration these tests use. */
    static final String SERVER_INFO_NAME = "ibm-mq-mcp-server";

    private static final String INITIALIZE_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": "1",
                      "method": "initialize",
                      "params": {
                        "protocolVersion": "2025-11-25",
                        "capabilities": {},
                        "clientInfo": {
                          "name": "FAT Test Client",
                          "title": "FAT Test Client",
                          "version": "1.0.0"
                        }
                      }
                    }
                    """;

    private DynamicMcpConfigTestUtils() {}

    /** Exports an EAR containing {@link #APP_NAME}.war to the server's apps directory. */
    static void deployEar(LibertyServer server) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage());
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                                          .addAsModule(war);
        ShrinkHelper.exportAppToServer(server, ear, new DeployOptions[] { SERVER_ONLY, DISABLE_VALIDATION });
    }

    /** Asserts that the configured MCP path is not served (404). */
    static void assertEndpointNotFound(LibertyServer server) throws Exception {
        newInitializeRequest(server).expectCode(404).run(String.class);
    }

    /** Asserts that the configured MCP path serves an MCP session, then closes the session. */
    static void assertEndpointLive(LibertyServer server) throws Exception {
        HttpRequest request = newInitializeRequest(server).expectCode(200);
        String response = request.run(String.class);
        assertServerInfoName(response);

        String sessionId = request.getResponseHeader(MCP_SESSION_ID);
        assertNotNull("Expected a session ID in the response from " + MCP_PATH, sessionId);

        new HttpRequest(server, "/" + APP_NAME + MCP_PATH)
                                                          .requestProp(MCP_SESSION_ID, sessionId)
                                                          .method("DELETE")
                                                          .run(String.class);
    }

    /** Asserts that the initialize response carries the serverInfo name configured with {@code <info name="...">}. */
    static void assertServerInfoName(String initializeResponse) throws Exception {
        JSONAssert.assertEquals("Expected the configured serverInfo name in the initialize response",
                                "{\"result\":{\"serverInfo\":{\"name\":\"" + SERVER_INFO_NAME + "\"}}}",
                                initializeResponse, JSONCompareMode.LENIENT);
    }

    private static HttpRequest newInitializeRequest(LibertyServer server) {
        return new HttpRequest(server, "/" + APP_NAME + MCP_PATH)
                                                                 .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                 .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                                 .jsonBody(INITIALIZE_REQUEST)
                                                                 .method("POST");
    }
}
