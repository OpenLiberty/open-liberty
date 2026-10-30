
/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.downgrade.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for testing protocol downgrade and fallback paths.
 * Echoes request details and supports returning custom HTTP status codes (e.g. 403, 400)
 * when protocol upgrades (such as WebSocket or h2c) are refused.
 */
@WebServlet("/DowngradeFallbackServlet")
public class DowngradeFallbackServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String RESPONSE_MARKER = "DOWNGRADE_FALLBACK_OK";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String statusParam = request.getParameter("status");
        if (statusParam != null && !statusParam.isEmpty()) {
            try {
                int status = Integer.parseInt(statusParam);
                response.setStatus(status);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        response.setContentType("text/plain");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        pw.println(RESPONSE_MARKER);
        pw.println("method=" + request.getMethod());
        pw.println("query=" + request.getQueryString());
        pw.println("upgradeHeader=" + request.getHeader("Upgrade"));
        pw.println("connectionHeader=" + request.getHeader("Connection"));
        pw.flush();

        byte[] body = baos.toByteArray();
        response.setContentLength(body.length);
        OutputStream out = response.getOutputStream();
        out.write(body);
        out.flush();
    }
}
