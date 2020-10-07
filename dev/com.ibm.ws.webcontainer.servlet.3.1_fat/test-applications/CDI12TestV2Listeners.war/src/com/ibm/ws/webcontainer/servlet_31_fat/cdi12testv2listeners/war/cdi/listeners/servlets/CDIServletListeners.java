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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log.ApplicationLog;

@WebServlet("/CDIListeners")
public class CDIServletListeners extends HttpServlet {
    private static final long serialVersionUID = 1L;

    //

    private static final String LOG_CLASS_NAME = "CDIServletListeners";

    private static final Logger LOG = Logger.getLogger(CDIServletListeners.class.getName());

    private static void logEntry(String methodName) {
        LOG.info(LOG_CLASS_NAME + ": " + methodName + ": ENTRY");
    }

    private static void logExit(String methodName) {
        LOG.info(LOG_CLASS_NAME + ": " + methodName + ": EXIT");
    }

    @SuppressWarnings("unused")
    private static void logInfo(String methodName, String text) {
        LOG.info(LOG_CLASS_NAME + ": " + methodName + ": " + text);
    }

    @SuppressWarnings("unused")
    private static void logException(String methodName, Exception e) {
        LOG.info(LOG_CLASS_NAME + ": " + methodName + ": " + " Unexpected exception [ " + e + " ]");
        LOG.throwing(LOG_CLASS_NAME, methodName, e);
    }

    //

    public CDIServletListeners() {
        super();
    }

    public static final String OPERATION_PARAMETER_NAME = "operation";

    public static final String DISPLAY_LOG_PARAMETER_VALUE = "displayLog";

    public static final String ADD_ATTRIBUTES_PARAMETER_VALUE = "addAttributes";

    public static final String CHANGE_SESSION_ID_PARAMETER_VALUE = "changeSessionId";

    public static final String ATTRIBUTE_NAME_PARAMETER_NAME = "attributeName";
    public static final String ATTRIBUTE_VALUE_PARAMETER_NAME = "parameterValue";

    public static final String COMMENT_PARAMETER_NAME = "comment";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        String methodName = "doGet";
        logEntry(methodName);

        PrintWriter responseWriter = servletResponse.getWriter();

        responseWriter.println("Hello! CDIListenersServlet");

        showRequestValues(servletRequest, servletResponse, responseWriter);

        changeSessionId(servletRequest, servletResponse, responseWriter);

        maybeAddAttributes(servletRequest, servletResponse, responseWriter);

        mabeEmitApplicationLog(servletRequest, servletResponse, responseWriter);

        logExit(methodName);
    }

    //

    // Adding attributes will trigger the three attribute listeners.  We use
    // that to generate stateful information from the attribute listeners.

    private void maybeAddAttributes(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PrintWriter responseWriter) {
        String operationParameterValue = servletRequest.getParameter(OPERATION_PARAMETER_NAME);
        if ((operationParameterValue == null) || !operationParameterValue.equals(ADD_ATTRIBUTES_PARAMETER_VALUE)) {
            return;
        }

        String attributeName = servletRequest.getParameter(ATTRIBUTE_NAME_PARAMETER_NAME);
        String attributeValue = servletRequest.getParameter(ATTRIBUTE_VALUE_PARAMETER_NAME);

        // @formatter:off
        applicationLog(
            "Parameter [ " + OPERATION_PARAMETER_NAME + " ] [ " + operationParameterValue + " ]:" +
            " Add attribute [ " + attributeName + " ] [ " + attributeValue + " ]");
        // @formatter:on

        servletRequest.setAttribute(attributeName + "_R", attributeValue);
        servletRequest.getSession().setAttribute(attributeName + "_S", attributeValue);
        servletRequest.getServletContext().setAttribute(attributeName + "_C", attributeValue);
    }

    //

    private void showRequestValues(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PrintWriter responseWriter) {
        Enumeration<String> headerNames = servletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = servletRequest.getHeader(headerName);
            responseWriter.println("Header [ " + headerName + " ] [ " + headerValue + " ]");
        }

        Enumeration<String> parameterNames = servletRequest.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            String parameterValue = servletRequest.getParameter(parameterName);
            responseWriter.println("Parameter [ " + parameterName + " ] [ " + parameterValue + " ]");
        }

        String comment = servletRequest.getParameter(COMMENT_PARAMETER_NAME);
        if (comment != null) {
            try {
                comment = URLDecoder.decode(comment, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Ignore
            }

            responseWriter.println("Comment [ " + comment + " ]");
        }
    }

    private void mabeEmitApplicationLog(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PrintWriter responseWriter) throws IOException, ServletException {
        String operationParameterValue = servletRequest.getParameter(OPERATION_PARAMETER_NAME);
        if ((operationParameterValue == null) || !operationParameterValue.equals(DISPLAY_LOG_PARAMETER_VALUE)) {
            return;
        }

        applicationLog("Parameter [ " + OPERATION_PARAMETER_NAME + " ] [ " + operationParameterValue + " ]: Show application log");

        emitApplicationLog(servletRequest, servletResponse, responseWriter); // throws IOException, ServletException
    }

    private void changeSessionId(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PrintWriter responseWriter) {
        String operationParameterValue = servletRequest.getParameter(OPERATION_PARAMETER_NAME);
        if ((operationParameterValue == null) || !operationParameterValue.equals(CHANGE_SESSION_ID_PARAMETER_VALUE)) {
            return;
        }

        servletRequest.changeSessionId();
    }

    //

    @Inject
    ApplicationLog applicationLog;

    private void applicationLog(String line) {
        applicationLog.addLine(line);
    }

    private void emitApplicationLog(ServletRequest servletRequest, ServletResponse servletResponse,
                                    PrintWriter responseWriter) throws IOException, ServletException {

        String methodName = "emitApplicationLog";
        logEntry(methodName);

        responseWriter.println("Application Log");
        responseWriter.println("========================");
        for (String line : applicationLog.getLines(ApplicationLog.DO_CLEAR_LINES)) {
            responseWriter.println(line);
        }
        responseWriter.println("========================");

        logExit(methodName);
    }
}
