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

import com.ibm.websphere.logging.hpel.LogRecordContext;

@WebServlet("/ExtURL")
public class ExtensionServlet extends HttpServlet {

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String loggerName = "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet";
        String logMessage = "Test Extension Message";

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);

        String key = request.getParameter("key");
        String value = request.getParameter("value");
        String msg = request.getParameter("msg");
        if (msg != null) {
            logMessage = msg;
        }

        //Populating LogRecord with extensions

        if (key == null) {
            LogRecordContext.addExtension("correctBooleanExtension_bool", "true");
            LogRecordContext.addExtension("correctBooleanExtension2_bool", "false");
            LogRecordContext.addExtension("correctIntExtension_int", "12345");
            LogRecordContext.addExtension("correctIntExtension2_int", "-12345");
            LogRecordContext.addExtension("correctStringExtension", "Testing string 1234");
            LogRecordContext.addExtension("correctFloatExtension_float", "100.123");
            LogRecordContext.addExtension("correctFloatExtension2_float", "-100.123");

            LogRecordContext.addExtension("wrongExtensionBoolean_bool", "12345");
            LogRecordContext.addExtension("wrongExtensionBoolean2_bool", "wrongBool");
            LogRecordContext.addExtension("wrongExtensionInt_int", "Testing");
            LogRecordContext.addExtension("wrongExtensionInt2_int", "false");
            LogRecordContext.addExtension("wrongExtensionInt3_int", "123.123");
            LogRecordContext.addExtension("wrongExtensionFloat_float", "Testing string");
            LogRecordContext.addExtension("wrongExtensionFloat2_float", "false");
        } else {
            LogRecordContext.addExtension(key, value);
        }

        logger.logp(java.util.logging.Level.INFO, loggerName, "Method.Info", logMessage);
        logger.logp(java.util.logging.Level.FINE, loggerName, "Method.Info", logMessage);

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("Simple extension servlet");
        if (key != null) {
            out.println("key=" + key);
        }
        if (value != null) {
            out.println("value=" + value);
        }
        out.println("</body></html>");
    }
}