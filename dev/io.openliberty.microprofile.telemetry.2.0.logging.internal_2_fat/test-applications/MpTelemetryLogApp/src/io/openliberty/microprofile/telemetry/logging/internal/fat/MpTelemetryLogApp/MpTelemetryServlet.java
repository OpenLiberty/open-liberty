/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class MpTelemetryServlet extends HttpServlet {

    String loggerName = "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet";
    String logMessage = "Test Logstash Message";
    String isFFDC = "false";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        isFFDC = req.getParameter("isFFDC");

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);

        String truncated = req.getParameter("truncated");
        String prefix = "";
        if (truncated != null) {
            prefix = truncated.equals("true") ? "T: " : "F: ";
        }

        for (int i = 0; i < 3; i++) {
            logger.logp(java.util.logging.Level.INFO, loggerName, "Method.Info", prefix + logMessage);
            logger.logp(java.util.logging.Level.FINE, loggerName, "Method.Info", prefix + logMessage);
        }

        PrintWriter out;

        if ((isFFDC != null) && isFFDC.equalsIgnoreCase("true")) {
            String myString = null;
            myString.toString();
        }
        String secondFFDC = req.getParameter("secondFFDC");

        if ((secondFFDC != null) && (secondFFDC.equalsIgnoreCase("true"))) {
            int i = 10 / 0;
        }

        String thirdFFDC = req.getParameter("thirdFFDC");

        if ((thirdFFDC != null) && (thirdFFDC.equalsIgnoreCase("true"))) {

            int[] a = { 0, 1, 2, 3 };
            a[6] = 10;
        }

        res.setContentType("text/html");

        out = res.getWriter();

        out.println("<HTML><HEAD><TITLE>Just a Servlet</TITLE></HEAD><BODY BGCOLOR=\"#FFFFEE\">");

    }
}
