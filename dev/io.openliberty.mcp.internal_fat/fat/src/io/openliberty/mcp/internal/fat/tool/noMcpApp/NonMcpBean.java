/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.noMcpApp;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A plain CDI bean with no MCP involvement. Its presence ensures that
 * the {@code NonMcpApp} module is CDI-enabled, making it a meaningful
 * test that the MCP servlet is not installed in CDI apps with no MCP tools.
 */
@ApplicationScoped
public class NonMcpBean {

    public String ping() {
        return "pinged";
    }
}
