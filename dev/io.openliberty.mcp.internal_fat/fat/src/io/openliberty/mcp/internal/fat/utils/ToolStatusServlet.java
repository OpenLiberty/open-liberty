/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interface to let the test call methods on {@link ToolStatus}
 * <p>
 * Test client should make a request like this:
 *
 * <pre>
 * POST / myApp / toolStatus / (method) / (latchName)
 * </pre>
 */
@WebServlet("/toolStatus/*")
public class ToolStatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private ToolStatus toolStatus;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String[] parsedPath = parsePath(req);
            String method = parsedPath[0];
            String latchName = parsedPath[1];

            switch (method) {
                case "signalStarted" -> toolStatus.signalStarted(latchName);
                case "awaitStarted" -> toolStatus.awaitStarted(latchName);
                case "signalShouldEnd" -> toolStatus.signalShouldEnd(latchName);
                case "awaitShouldEnd" -> toolStatus.awaitShouldEnd(latchName);
                default -> throw new IllegalArgumentException("No such method: " + method);
            }
        } catch (Exception | AssertionError e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain");
            e.printStackTrace(resp.getWriter());
        }
    }

    private String[] parsePath(HttpServletRequest req) {
        String pathParam = req.getPathInfo();
        Pattern pattern = Pattern.compile("/(\\w+)/(\\w+)");
        Matcher m = pattern.matcher(pathParam);
        if (m.matches()) {
            String[] result = { m.group(1), m.group(2) };
            return result;
        } else {
            throw new IllegalArgumentException("Invalid URL");
        }
    }
}
