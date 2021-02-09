/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/RequestParameterTest", asyncSupported = true)
public class RequestParameterTest extends HttpServlet {

    /**  */

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RequestParameterTest.class.getName());

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        String variation = req.getParameter("NumberOfOtherParameters");

        LOG.info("RequestParameterTest : expected number of parameters = " + variation);
        int numParms = Integer.parseInt(variation);

        String nextParamName = "FirstParameter";
        String paramValue = "";
        boolean longParamNext = false;
        boolean nullParamNext = false;
        boolean noValueNext = false;
        int longParamLength = 0;

        boolean result = true;
        for (int i = 0; i < numParms; i++) {
            paramValue = req.getParameter(nextParamName);
            if (paramValue == null) {
                LOG.info("RequestParameterTest : " + nextParamName + " = " + paramValue);
                result = false;
                break;
            } else if (paramValue.equals("")) {
                LOG.info("RequestParameterTest : " + nextParamName + " = " + paramValue);
                if (nullParamNext) {
                    nextParamName = "FirstAfterParamNoEqual";
                    nullParamNext = false;
                } else if (noValueNext) {
                    nextParamName = "FirstAfterParamNoValue";
                    noValueNext = false;
                } else {
                    result = false;
                    break;
                }
            } else if (longParamNext) {
                LOG.info("RequestParameterTest : " + nextParamName + ", length = " + paramValue.length());
                longParamLength = paramValue.length();
                nextParamName = "FirstParameterAfterReallyReallLongParamaterValue";
                longParamNext = false;
            } else {
                LOG.info("RequestParameterTest : " + nextParamName + " = " + paramValue);
                if (paramValue.equals("ReallyReallyLongParamaterValue")) {
                    longParamNext = true;
                } else if (paramValue.equals("ParamNoEqual")) {
                    nullParamNext = true;
                } else if (paramValue.equals("ParamNoValue")) {
                    noValueNext = true;
                }
                nextParamName = paramValue;
            }
        }

        PrintWriter pw = res.getWriter();
        if (result) {
            pw.println("Last parameter value found was " + (paramValue.equals("") ? "EmptyString" : nextParamName));
            if (longParamLength != 0) {
                pw.println("Long parameter length was " + longParamLength);
            } else {
                pw.println("Last parameter length was " + paramValue.length());
            }
        } else {
            pw.println("FAIL. No value found for parameter " + nextParamName);
        }

    }
}
