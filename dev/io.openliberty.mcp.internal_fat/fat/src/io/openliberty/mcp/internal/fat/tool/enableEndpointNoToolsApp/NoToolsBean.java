/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.enableEndpointNoToolsApp;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A CDI bean with no {@code @Tool}-annotated methods and no {@code ToolManager} usage, used to check that
 * the MCP endpoint is not opened for a module which has no tools.
 */
@ApplicationScoped
public class NoToolsBean {

    public String hello() {
        return "no tools here";
    }
}
