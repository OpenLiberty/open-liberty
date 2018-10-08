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
 * Servlet for testing the new EL 3.0 operators
 *
 * Servlet prints "Test Failed!" when that an expression does not evaluate to the expected value.
 * If "Test Failed!" is not printed, all of the expressions here evaluated as expected.
 */
@WebServlet({ "/EL30OperatorsServlet" })
public class EL30OperatorsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        sos.println("Test the new EL 3.0 Operators");
        ELProcessor elp = new ELProcessor();
        int n = 1;

        String test = "Test" + n++ + " EL 3.0 String Concatenation Operator (+=) with literals (Expected: xy): ";
        String expression = "\"x\" += \"y\"";
        String expected = "xy";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 String Concatenation Operator (+=) with variables (Expected: 12): ";
        expression = "testString1 = \"1\"; testString2 = \"2\"; testString1 += testString2";
        expected = "12";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 String Concatenation Operator with literals and multiple concatenations (Expected: xyz): ";
        // "x" += "y" += "z"
        expression = "\"x\" += \"y\" += \"z\"";
        expected = "xyz";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 String Concatenation Operator with literals and single quotes  (Expected: xyz): ";
        // 'x' += 'y' += 'z'
        expression = "'x' += 'y' += 'z'";
        expected = "xyz";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 String Concatenation Operator with literals and mixed quotes  (Expected: xyz): ";
        // "x" += 'y' += "z"
        expression = "\"x\" += 'y' += \"z\"";
        expected = "xyz";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 String Concatenation Operator with literals and escape characters  (Expected: \"x\"yz): ";
        // "\"x\"" += 'y' += "z"
        expression = "\"\\\"x\\\"\" += 'y' += \"z\"";
        expected = "\"x\"yz";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 String Concatenation Operator with literals and escape characters  (Expected: 'x'yz): ";
        // "\'x\'" += 'y' += "z"
        expression = "\"\\'x\\'\" += 'y' += \"z\"";
        expected = "'x'yz";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 Assignment Operator (=) (Expected:3): ";
        expression = "x=3";
        expected = "3";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 Assignment Operator (=) (Expected:8): ";
        expression = "y=x+5";
        expected = "8";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 Semi-colon Operator (Expected:8): ";
        expression = "x = 5; y = 3; z = x + y";
        expected = "8";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 Assignment Operator (Expected:3): ";
        expression = "x=(x->x+1)(2)";
        expected = "3";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ": EL 3.0 Assignment Operator (Expected:javax.el.PropertyNotWritableException): ";
        expression = "null=(x->x+1)(2)";
        expected = "javax.el.PropertyNotWritableException: Illegal Syntax for Set Operation";
        testExpression(expression, expected, test, sos, elp);
    }

    /**
     * Evaluates an EL expression; prints "Test Failed!", along with expression info, if an expression does not
     * evaluate to the expected value.
     */
    private static void testExpression(String expression, String expected, String test, ServletOutputStream sos, ELProcessor elp) throws IOException {
        String result = null;
        try {
            result = elp.eval(expression).toString();
        } catch (Exception e) {
            result = e.toString();
        }
        if (!result.equals(expected)) {
            sos.println(test + result + " - Test Failed!");
        } else {
            // Don't bother to print out the successful tests
            //sos.println(test + result + " - Test Passed!");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}