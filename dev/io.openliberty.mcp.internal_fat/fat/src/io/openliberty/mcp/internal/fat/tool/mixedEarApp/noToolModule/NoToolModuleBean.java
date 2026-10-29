/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.mixedEarApp.noToolModule;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Web module that has a CDI bean but registers <em>no</em> MCP tools.
 * This module should <em>not</em> receive an MCP endpoint.
 */
@ApplicationScoped
public class NoToolModuleBean {
    // Intentionally empty — no @Tool methods, no ToolManager calls.
}
