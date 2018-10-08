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
 * Servlet for testing the EL 2.2 operators, to make sure they function properly in the
 * EL 3.0 implementation.
 *
 * Servlet prints "Test Failed!" when that an expression does not evaluate to the expected value.
 * If "Test Failed!" is not printed, all of the expressions here evaluated as expected.
 */
@WebServlet({ "/EL22OperatorsServlet" })
public class EL22OperatorsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        sos.println("Test the existing EL 2.2 Operators");
        ELProcessor elp = new ELProcessor();
        int n = 1;

        String test = "Test" + n++ + " EL 2.2 Multiplication Operator (Expected:16): ";
        String expression = "8*2";
        String expected = "16";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Addition Operator (+) (Expected:5): ";
        expression = "2+3";
        expected = "5";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Subtraction Operator (-) (Expected:1): ";
        expression = "5-4";
        expected = "1";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Division Operator (/) (Expected:8.0): ";
        expression = "16/2";
        expected = "8.0";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Division Operator (div) (Expected:8.0): ";
        expression = "16 div 2";
        expected = "8.0";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Remainder Operator (%) (Expected:1): ";
        expression = "19%2";
        expected = "1";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Remainder Operator (mod) (Expected:1): ";
        expression = "19 mod 2";
        expected = "1";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (==) (Expected: true): ";
        expression = "3 == 3";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (eq) (Expected: false): ";
        expression = "3 eq 4";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (!=) (Expected: true): ";
        expression = "3 != 4";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (ne) (Expected: false): ";
        expression = "3 ne 3";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (<) (Expected: true): ";
        expression = "3 < 4";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (lt) (Expected: false): ";
        expression = "5 lt 4";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (>) (Expected: false): ";
        expression = "3 > 4";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (gt) (Expected: true): ";
        expression = "5 gt 4";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (<=) (Expected: true): ";
        expression = "3 <= 4";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (le) (Expected: false): ";
        expression = "5 le 4";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (le) (Expected: true): ";
        expression = "3 le 3";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (>=) (Expected: true): ";
        expression = "5 >= 4";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (ge) (Expected: false): ";
        expression = "3 ge 4";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Relational Operator (ge) (Expected: true): ";
        expression = "3 ge 3";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Logical Operator (&&) (Expected: false): ";
        expression = "true && false";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Logical Operator (&&) (Expected: true): ";
        expression = "true && true";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Logical Operator (and) (Expected: false): ";
        expression = "false and false";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Logical Operator (||) (Expected: true): ";
        expression = "true || false";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Logical Operator (||) (Expected: false): ";
        expression = "false || false";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Logical Operator (or) (Expected: true): ";
        expression = "true or false";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Logical Operator (!) (Expected: false): ";
        expression = "!true";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Logical Operator (!) (Expected: true): ";
        expression = "!false";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Logical Operator (not) (Expected: true): ";
        expression = "not false";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        /*
         * This throws a javax.el.PropertyNotFoundException here; however, in a JSP it evaluates
         * to true. Tomcat also behaves this way, so this behavior seems to be okay.
         */
//        test = "Test" + n++ + ":  EL 2.2 Empty Operator (empty) (Expected: true): ";
//        expression = "empty z";
//        expected = "true";
//        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Empty Operator (empty) (Expected: true): ";
        expression = "b=null; empty b";
        expected = "true";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Empty Operator (empty) (Expected: false): ";
        expression = "x=5; empty x";
        expected = "false";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Conditional Operator (A?B:C) (Expected: 2): ";
        expression = "1==1?2:3";
        expected = "2";
        testExpression(expression, expected, test, sos, elp);

        test = "Test" + n++ + ":  EL 2.2 Conditional Operator (A?B:C) (Expected: 3): ";
        expression = "1==2?2:3";
        expected = "3";
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
