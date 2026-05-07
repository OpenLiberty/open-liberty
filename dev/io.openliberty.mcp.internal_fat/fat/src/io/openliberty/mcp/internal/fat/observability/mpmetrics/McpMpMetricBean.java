/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.observability.mpmetrics;

import io.openliberty.mcp.annotations.Tool;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class McpMpMetricBean {
    @Tool(name = "basicTool", title = "Basic Tool", description = "Simple tool to test mpMetrics")
    public String basicTool() {
        return "Hello from this basic tool";
    }

    @Tool(name = "advancedTool", title = "Advanced Tool", description = "Advanced tool to test mpMetrics")
    public String advancedTool() {
        return "Hello from this advanced tool";
    }

}
