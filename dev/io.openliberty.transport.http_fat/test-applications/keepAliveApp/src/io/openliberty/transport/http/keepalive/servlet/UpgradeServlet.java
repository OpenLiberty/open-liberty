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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used by KeepAliveEnforcementTest to exercise a Servlet 3.1
 * {@code HttpUpgradeHandler} upgrade.
 *
 * <p>When the request carries an {@code Upgrade} header the servlet answers
 * {@code 101 Switching Protocols} and hands the connection to an echo handler.
 * By default that is {@link EchoUpgradeHandler}, which uses a
 * {@code ReadListener}. With {@code ?mode=blocking} it is
 * {@link EchoBlockingUpgradeHandler}, which uses plain blocking reads and no
 * listener. The two take different paths when the server closes connections.
 * Both follow the servlet upgrade samples in the servlet 3.1 FAT bucket.
 *
 * <p>The point of the test is what happens to the <em>socket</em> afterwards.
 * Once upgraded, the connection belongs to the application protocol, so it must
 * stay open even when {@code keepAliveEnabled=false} or
 * {@code maxKeepAliveRequests} has been reached.
 */
@WebServlet("/UpgradeServlet")
public class UpgradeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getHeader("Upgrade") != null) {
            response.setStatus(HttpServletResponse.SC_SWITCHING_PROTOCOLS);
            response.setHeader("Upgrade", "KeepAliveEcho");
            response.setHeader("Connection", "Upgrade");
            if ("blocking".equals(request.getParameter("mode"))) {
                request.upgrade(EchoBlockingUpgradeHandler.class);
            } else {
                request.upgrade(EchoUpgradeHandler.class);
            }
        } else {
            response.setContentType("text/plain");
            response.getWriter().println("No Upgrade header.");
        }
    }
}
