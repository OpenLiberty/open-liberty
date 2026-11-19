/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.enableEndpointAnnotatedApp;

import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A bean with a {@code @Tool}-annotated method, used to check that
 * {@code enableEndpoint="auto"} opens the endpoint when annotated tools exist.
 */
@ApplicationScoped
public class AnnotatedToolsBean {

    @Tool(name = "annotated-echo", description = "Returns the input unchanged")
    public String annotatedEcho(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }
}
