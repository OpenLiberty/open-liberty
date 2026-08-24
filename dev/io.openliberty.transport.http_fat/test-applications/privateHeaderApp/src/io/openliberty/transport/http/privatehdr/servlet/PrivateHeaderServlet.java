/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.privatehdr.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used by PrivateHeaderFilterTest to expose request metadata that
 * is derived from WAS private ($WS*) headers when a trusted reverse proxy is
 * configured. The servlet echoes:
 *   SERVER_PORT  — from HttpServletRequest.getServerPort()
 *   SERVER_NAME  — from HttpServletRequest.getServerName()
 *   REQUEST_URL  — from HttpServletRequest.getRequestURL()
 *   REMOTE_ADDR  — from HttpServletRequest.getRemoteAddr()
 *   HDR_WSRA     — raw value of the $WSRA header as seen by the servlet
 *                  (null when stripped before the servlet layer)
 */
@WebServlet("/PrivateHeaderServlet")
public class PrivateHeaderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        writer.println("SERVER_PORT=" + request.getServerPort());
        writer.println("SERVER_NAME=" + request.getServerName());
        writer.println("REQUEST_URL=" + request.getRequestURL().toString());
        writer.println("REMOTE_ADDR=" + request.getRemoteAddr());
        writer.println("HDR_WSRA=" + request.getHeader("$WSRA"));
        writer.flush();
    }
}
