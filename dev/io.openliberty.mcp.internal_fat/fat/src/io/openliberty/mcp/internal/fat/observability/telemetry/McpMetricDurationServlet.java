/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.observability.telemetry;

import java.io.IOException;

import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/McpMetricDuration")
public class McpMetricDurationServlet extends HttpServlet {

    @Inject
    private McpMetricReader reader;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String toolName = req.getParameter("toolName");

        HistogramPointData data = reader.getToolCallPoint(toolName);
        double duration = data.getSum();

        try (var output = resp.getOutputStream()) {
            output.println(duration);
        }
    }

}
