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
package io.openliberty.microprofile.telemetry.logging.internal.container.fat.MpTelemetryLogApp;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/LogURL")
public class LogServlet extends HttpServlet {
    String loggerName = "io.openliberty.microprofile.telemetry.logging.internal.container.fat.MpTelemetryLogApp.MpTelemetryServlet";

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
        System.out.println("{}");
        res.getWriter().print(new Date());
    }
}