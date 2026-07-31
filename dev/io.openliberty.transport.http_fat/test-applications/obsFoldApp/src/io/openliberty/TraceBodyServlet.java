/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat.obsfold.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used to test TRACE-body protection.
 *
 * <ul>
 *   <li>TRACE: returns HTTP 200 with a recognisable marker so the test can
 *       confirm the TRACE itself was handled.</li>
 *   <li>GET:   returns HTTP 200 with a different marker so the test can detect
 *       whether a boddy with a request was dispatched on the same connection.</li>
 * </ul>
 *
 * Both methods keep the connection alive (default HTTP/1.1 behaviour) so that
 * the server's decision to setPersistent(false) is what prevents the body
 * request from being processed on the same back-end connection.
 */
@WebServlet("/TraceBodyServlet")
public class TraceBodyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /** Marker written for TRACE responses — test looks for exactly this string. */
    public static final String TRACE_MARKER = "TRACE_RESPONSE_MARKER";

    /** Marker written for GET responses — test looks for exactly this string. */
    public static final String GET_MARKER = "SMUGGLED_GET_MARKER";

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        writer.println(TRACE_MARKER);
        writer.flush();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        writer.println(GET_MARKER);
        writer.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        writer.println(GET_MARKER);
        writer.flush();
    }
}