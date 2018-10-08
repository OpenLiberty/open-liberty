/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import java.io.PrintWriter;

import javax.el.ELException;
import javax.el.ELProcessor;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.el30.fat.beans.EL30ReservedWordsTestBean;

/**
 * This servlet tests all the EL 3.0 reserved words depending on the parameter that it is being passed.
 */
@WebServlet("/EL30ReservedWordsServlet")
public class EL30ReservedWordsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public EL30ReservedWordsServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ELProcessor elp = new ELProcessor();
        PrintWriter pw = response.getWriter();

        // Create an instance of the ReservedWordsTestBean
        EL30ReservedWordsTestBean test = new EL30ReservedWordsTestBean();
        test.setCat("Testing \"cat\" non-reserved word. Test Successful");
        test.setT("Testing \"T\" non-reserved word. Test Successful");

        // Define the bean within the ELProcessor object
        elp.defineBean("test", test);

        if (request.getParameter("testReservedWord").equals("and")) {
            String expression = "test.and";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("or")) {
            String expression = "test.or";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("not")) {
            String expression = "test.not";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("eq")) {
            String expression = "test.eq";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("ne")) {
            String expression = "test.ne";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("lt")) {
            String expression = "test.lt";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("gt")) {
            String expression = "test.gt";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("le")) {
            String expression = "test.le";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("ge")) {
            String expression = "test.ge";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("true")) {
            String expression = "test.true";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("false")) {
            String expression = "test.false";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("null")) {
            String expression = "test.null";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("instanceof")) {
            String expression = "test.instanceof";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("empty")) {
            String expression = "test.empty";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("div")) {
            String expression = "test.div";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("mod")) {
            String expression = "test.mod";
            evaluateReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("cat")) {
            String expression = "test.cat";
            evaluateNonReservedWord(elp, pw, expression);
        } else if (request.getParameter("testReservedWord").equals("T")) {
            String expression = "test.t";
            evaluateNonReservedWord(elp, pw, expression);
        } else {
            pw.println("Invalid parameter");
        }
    }

    /**
     * Helper method to evaluate EL3.0 reserved words and print the response
     *
     * @param elp The ELProcessor
     * @param pw PrintWriter
     * @param expression Expression to be evaluated.
     */
    private void evaluateReservedWord(ELProcessor elp, PrintWriter pw, String expression) {
        try {
            pw.println(elp.eval(expression));
        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            if (e instanceof ELException) {
                pw.println("Test Successful. Correct exception was thrown: " + e.toString());
            } else {
                pw.println("Incorrect exception was thrown: " + e.toString());
            }
        }
    }

    /**
     * Helper method to evaluate the non reserved words and print the response
     *
     * @param elp The ELProcessor
     * @param pw PrintWriter
     * @param expression Expression to be evaluated.
     */
    private void evaluateNonReservedWord(ELProcessor elp, PrintWriter pw, String expression) {
        try {
            pw.println(elp.eval(expression));
        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            pw.println("Test Failed. An exception was thrown: " + e.toString());
        }
    }
}