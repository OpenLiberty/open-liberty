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
package oidc.client.base.servlets;

import java.io.IOException;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oidc.client.base.utils.OpenIdContextLogger;
import oidc.client.base.utils.RequestLogger;
import oidc.client.base.utils.ServletLogger;
import oidc.client.base.utils.WSSubjectLogger;

@WebServlet("/BaseServlet")
public class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private OpenIdContext context;

    public BaseServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ServletOutputStream outputStream = response.getOutputStream();

        ServletLogger.printLine(outputStream, "Class: " + this.getClass().getName());
        ServletLogger.printLine(outputStream, "got here");

        RequestLogger requestLogger = new RequestLogger(request, ServletMessageConstants.SERVLET + ServletMessageConstants.REQUEST);
        requestLogger.printRequest(outputStream);

        OpenIdContextLogger contextLogger = new OpenIdContextLogger(request, response, ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT, context);
        contextLogger.logContext(outputStream);

        WSSubjectLogger subjectLogger = new WSSubjectLogger(request, ServletMessageConstants.SERVLET + ServletMessageConstants.WSSUBJECT);
        subjectLogger.printProgrammaticApiValues(outputStream);

        recordHelloWorld(outputStream);

    }

    protected void recordHelloWorld(ServletOutputStream outputStream) throws IOException {

        ServletLogger.printLine(outputStream, "Hello world from BaseServlet");
        ServletLogger.printLine(outputStream, this.getClass().getSuperclass().getName());
        ServletLogger.printLine(outputStream, this.getClass().getName());

    }
}
