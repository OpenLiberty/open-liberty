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
package oidc.client.base.utils;

import java.io.IOException;
import java.util.Enumeration;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
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

        printRequestCookies(ps);

        printRequestParms(ps);

        printRequestValues(ps);

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

    public void printRequestCookies(ServletOutputStream ps) throws IOException {

        Cookie[] cookies = req.getCookies();
        if (cookies != null && cookies.length != 0) {
            for (Cookie c : cookies) {
                String cookieName = c.getName();
                String cookieValue = c.getValue();
                ServletLogger.printLine(ps, caller, ServletMessageConstants.COOKIE + ServletMessageConstants.NAME + cookieName + " " + ServletMessageConstants.VALUE + cookieValue
                                                    + " " + "Domain: " + c.getDomain());
            }
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

    public void printRequestValues(ServletOutputStream ps) throws IOException {

        ServletLogger.printLine(ps, caller, "User Principal: " + req.getUserPrincipal());
        ServletLogger.printLine(ps, caller, "Remote User: " + req.getRemoteUser());
        ServletLogger.printLine(ps, caller, "Request URI: " + req.getRequestURI());
    }

}
