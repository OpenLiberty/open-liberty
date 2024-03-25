/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.el30.fat.servlets;

import static componenttest.annotation.SkipForRepeat.EE10_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.el.ELContext;
import javax.el.ELManager;
import javax.el.ELProcessor;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.el30.fat.beans.EL30InvocationMethodExpressionTestBean;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import junit.framework.Assert;

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
     * The isParmetersProvided method was removed in the Expression Language 5.0 API.
     *
     * The isParmetersProvided method was previously deprecated in favor of the correctly spelled
     * method: isParametersProvided.
     *
     * Ensure that the isParmetersProvided method is available in version of the Expression Language before EE10, Expression Language 5.0.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(EE10_OR_LATER_FEATURES)
    public void testMethodExpression_isParmetersProvided_available() throws Exception {
        boolean exceptionOccurred = false;
        ELProcessor elp = new ELProcessor();
        ELContext context = elp.getELManager().getELContext();
        ExpressionFactory factory = ELManager.getExpressionFactory();
        MethodExpression testMethodExpression = factory.createMethodExpression(context, "#{testBean.testMethod}", Void.class, new Class<?>[] { String.class });
        try {
            testMethodExpression.isParmetersProvided();
        } catch (NoSuchMethodError nsme) {
            exceptionOccurred = true;
        }

        Assert.assertFalse("The isParmetersProvided() method was not available and should have been.", exceptionOccurred);
    }

    /**
     * The isParmetersProvided method was removed in the Expression Language 5.0 API.
     *
     * The isParmetersProvided method was previously deprecated in favor of the correctly spelled
     * method: isParametersProvided.
     *
     * Since Jakarta EE10, Expression Language 5.0 is being tested the test will ensure the method isParmetersProvided is not available.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat({ NO_MODIFICATION, EE9_FEATURES })
    public void testMethodExpression_isParmetersProvided_not_available() throws Exception {
        boolean exceptionOccurred = false;
        ELProcessor elp = new ELProcessor();
        ELContext context = elp.getELManager().getELContext();
        ExpressionFactory factory = ELManager.getExpressionFactory();
        MethodExpression testMethodExpression = factory.createMethodExpression(context, "#{testBean.testMethod}", Void.class, new Class<?>[] { String.class });
        try {
            testMethodExpression.isParmetersProvided();
        } catch (NoSuchMethodError e) {
            exceptionOccurred = true;
        }

        Assert.assertTrue("The isParmetersProvided() method was available and should not have been.", exceptionOccurred);

    }

    /**
     * Helper method to get value using Value Expressions
     *
     * @param expression   Expression to be evaluated
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
