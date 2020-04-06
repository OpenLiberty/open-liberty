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
package cdi.interceptors.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cdi.beans.v2.CDICaseInstantiableType;
import cdi.beans.v2.log.ApplicationLog;

/**
 * Servlet used to test interceptors.
 */
@WebServlet(urlPatterns = { "/CDICounter" })
public class CDIServletCounter extends HttpServlet {
    //
    private static final long serialVersionUID = 1L;

    // Test utility ...

    /**
     * Answer the subject of this test. This is included in
     * various output and is used to verify the output.
     * 
     * @return The test subject. This implementation always answers {@link CDICaseInstantiableType#Servlet}.
     */
    public CDICaseInstantiableType getInstantiableType() {
        return CDICaseInstantiableType.Servlet;
    }

    /**
     * Prepend the type tag to response text.
     * 
     * @param responseText Input responst text.
     * 
     * @return The responst text with the type tag prepended.
     */
    private String prependType(String responseText) {
        return (":" + getInstantiableType().getTag() + ":" + responseText + ":");
    }

    // Servlet API ...

    public static final String OPERATION_PARAMETER_NAME = "operation";
    public static final String OPERATION_INCREMENT = "increment";
    public static final String OPERATION_DECREMENT = "decrement";
    public static final String OPERATION_GET_COUNT = "getCount";
    public static final String OPERATION_DISPLAY_LOG = "displayLog";

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
                    throws ServletException, IOException {

        PrintWriter responseWriter = servletResponse.getWriter();

        responseWriter.println(prependType("Entry"));

        String operation = servletRequest.getParameter(OPERATION_PARAMETER_NAME);
        if (operation == null) {
            responseWriter.println("Error: No operation parameter [ " + OPERATION_PARAMETER_NAME + " ]");
            servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (operation.equals(OPERATION_DECREMENT)) {
            int finalCount = servletCount.decrement();
            responseWriter.println("Decrement [ " + Integer.toString(finalCount) + " ]");

        } else if (operation.equals(OPERATION_INCREMENT)) {
            int finalCount = servletCount.increment();
            responseWriter.println("Increment [ " + Integer.toString(finalCount) + " ]");

        } else if (operation.equals(OPERATION_GET_COUNT)) {
            int count = servletCount.getCount();
            responseWriter.println("Count [ " + Integer.toString(count) + " ]");

        } else if (operation.equals(OPERATION_DISPLAY_LOG)) {
            displayApplicationLog(responseWriter, servletRequest, servletResponse);

        } else {
            responseWriter.println("Error: Unknown operation parameter [ " + OPERATION_PARAMETER_NAME + " ] [ " + operation + " ]");
            servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        responseWriter.println(prependType("Exit"));
    }

    //

    @Inject
    ServletCounter servletCount;

    //

    @Inject
    ApplicationLog applicationLog;

    private void displayApplicationLog(PrintWriter responseWriter, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        responseWriter.println("Application Log [ " + applicationLog + " ]");
        responseWriter.println("========================");
        for (String line : applicationLog.getLines(ApplicationLog.DO_CLEAR_LINES)) {
            responseWriter.println(line);
        }
        responseWriter.println("========================");
    }
}
