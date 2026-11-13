/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.vhost.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet deployed on the named virtual host {@code publicVhost} and used by
 * virtual-host routing tests.  The {@code APP_ID=vhost} field lets the test
 * distinguish a response from this application from one produced by
 * {@code privateHeaderApp} (which does not emit that field).
 */
@WebServlet("/VhostServlet")
public class VhostServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        writer.println("APP_ID=vhost");
        writer.println("SERVER_NAME=" + request.getServerName());
        writer.println("REQUEST_URL=" + request.getRequestURL().toString());
        writer.println("SERVER_PORT=" + request.getServerPort());
        writer.flush();
    }
}
