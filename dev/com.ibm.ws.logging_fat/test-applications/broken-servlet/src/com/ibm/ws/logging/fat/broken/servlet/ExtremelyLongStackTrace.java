/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat.broken.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The ExtremelyLongStackTrace Servlet throws an exception which contains a very long text.
 */
@WebServlet("/ExtremelyLongStackTrace")
public class ExtremelyLongStackTrace extends HttpServlet {
    private final String EXTREMELY_LONG_TEXT = "ExtremelyLongText ";
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ExtremelyLongStackTrace() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     * @param request can include parameter 'size', which specifies the minimum length of the error message
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        response.getWriter().println("You have now entered the ExtremelyLongStackTrace servlet..");

        // Set default length of error message (if not specified in request)
        int minimumLengthOfErrorMessage = 256 * 1024;

        // Check if size parameter was included in request
        if (request.getParameter("size") != null) {
            try {
                minimumLengthOfErrorMessage = Integer.valueOf(request.getParameter("size"));
            } catch (NumberFormatException e) {
            }
        }
        String errorToOutput = EXTREMELY_LONG_TEXT;
        while (errorToOutput.length() < minimumLengthOfErrorMessage)
            errorToOutput += EXTREMELY_LONG_TEXT;
        System.err.println(errorToOutput);
        response.getWriter().println("Check your logs, there should be an extremely long text.");

    }

}