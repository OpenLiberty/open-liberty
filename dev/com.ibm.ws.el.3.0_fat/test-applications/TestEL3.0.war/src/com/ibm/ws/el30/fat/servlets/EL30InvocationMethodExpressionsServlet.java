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

import javax.el.ELProcessor;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.el30.fat.beans.EL30InvocationMethodExpressionTestBean;

/**
 * This servlet test the invocation of Method Expressions
 */
@WebServlet("/EL30InvocationMethodExpressionsServlet")
public class EL30InvocationMethodExpressionsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public EL30InvocationMethodExpressionsServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ELProcessor elp = new ELProcessor();
        PrintWriter pw = response.getWriter();

        // Create an instance of the InvocationMethodExpressionTest bean
        EL30InvocationMethodExpressionTestBean parent = new EL30InvocationMethodExpressionTestBean();

        // Add the parent object to the ELProcessor
        elp.defineBean("parent", parent);

        // Lets first use Value Expressions to perform invocations

        // Set the parent object property using Value Expression
        setValueExpression(elp, pw, "parent.parentName", "John Smith Sr.");

        // Get the parent object property value using Value Expression
        Object obj = getValueExpression(elp, pw, "parent.parentName", java.lang.String.class);
        pw.println("Get Parent Name Using Value Expression (Expected: \"John Smith Sr.\"): " + obj);

        // Set the child object property using Value Expression
        setValueExpression(elp, pw, "parent.child.childName", "John Smith Jr.");

        // Get the child object property value using Value Expression
        obj = getValueExpression(elp, pw, "parent.child.childName", java.lang.String.class);
        pw.println("Get Child Name Using Value Expression (Expected: \"John Smith Jr.\"): " + obj);

        obj = getValueExpression(elp, pw, "parent", java.lang.String.class);
        pw.println("Get Object Representation Using Value Expression: " + obj);

        // Now we use Method Expressions to perform invocations

        // Set the parent object method using Method Expression
        setMethodExpression(elp, pw, "parent.setParentName('Steven Johnson Sr.')");

        // Get the parent object method value using Method Expression
        obj = getMethodExpression(elp, pw, "parent.getParentName()");
        pw.println("Get Parent Name Using Method Expression (Expected: \"Steven Johnson Sr.\"): " + obj);

        // Set the child object method using Method Expression
        setMethodExpression(elp, pw, "parent.child.setChildName('Steven Johnson Jr.')");

        // Get the child object method value using Method Expression
        obj = getMethodExpression(elp, pw, "parent.child.getChildName()");
        pw.println("Get Child Name Using Method Expression (Expected: \"Steven Johnson Jr.\"): " + obj);

        obj = getMethodExpression(elp, pw, "parent.toString()");
        pw.println("Get Object Representation Using Method Expression: " + obj);

    }

    /**
     * Helper method to set Value Expressions
     *
     * @param elp The ELProcessor
     * @param pw PrintWriter
     * @param expression Expression to be evaluated
     * @param value The value to be set
     */
    private void setValueExpression(ELProcessor elp, PrintWriter pw, String expression, String value) {
        try {
            elp.setValue(expression, value);
        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            pw.println("An exception was thrown: " + e.toString());
        }
    }

    /**
     * Helper method to get value using Value Expressions
     *
     * @param elp The ELProcessor
     * @param pw PrintWriter
     * @param expression Expression to be evaluated
     * @param expectedType Class type
     * @return the result of the evaluated expression
     */
    private Object getValueExpression(ELProcessor elp, PrintWriter pw, String expression, Class<?> expectedType) {
        Object obj = null;
        try {
            obj = elp.getValue(expression, expectedType);
        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            pw.println("An exception was thrown: " + e.toString());
        }
        return obj;
    }

    /**
     * Helper method to set Method Expressions
     *
     * @param elp The ELProcessor
     * @param pw PrintWriter
     * @param expression Expression to be evaluated
     */
    private void setMethodExpression(ELProcessor elp, PrintWriter pw, String expression) {
        try {
            elp.eval(expression);
        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            pw.println("An exception was thrown: " + e.toString());
        }
    }

    /**
     * Helper method to get value using Method Expressions
     *
     * @param elp The ELProcessor
     * @param pw PrintWriter
     * @param expression Expression to be evaluated
     * @return the result of the evaluated expression
     */
    private Object getMethodExpression(ELProcessor elp, PrintWriter pw, String expression) {
        Object obj = null;
        try {
            obj = elp.eval(expression);
        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            pw.println("An exception was thrown: " + e.toString());
        }
        return obj;
    }

}
