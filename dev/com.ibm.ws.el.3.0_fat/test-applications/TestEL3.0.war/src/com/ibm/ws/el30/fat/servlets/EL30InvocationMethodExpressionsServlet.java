/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el30.fat.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.el.ELProcessor;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.el30.fat.beans.EL30InvocationMethodExpressionTestBean;

import componenttest.app.FATServlet;

/**
 * This servlet test the invocation of Method Expressions
 */
@WebServlet("/EL30InvocationMethodExpressionsServlet")
public class EL30InvocationMethodExpressionsServlet extends FATServlet {

    private static final long serialVersionUID = 1L;
    ELProcessor elp;

    /**
     * Constructor
     */
    public EL30InvocationMethodExpressionsServlet() {
        super();

        elp = new ELProcessor();

        // Create an instance of the InvocationMethodExpressionTest bean
        EL30InvocationMethodExpressionTestBean parent = new EL30InvocationMethodExpressionTestBean();

        // Add the parent object to the ELProcessor
        elp.defineBean("parent", parent);

    }

    @Test
    public void testEL30InvocationMethodExpressions() throws Exception {
        // Lets first use Value Expressions to perform invocations

        // Set the parent object property using Value Expression
        elp.setValue("parent.parentName", "John Smith Sr.");

        // Get the parent object property value using Value Expression
        getValueExpression("parent.parentName", java.lang.String.class, "John Smith Sr.");

        // Set the child object property using Value Expression
        elp.setValue("parent.child.childName", "John Smith Jr.");

        // Get the child object property value using Value Expression
        getValueExpression("parent.child.childName", java.lang.String.class, "John Smith Jr.");

        getValueExpression("parent", java.lang.String.class, "toString method of object with current parent name John Smith Sr.");

        // Now we use Method Expressions to perform invocations

        // Set the parent object method using Method Expression
        elp.eval("parent.setParentName('Steven Johnson Sr.')");

        // Get the parent object method value using Method Expression
        getMethodExpression("parent.getParentName()", "Steven Johnson Sr.");

        // Set the child object method using Method Expression
        elp.eval("parent.child.setChildName('Steven Johnson Jr.')");

        // Get the child object method value using Method Expression
        getMethodExpression("parent.child.getChildName()", "Steven Johnson Jr.");

        getMethodExpression("parent.toString()", "toString method of object with current parent name Steven Johnson Sr.");

    }

    /**
     * Helper method to get value using Value Expressions
     *
     * @param expression Expression to be evaluated
     * @param expectedType Class type
     * @return the result of the evaluated expression
     */
    private void getValueExpression(String expression, Class<?> expectedType, String expectedResult) throws Exception {
        String result;

        Object obj = elp.getValue(expression, expectedType);

        assertNotNull(obj);

        result = obj.toString();

        assertEquals("The expression did not evaluate to: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    /**
     * Helper method to get value using Method Expressions
     *
     * @param expression Expression to be evaluated
     * @return the result of the evaluated expression
     */
    private void getMethodExpression(String expression, String expectedResult) throws Exception {
        String result;
        Object obj = elp.eval(expression);
        assertNotNull(obj);
        result = obj.toString();

        assertEquals("The expression did not evaluate to: " + expectedResult + " but was: " + result, expectedResult, result);
    }

}
