/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.statelessMode;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 *
 */
@ApplicationScoped
public class StatelessModeTools {

    @Inject
    ToolStatus toolStatus;

    @Tool(name = "blockingEcho", description = "A tool that blocks until explicitly released by the test", structuredContent = false)
    public ToolResponse blockingEcho(
                                     @ToolArg(name = "input", description = "text to echo back") String input,
                                     @ToolArg(name = "latchName", description = "name of countdown latch to use for test") String latchName) {
        toolStatus.signalStarted(latchName); // Signal the test that tool has started
        toolStatus.awaitShouldEnd(latchName); // Wait for the test to release the latch

        // Return echoed text wrapped in ToolResponse
        return ToolResponse.success(new TextContent(input));
    }
}
