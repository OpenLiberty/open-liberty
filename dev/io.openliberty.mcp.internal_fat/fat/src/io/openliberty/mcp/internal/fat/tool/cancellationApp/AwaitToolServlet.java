/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.cancellationApp;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Allows the test to wait for a tool to start running, so that it can be cancelled
 */
@SuppressWarnings("serial")
@WebServlet("/awaitTool/*")
public class AwaitToolServlet extends HttpServlet {

    @Inject
    private ToolStatus toolStatus;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String latchName = getPathParam(req);
            toolStatus.awaitRunning(latchName);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private String getPathParam(HttpServletRequest req) {
        String pathParam = req.getPathInfo();
        if (pathParam != null && pathParam.length() > 1) {
            //removes the "/" from the start of the path param
            return pathParam.substring(1);
        }
        throw new IllegalArgumentException("No path parameter found for awaitTool request");
    }

}
