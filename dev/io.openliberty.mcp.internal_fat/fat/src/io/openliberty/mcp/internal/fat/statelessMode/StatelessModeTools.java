/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.statelessMode;

import org.mcpjava.server.ImplementationInfo;
import org.mcpjava.server.McpRequest;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;
import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 *
 */
@ApplicationScoped
public class StatelessModeTools {

    @Inject
    ToolStatus toolStatus;

    @Tool(name = "clientInfoInStatelessMode", description = "Returns client info and capabilities when called in stateless mode", structuredContent = false)
    public ToolResponse clientInfoInStatelessMode(McpRequest request) {
        ImplementationInfo clientInfo = request.clientInfo();
        String caps = String.valueOf(request.rawClientCapabilities());
        return ToolResponse.ofText("name=" + clientInfo.name() + " version=" + clientInfo.version() + " caps=" + caps);
    }

    @Tool(name = "blockingEcho", description = "A tool that blocks until explicitly released by the test", structuredContent = false)
    public ToolResponse blockingEcho(
                                     @ToolArg(name = "input", description = "text to echo back") String input,
                                     @ToolArg(name = "latchName", description = "name of countdown latch to use for test") String latchName) {
        toolStatus.signalStarted(latchName); // Signal the test that tool has started
        toolStatus.awaitShouldEnd(latchName); // Wait for the test to release the latch

        // Return echoed text wrapped in ToolResponse
        return ToolResponse.ofText(input);
    }
}
