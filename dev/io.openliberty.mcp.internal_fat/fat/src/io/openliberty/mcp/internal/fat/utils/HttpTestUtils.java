/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import static org.junit.Assert.assertNull;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;

/**
 *
 */
public class HttpTestUtils {
    private static final String ACCEPT_HEADER = "application/json, text/event-stream";
    private static final String MCP_PROTOCOL_HEADER = "MCP-Protocol-Version";
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";

    /**
     * Call MCP server, and get the String response body
     */

    public static String callMCP(LibertyServer server, String path, String jsonRequestBody) throws Exception {
        return new HttpRequest(server, path + "/mcp")
                                                     .requestProp("Accept", ACCEPT_HEADER)
                                                     .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                     .jsonBody(jsonRequestBody)
                                                     .method("POST")
                                                     .run(String.class);

    }

    /**
     * Call MCP server notification endpoint, and provide a 202 expected response code. No response body is returned
     * If a response body is returned, or a response code that is not 202, and exception is thrown
     */
    public static void callMCPNotification(LibertyServer server,
                                           String path,
                                           String jsonRequestBody)
                    throws Exception {

        String response = new HttpRequest(server, path + "/mcp")
                                                                .requestProp("Accept", ACCEPT_HEADER)
                                                                .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                                .jsonBody(jsonRequestBody)
                                                                .method("POST")
                                                                .expectCode(202)
                                                                .run(String.class);

        assertNull("Notification request received a response", response);
    }

}
