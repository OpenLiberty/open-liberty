
/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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

        res.getWriter().print(new Date());
    }
}