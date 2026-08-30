/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class ForbiddenServlet extends HttpServlet {

    public static final String ERROR_MESSAGE = "Error: ";
    public static final String SUCCESS_MESSAGE = "Success";

    /**
     * A simple servlet that when it received a request it simply outputs the message
     * as defined by the static field.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("GET: " + request.getRequestURI());
        PrintWriter writer = response.getWriter();
        String testName = request.getParameter("testName");
        String result = ERROR_MESSAGE;

        try {
            if ("ping".equals(testName)) {
                result = "ACK";
            } else if ("testForbidden".equals(testName)) {
                result = testForbidden();
            } else if ("testNothingForbidden".equals(testName)) {
                result = testNothingForbidden();
            }
        } catch (Throwable e) {
            result += e.toString();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            result += baos.toString();
        } finally {
            writer.println(result);
            writer.flush();
            writer.close();
        }
    }

    /**
     * Test to verify that the forbidden class cannot load
     *
     */
    private String testForbidden() {

        try {
            Class.forName("org.apache.logging.log4j.core.lookup.GoodClass");
        } catch (ClassNotFoundException e) {
            return ERROR_MESSAGE;
        }
        try {
            Class.forName("org.apache.logging.log4j.core.lookup.JndiLookup");
            return ERROR_MESSAGE;
        } catch (ClassNotFoundException e) {
            return SUCCESS_MESSAGE;
        }
    }

    /**
     * Test to verify that the forbidden class can load when overridden
     *
     */
    private String testNothingForbidden() {

        try {
            Class.forName("org.apache.logging.log4j.core.lookup.GoodClass");
        } catch (ClassNotFoundException e) {
            return ERROR_MESSAGE;
        }
        try {
            Class.forName("org.apache.logging.log4j.core.lookup.JndiLookup");
            return SUCCESS_MESSAGE;
        } catch (ClassNotFoundException e) {
            return ERROR_MESSAGE;
        }
    }

}
