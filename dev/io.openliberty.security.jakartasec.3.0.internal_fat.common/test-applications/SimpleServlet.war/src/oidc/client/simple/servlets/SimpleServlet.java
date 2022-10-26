/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.simple.servlets;

import java.io.IOException;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oidc.client.base.utils.RequestLogger;
import oidc.client.base.utils.ServletLogger;
import oidc.client.base.utils.WSSubjectLogger;

@WebServlet("/SimpleServlet")
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public SimpleServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ServletOutputStream outputStream = response.getOutputStream();

        ServletLogger.printLine(outputStream, "Hello world from SimpleServlet");

        ServletLogger.printLine(outputStream, "got here servlet");

        RequestLogger requestLogger = new RequestLogger(request, ServletMessageConstants.SERVLET + ServletMessageConstants.REQUEST);
        requestLogger.printRequest(outputStream);

        WSSubjectLogger subjectLogger = new WSSubjectLogger(request, ServletMessageConstants.SERVLET + ServletMessageConstants.WSSUBJECT);
        subjectLogger.printProgrammaticApiValues(outputStream);

    }

}
