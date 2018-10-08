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
package com.ibm.ws.el30.fat.servlets;

import java.io.IOException;

import javax.el.ELProcessor;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for testing the new EL 3.0 static field/method functionality
 */
@WebServlet("/EL30StaticFieldsAndMethodsServlet")
public class EL30StaticFieldsAndMethodsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public static final String out = "now";

    public EL30StaticFieldsAndMethodsServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ServletOutputStream sos = response.getOutputStream();
        ELProcessor elp = new ELProcessor();
        String expression = request.getParameter("testExpression");
        String expected = request.getParameter("expectedResult");

        // We'll use the ImportHandler if this test needs to load one of these classes
        if (expression.startsWith("EL30StaticFieldsAndMethodsBean."))
            elp.getELManager().importClass("beans.EL30StaticFieldsAndMethodsBean");
        if (expression.startsWith("EL30StaticFieldsAndMethodsEnum"))
            elp.getELManager().importClass("beans.EL30StaticFieldsAndMethodsEnum");

        testExpression(expression, expected, sos, elp);
    }

    /**
     * Evaluates an EL expression; simply prints "Test successful" if the expression evaluates as expected.
     */
    private static void testExpression(String expression, String expected, ServletOutputStream sos, ELProcessor elp) throws IOException {
        String result = null;
        try {
            result = elp.eval(expression).toString();
        } catch (Exception e) {
            result = e.toString();
        }
        String s = expression + " | " + expected + " =? "
                   + result;
        if (result.startsWith(expected) || (expected.startsWith("positive") && !expected.contains("Exception"))) {
            sos.println("Test successful.");
        } else {
            sos.println("Test failed.  Expression | expected =? output :  " + s);
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
