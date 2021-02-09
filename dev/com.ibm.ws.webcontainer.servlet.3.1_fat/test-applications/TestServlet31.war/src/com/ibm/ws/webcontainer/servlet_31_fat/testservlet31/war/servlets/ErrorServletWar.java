/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for generating an error. Intended for testing error pages, including default error pages.
 *
 * If a "sendError" parameter is received and is true, send back the error code '404' (HttpServletResponse.SC_NOT_FOUND).
 *
 * If a "sendError" parameter is not received, or is received but is not true, throw a ServletException.
 */
@WebServlet("/ErrorServlet")
public class ErrorServletWar extends HttpServlet {
    /** Default serialization ID. */
    private static final long serialVersionUID = 1L;

    /** Standard constructor. */
    public ErrorServletWar() {
        super();
    }

    /** Control parameter: If present and true, set an error code. Otherwise, throw a servlet exception. */
    public static final String SEND_ERROR_PARAMETER_NAME = "sendError";

    /**
     * Override: Send the error code if request parameter {@link #SEND_ERROR_PARAMETER_NAME} is received and is true. Otherwise, throw a {@link ServletException}.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();

        sos.println("ErrorServlet");

        String sendErrorParam = request.getParameter(SEND_ERROR_PARAMETER_NAME);
        if ((sendErrorParam != null) && Boolean.valueOf(sendErrorParam)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                               SEND_ERROR_PARAMETER_NAME + "received; generating error code " + Integer.toString(HttpServletResponse.SC_NOT_FOUND));
        } else {
            throw new ServletException(SEND_ERROR_PARAMETER_NAME + " not received; generating ServletException");
        }
    }

    /**
     * Override: Implement as a call to {@link #doGet}.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
