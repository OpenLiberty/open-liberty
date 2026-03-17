/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.toolManagerApp;

import static org.junit.Assert.assertNull;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;

/**
 * Client for {@link ToolEditorServlet}
 */
public class ToolEditorClient {

    private final LibertyServer server;
    private final String appName;

    public ToolEditorClient(LibertyServer server, String appName) {
        super();
        this.server = server;
        this.appName = appName;
    }

    /**
     * Add a tool named {@code dynamicRepeater} which repeats the input string a number of times.
     *
     * @param repeatCount the number of times the input should be repeated
     * @throws Exception on error
     */
    public void addDynamicRepeaterTool(int repeatCount) throws Exception {
        String request = """
                        {
                            "action":"add",
                            "repeatCount":%d
                        }
                        """.formatted(repeatCount);
        sendRequest(request);
    }

    /**
     * Remove the {@code dynamicRepeater} tool (if one exists)
     *
     * @throws Exception on error
     */
    public void removeDynamicRepeaterTool() throws Exception {
        String request = """
                        {
                            "action": "remove"
                        }
                        """;
        sendRequest(request);
    }

    private void sendRequest(String request) throws Exception {
        String response = new HttpRequest(server, "/", appName, "/", "editTools").method("POST")
                                                                                 .jsonBody(request)
                                                                                 .run(String.class);
        assertNull("response", response);
    }

}
