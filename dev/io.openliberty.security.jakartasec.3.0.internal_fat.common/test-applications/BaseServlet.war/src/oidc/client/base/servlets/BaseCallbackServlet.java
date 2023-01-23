/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package oidc.client.base.servlets;

import java.io.IOException;
import java.util.Optional;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.inject.Inject;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oidc.client.base.utils.OpenIdContextLogger;
import oidc.client.base.utils.RequestLogger;
import oidc.client.base.utils.ServletLogger;
import oidc.client.base.utils.WSSubjectLogger;

public class BaseCallbackServlet extends HttpServlet {

    private static final long serialVersionUID = -417476984908088827L;

    @Inject
    private OpenIdContext context;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWorker(request, response, "got here callback");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWorker(request, response, "got here post callback");
    }

    private void doWorker(HttpServletRequest request, HttpServletResponse response, String message) throws ServletException, IOException {
        ServletOutputStream ps = response.getOutputStream();

        ServletLogger.printLine(ps, "Class: " + this.getClass().getName());
        ServletLogger.printLine(ps, "Super Class: " + this.getClass().getSuperclass().getName());

        ServletLogger.printLine(ps, message);

        RequestLogger requestLogger = new RequestLogger(request, ServletMessageConstants.CALLBACK + ServletMessageConstants.REQUEST);
        requestLogger.printRequest(ps);

        OpenIdContextLogger contextLogger = new OpenIdContextLogger(request, response, ServletMessageConstants.CALLBACK + ServletMessageConstants.OPENID_CONTEXT, context);
        contextLogger.logContext(ps);

        WSSubjectLogger subjectLogger = new WSSubjectLogger(request, ServletMessageConstants.CALLBACK + ServletMessageConstants.WSSUBJECT);
        subjectLogger.printProgrammaticApiValues(ps);

        if (context != null) {
            Optional<String> originalRequest = context.getStoredValue(request, response, OpenIdConstant.ORIGINAL_REQUEST);
            if (originalRequest.isPresent()) {
                String originalRequestString = originalRequest.get();
                response.sendRedirect(originalRequestString);
            }
        }

    }

}