/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2dynamic.war.cdi.dynamic;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * CDI Testing: CDI Servlet (Dynamic case)
 *
 * CDIServlet with static registration removed.
 */
@WebServlet(urlPatterns = { "/CDIVerifier" })
public class CDIDynamicVerifier extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Servlet API

    public static final String PARAMETER_NAME_OPERATION = "operation";
    public static final String OPERATION_NAME_VERIFY = "verify";

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        doPost(servletRequest, servletResponse); // throws ServletException, IOException
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        String responsePrefix = "CDIDynamicVerifier.doPost";

        PrintWriter responseWriter = servletResponse.getWriter();

        responseWriter.println(responsePrefix + ":" + "Entry");

        String operationName = servletRequest.getParameter(PARAMETER_NAME_OPERATION);
        responseWriter.println(responsePrefix + ":" + "Operation [ " + operationName + "]");

        if (operationName == null) {
            responseWriter.println(responsePrefix + ":" + "FAILED (No operation)");
        } else if (operationName.equals(OPERATION_NAME_VERIFY)) {
            CDIDynamicInitializer.verifyRegistration(servletRequest, servletResponse, responseWriter);
        } else {
            responseWriter.println(responsePrefix + ":" + "FAILED (Unknown operation)");
        }

        responseWriter.println(responsePrefix + ":" + "Exit");
    }

}
