/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.logs;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class LogstashServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    String loggerName = "com.ibm.logs.LogstashServlet";
    String logMessage = "Test Logstash Message";
    String isFFDC = "false";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        isFFDC = req.getParameter("isFFDC");

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);

        String id = req.getParameter("id");
        if (id == null) {
            id = "";
        } else {
            id = " " + id;
        }

        logger.logp(java.util.logging.Level.INFO, loggerName, "Method.Info", logMessage + id);

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

        String gc = req.getParameter("gc");
        if ((gc != null) && (gc.equalsIgnoreCase("true"))) {
            performGC();
        }

        res.setContentType("text/html");
        out = res.getWriter();
        out.println("<HTML><HEAD><TITLE>Just a Servlet</TITLE></HEAD><BODY BGCOLOR=\"#FFFFEE\">");

    }

    private void performGC() {
        String[] sa = new String[1000];
        for (int i = 0; i < 1000; i++) {
            sa[i] = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
        }
        sa = null;
        System.gc();
    }
}
