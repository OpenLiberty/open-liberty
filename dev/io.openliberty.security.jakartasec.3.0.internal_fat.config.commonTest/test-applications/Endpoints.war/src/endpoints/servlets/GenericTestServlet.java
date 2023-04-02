/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package endpoints.servlets;

import java.io.IOException;
import java.util.Enumeration;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oidc.client.base.utils.ServletLogger;

@WebServlet("/GenericTest")
public class GenericTestServlet extends HttpServlet {

    private static final long serialVersionUID = -417476984908088827L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response);
    }

    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ServletOutputStream outputStream = response.getOutputStream();
        String shortName = ServletLogger.getShortName(this.getClass().getName());
        ServletLogger.printLine(outputStream, shortName + " method: " + request.getMethod());

        Enumeration<String> parmNames = request.getParameterNames();
        if (parmNames.hasMoreElements()) {
            while (parmNames.hasMoreElements()) {
                String key = parmNames.asIterator().next();
                ServletLogger.printLine(outputStream, shortName + " - parmKey: " + key + " parmValue: " + request.getParameter(key));
            }
        } else {
            ServletLogger.printLine(outputStream, shortName + " - No Parms were passed");
        }

    }

}
