
/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@WebServlet("")
@WebServlet("/LogURL")
public class LogServlet extends HttpServlet {
    String loggerName = "com.ibm.logs.LogstashServlet";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Logger logger = java.util.logging.Logger.getLogger(loggerName);

        logger.entering("LogServlet", "doGet");
        logger.severe("severe message");
        logger.warning("warning message");
        logger.info("info message");
        System.out.println("System.out.println");
        System.err.println("System.err.println");
        logger.config("config trace");
        logger.fine("fine trace");
        logger.finer("finer trace");
        logger.finest("finest trace");
        logger.exiting("LogServlet", "doGet");
        System.out.println("{\"key\":\"value\"}");
        System.err.println("{\"key\":\"value\",\"loglevel\":\"System.err\"}");
        res.getWriter().print(new Date());
    }
}