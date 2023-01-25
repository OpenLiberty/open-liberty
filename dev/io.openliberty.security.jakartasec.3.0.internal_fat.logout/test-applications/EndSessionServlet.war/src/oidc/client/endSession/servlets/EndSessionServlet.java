/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.endSession.servlets;

import java.io.IOException;
import java.util.Enumeration;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oidc.client.base.utils.ServletLogger;

@WebServlet("/end_session")
public class EndSessionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public EndSessionServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ServletOutputStream outputStream = response.getOutputStream();
        ServletLogger.printLine(outputStream, "Reached EndSessionServlet");

        Enumeration<String> parmNames = request.getParameterNames();
        while (parmNames.hasMoreElements()) {
            String key = parmNames.asIterator().next();
            ServletLogger.printLine(outputStream, "EndSessionServlet - parmKey: " + key + " parmValue: " + request.getParameter(key));
        }
    }

}
