/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.keepalive.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used by KeepAliveEnforcementTest.
 *
 * <p>The test sends several requests down a single raw socket and has to read
 * each response off that same socket. That is only straightforward when every
 * response has a known length, so this servlet always sets an explicit
 * {@code Content-Length} header. Without it the container may choose chunked
 * transfer encoding, which would force the test to parse chunks just to find
 * where one response ends and the next begins.
 *
 * <p>The servlet echoes the {@code req} query parameter so a test can tell
 * which response it is reading.
 */
@WebServlet("/KeepAliveServlet")
public class KeepAliveServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String req = request.getParameter("req");
        byte[] body = ("KeepAliveServlet req=" + req + "\n").getBytes(StandardCharsets.UTF_8);

        response.setContentType("text/plain");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.getOutputStream().flush();
    }
}
