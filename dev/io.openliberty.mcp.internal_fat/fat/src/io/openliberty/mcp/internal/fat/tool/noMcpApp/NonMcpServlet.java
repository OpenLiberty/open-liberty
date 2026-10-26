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

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.inject.Inject;

/**
 * A CDI-enabled servlet with no MCP tools, no {@code @Tool} annotations, and no
 * {@code ToolManager} injection. Used to verify that the MCP servlet is NOT
 * installed in CDI-enabled web modules that have no MCP participation.
 */
@WebServlet("/ping")
public class NonMcpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    NonMcpBean nonMcpBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
        resp.getWriter().write(nonMcpBean.ping());
    }
}
