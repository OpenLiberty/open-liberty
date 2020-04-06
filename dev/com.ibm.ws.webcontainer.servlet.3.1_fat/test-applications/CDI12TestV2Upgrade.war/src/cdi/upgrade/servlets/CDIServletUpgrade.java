/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.upgrade.servlets;

import java.io.IOException;
import java.io.PrintWriter;
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

import cdi.beans.v2.log.ApplicationLog;
import cdi.upgrade.handlers.CDITestHttpUpgradeHandler;

/**
 * CDI Test upgrade handler servlet.
 */
@WebServlet(urlPatterns = "/CDIUpgrade")
public class CDIServletUpgrade extends HttpServlet {
    private static final long serialVersionUID = 1L;

    //

    private static final String LOG_CLASS_NAME = "CDIServletUpgrade";

    private static final Logger LOG = Logger.getLogger(CDIServletUpgrade.class.getName());

    private static void logEntry(String methodName) {
        LOG.info(LOG_CLASS_NAME + ": " + methodName + ": ENTRY");
    }

    private static void logExit(String methodName) {
        LOG.info(LOG_CLASS_NAME + ": " + methodName + ": EXIT");
    }

    private static void logInfo(String methodName, String text) {
        LOG.info(LOG_CLASS_NAME + ": " + methodName + ": " + text);
    }

    @SuppressWarnings("unused")
    private static void logException(String methodName, Exception e) {
        LOG.info(LOG_CLASS_NAME + ": " + methodName + ": " + " Unexpected exception [ " + e + " ]");
        LOG.throwing(LOG_CLASS_NAME, methodName, e);
    }

    //

    public static final String HEADER_FIELD_UPGRADE = "Upgrade";
    public static final String PROTOCOL_NAME_TEST_UPGRADE = "TestUpgrade";

    public static final String HEADER_FIELD_CONNECTION = "Connection";
    public static final String CONNECTION_UPGRADE_VALUE = "Upgrade";

    public static final String RESPONSE_NO_UPGRADE = "NoUpgrade";

    public static final String PARAMETER_NAME_SHOW_LOG = "ShowLog";
    public static final String APPLICATION_LOG_VALUE = "Application";

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
                    throws IOException, ServletException {
        String methodName = "doPost";
        logEntry(methodName);

        PrintWriter responseWriter = servletResponse.getWriter();

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

        String logParameterValue = servletRequest.getParameter(PARAMETER_NAME_SHOW_LOG);
        if ((logParameterValue != null) && logParameterValue.equals(APPLICATION_LOG_VALUE)) {
            applicationLog.addLine("Parameter [ " + PARAMETER_NAME_SHOW_LOG + " ] [ " + logParameterValue + " ]: Show application log");
            performShowLog(servletRequest, servletResponse, responseWriter);

        } else {
            String upgradeHeaderValue = servletRequest.getHeader(HEADER_FIELD_UPGRADE);
            logInfo(methodName, "Upgrade: " + upgradeHeaderValue);

            if ((upgradeHeaderValue == null) || !upgradeHeaderValue.equals(PROTOCOL_NAME_TEST_UPGRADE)) {
                applicationLog.addLine("Header [ " + HEADER_FIELD_UPGRADE + " ] [ " + upgradeHeaderValue + " ]: No upgrade");
                performNoUpgrade(servletRequest, servletResponse, responseWriter);
            } else {
                applicationLog.addLine("Header [ " + HEADER_FIELD_UPGRADE + " ] [ " + upgradeHeaderValue + " ]: Upgrade");
                performUpgrade(servletRequest, servletResponse, responseWriter);
            }
        }

        logExit(methodName);
    }

    private void performNoUpgrade(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                  PrintWriter responseWriter)
                    throws IOException, ServletException {

        String methodName = "performNoUpgrade";
        logEntry(methodName);

        responseWriter.println(RESPONSE_NO_UPGRADE);

        logExit(methodName);
    }

    private void performUpgrade(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                PrintWriter responseWriter)
                    throws IOException, ServletException {

        String methodName = "performUpgrade";
        logEntry(methodName);

        // The upgrade handler does NOT run in the scope of the request
        // which requested the upgrade.

        CDITestHttpUpgradeHandler handler = servletRequest.upgrade(CDITestHttpUpgradeHandler.class);
        logInfo(methodName, "Upgrade handler [ " + handler + " ]");

        servletResponse.setStatus(101); // 101 must be set to upgrade the protocol.
        servletResponse.setHeader(HEADER_FIELD_UPGRADE, PROTOCOL_NAME_TEST_UPGRADE);
        servletResponse.setHeader(HEADER_FIELD_CONNECTION, CONNECTION_UPGRADE_VALUE);

        logExit(methodName);
    }

    @Inject
    ApplicationLog applicationLog;

    private void performShowLog(ServletRequest servletRequest, ServletResponse servletResponse,
                                PrintWriter responseWriter)
                    throws IOException, ServletException {

        String methodName = "showLog";
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
