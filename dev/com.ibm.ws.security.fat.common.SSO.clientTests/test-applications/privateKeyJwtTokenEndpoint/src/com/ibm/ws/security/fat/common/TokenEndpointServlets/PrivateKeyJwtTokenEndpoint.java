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

package com.ibm.ws.security.fat.common.TokenEndpointServlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Social login-specific base servlet to help print values pertaining to social login
 */
public class PrivateKeyJwtTokenEndpoint extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected String sigAlgForPrivateKey = null;

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleSaveConfigTokenRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleProcessTokenRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleProcessTokenRequest(req, resp);
    }

    protected void handleSaveConfigTokenRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter writer = resp.getWriter();
        writer.println("In handleSaveConfigTokenRequest");

        printContent(writer, req);

        sigAlgForPrivateKey = req.getParameter("sigAlgForPrivateKey");

        writer.flush();
        writer.close();

    }

    protected void handleProcessTokenRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter writer = resp.getWriter();
        writer.println("In handleProcessTokenRequest");

        printContent(writer, req);

        writer.flush();
        writer.close();

    }

    protected void printContent(PrintWriter writer, HttpServletRequest req) throws IOException {

        testWriter(writer, "request: " + req.getQueryString());
        printHeader(writer, req);
        printParms(writer, req);
    }

    protected void printHeader(PrintWriter writer, HttpServletRequest req) throws IOException {

        testWriter(writer, "Headers:");

        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String value = req.getHeader(headerName);
            testWriter(writer, "header: " + headerName + ":" + value);
        }

    }

    protected void printParms(PrintWriter writer, HttpServletRequest req) throws IOException {

        testWriter(writer, "Parameters:");

        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames != null && parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            String[] values = req.getParameterValues(parameterName);
            if (values != null && values.length > 0) {
                for (int iI = 0; iI < values.length; iI++) {
                    testWriter(writer, "parameter: " + parameterName + ":" + values[iI]);
                }
            } else {
                testWriter(writer, "parameter: " + parameterName + ":null or empty");
            }
        }

    }

    protected void testWriter(PrintWriter writer, String msg) throws IOException {

        System.out.println(msg);
        writer.println(msg);
    }
}
