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
package oidc.client.base.utils;

import java.io.IOException;
import java.util.Enumeration;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;

public class RequestLogger {

    protected String caller = null;
    HttpServletRequest req;

    public RequestLogger(HttpServletRequest request, String callingClass) {

        req = request;
        caller = callingClass;

    }

    public void printRequest(ServletOutputStream ps) throws IOException {

        ServletLogger.printSeparator(ps);

        printRequestHeader(ps);

        printRequestParms(ps);

        ServletLogger.printSeparator(ps);
    }

    public void printRequestHeader(ServletOutputStream ps) throws IOException {

        Enumeration<String> headerEntryNames = req.getHeaderNames();
        while (headerEntryNames.hasMoreElements()) {
            String headerName = headerEntryNames.nextElement();
            String value = req.getHeader(headerName);
            ServletLogger.printLine(ps, caller, ServletMessageConstants.HEADER + ServletMessageConstants.NAME + headerName + " " + ServletMessageConstants.VALUE + value);
        }

    }

    public void printRequestParms(ServletOutputStream ps) throws IOException {

        Enumeration<String> parmNames = req.getParameterNames();
        while (parmNames.hasMoreElements()) {
            String parmName = parmNames.nextElement();
            String value = req.getParameter(parmName);
            ServletLogger.printLine(ps, caller, ServletMessageConstants.PARMS + ServletMessageConstants.NAME + parmName + " " + ServletMessageConstants.VALUE + value);
        }

    }
}