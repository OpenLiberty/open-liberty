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
import static org.junit.Assert.assertTrue;

import javax.el.ELProcessor;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Servlet for testing the new EL 3.0 static field/method functionality
 */
@WebServlet("/EL30StaticFieldsAndMethodsServlet")
public class EL30StaticFieldsAndMethodsServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    public static final String out = "now";
    ELProcessor elp;

    public EL30StaticFieldsAndMethodsServlet() {
        super();
        elp = new ELProcessor();
        elp.getELManager().importClass("com.ibm.ws.el30.fat.beans.EL30StaticFieldsAndMethodsBean");
        elp.getELManager().importClass("com.ibm.ws.el30.fat.beans.EL30StaticFieldsAndMethodsEnum");
    }

    /**
     * Test Boolean.TRUE
     */
    @Test
    public void testBooleanStatic() throws Exception {
        evaluateExpression("Boolean.TRUE", "true");

    }

    /**
     * Invoke a constructor with an argument (spec 1.22)
     *
     * @throws Exception
     */
    @Test
    public void testConstructor() throws Exception {
        evaluateExpression("Boolean(true)", "true");

    }

    /**
     * Reference a static field (spec 1.22)
     *
     * @throws Exception
     */
    @Test
    public void testStaticField() throws Exception {
        evaluateExpression("Integer.MAX_VALUE", new Integer(Integer.MAX_VALUE).toString());
    }

    /**
     * Reference a static method (spec 1.22)
     *
     * @throws Exception
     */
    @Test
    public void testStaticMethod() throws Exception {
        String result = evaluateExpression("System.currentTimeMillis()").toString();

        assertNotNull(result);
        assertTrue("The value was expected to be greater than 0 however it was not: " + result, Long.parseLong(result) > 0);

    }

    /**
     * Invoke a constructor with an argument (spec 1.22.3)
     *
     * @throws Exception
     */
    @Test
    public void testInvokeConstructorWithArgument() throws Exception {
        evaluateExpression("Integer('1000')", "1000");
    }

    /**
     * Try to modify a static field (spec 1.22.1 #2)
     *
     * @throws Exception
     */
    @Test
    public void testModifyStaticField() throws Exception {

        Object result = evaluateExpression("Integer.MAX_VALUE = 1");

        assertNotNull(result);

        //Writing to static fields (in this case field [MAX_VALUE] on class [java.lang.Integer]) is not permitted
        assertTrue(result.toString().contains("javax.el.PropertyNotWritableException"));
    }

    /**
     * Try to reference a field whose class hasn't been imported (spec 1.22.1 #3)
     *
     * @throws Exception
     */
    @Test
    public void testNonImportedClassField() throws Exception {

        Object result = evaluateExpression("java.math.RoundingMode.CEILING");

        assertNotNull(result);

        assertTrue(result.toString().contains("javax.el.PropertyNotFoundException"));
    }

    // Test EL30StaticFieldsAndMethodsBean and EL30StaticFieldsAndMethodsEnum classes

    /**
     * Reference a static field on custom enum
     *
     * @throws Exception
     */
    @Test
    public void testStaticFiledCustomEnum() throws Exception {

        evaluateExpression("EL30StaticFieldsAndMethodsEnum.TEST_ONE", "TEST_ONE");
    }

    /**
     * Reference a static field on a custom class
     *
     * @throws Exception
     */
    @Test
    public void testStaticFieldOnCustomClass() throws Exception {

        evaluateExpression("EL30StaticFieldsAndMethodsBean.staticReference", "static reference");
    }

    /**
     * Call a zero parameter custom method
     *
     * @throws Exception
     */
    @Test
    public void testZeroParameterCustomMethod() throws Exception {
        evaluateExpression("EL30StaticFieldsAndMethodsBean.staticMethod()", "static method");
    }

    /**
     * Call a one parameter custom method
     *
     * @throws Exception
     */
    @Test
    public void testOneParameterCustomMethod() throws Exception {
        evaluateExpression("EL30StaticFieldsAndMethodsBean.staticMethodParam('static method param')", "static method param");
    }

    //Try to reference a non-static field
    @Test
    public void testNonStaticField() throws Exception {

        Object result = evaluateExpression("EL30StaticFieldsAndMethodBean.nonStaticReference");

        assertNotNull(result);

        assertTrue(result.toString().contains("javax.el.PropertyNotFoundException"));
    }

    /**
     * Try to reference a non-static method
     *
     * @throws Exception
     */
    @Test
    public void testNonStaticMethod() throws Exception {
        Object result = evaluateExpression("EL30StaticFieldsAndMethodsBean.nonStaticMethod()");

        assertNotNull(result);

        assertTrue(result.toString().contains("javax.el.MethodNotFoundException"));
    }

    /*
     *
     */
    private void evaluateExpression(String expression, String expectedResult) throws Exception {
        String result = elp.eval(expression).toString();

        System.out.println("Result: " + result);

        assertNotNull(result);
        assertEquals("The expression did not evaluate to: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    /*
     *
     */
    private Object evaluateExpression(String expression) {
        Object result = null;

        try {
            result = elp.eval(expression);
        } catch (Exception e) {
            System.out.println("The following expression was thrown: " + e.getMessage() + " while evaluating the following expression: " + expression);
            result = e.getClass() + " " + e.getMessage();
        }

        System.out.println("Result: " + result);

        return result;
    }

}
