/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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
import javax.el.PropertyNotWritableException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Servlet for testing the new EL 3.0 operators
 *
 * Servlet prints "Test Failed!" when that an expression does not evaluate to the expected value.
 * If "Test Failed!" is not printed, all of the expressions here evaluated as expected.
 */
@WebServlet({ "/EL30OperatorsServlet" })
public class EL30OperatorsServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    ELProcessor elp;
    String test = null;
    String expected = null;
    String expression = null;

    public EL30OperatorsServlet() {
        super();
        elp = new ELProcessor();
    }

    @Test
    public void testEL30StringConcatenationOperator_Literal() throws Exception {
        test = "EL 3.0 String Concatenation Operator (+=) with literals (Expected: xy): ";
        expression = "\"x\" += \"y\"";
        expected = "xy";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL30StringConcatenationOperator_Variables() throws Exception {
        test = "EL 3.0 String Concatenation Operator (+=) with variables (Expected: 12): ";
        expression = "testString1 = \"1\"; testString2 = \"2\"; testString1 += testString2";
        expected = "12";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL30StringConcatenationOperator_Literal_MultipleConcatenations() throws Exception {
        test = "EL 3.0 String Concatenation Operator with literals and multiple concatenations (Expected: xyz): ";
        // "x" += "y" += "z"
        expression = "\"x\" += \"y\" += \"z\"";
        expected = "xyz";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL30StringConcatenationOperator_Literal_SingleQuotes() throws Exception {
        test = "EL 3.0 String Concatenation Operator with literals and single quotes  (Expected: xyz): ";
        // 'x' += 'y' += 'z'
        expression = "'x' += 'y' += 'z'";
        expected = "xyz";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL30StringConcatenationOperator_Literal_MixedQuotes() throws Exception {
        test = "EL 3.0 String Concatenation Operator with literals and mixed quotes  (Expected: xyz): ";
        // "x" += 'y' += "z"
        expression = "\"x\" += 'y' += \"z\"";
        expected = "xyz";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL30StringConcatenationOperator_Literal_EscapeCharacters_1() throws Exception {
        test = "EL 3.0 String Concatenation Operator with literals and escape characters  (Expected: \"x\"yz): ";
        // "\"x\"" += 'y' += "z"
        expression = "\"\\\"x\\\"\" += 'y' += \"z\"";
        expected = "\"x\"yz";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL30StringConcatenationOperator_Literal_EscapeCharacters_2() throws Exception {
        test = "EL 3.0 String Concatenation Operator with literals and escape characters  (Expected: 'x'yz): ";
        // "\'x\'" += 'y' += "z"
        expression = "\"\\'x\\'\" += 'y' += \"z\"";
        expected = "'x'yz";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL30AssignmentOperator_1() throws Exception {
        test = "EL 3.0 Assignment Operator (=) (Expected:3): ";
        expression = "x=3";
        expected = "3";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL30AssignmentOperator_2() throws Exception {
        test = "EL 3.0 Assignment Operator (=) (Expected:8): ";
        expression = "x=3; y=x+5";
        expected = "8";
        testExpression(expression, expected, test);

    }

    @Test
    public void testEL30AssignmentOperator_3() throws Exception {
        test = "EL 3.0 Assignment Operator (Expected:3): ";
        expression = "x=(x->x+1)(2)";
        expected = "3";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL30AssignmentOperator_4() throws Exception {
        boolean exceptionCaught = false;
        expected = "Illegal Syntax for Set Operation";
        test = "EL 3.0 Assignment Operator (Expected:javax.el.PropertyNotWritableException: Illegal Syntax for Set Operation): ";
        expression = "null=(x->x+1)(2)";

        try {
            elp.eval(expression);
        } catch (PropertyNotWritableException e) {
            exceptionCaught = true;
            System.out.println(e.getMessage());
            System.out.println(e.toString());
            assertTrue(expected + " but was: " + e.getMessage(), e.getMessage().equals("Illegal Syntax for Set Operation"));
        }
        assertTrue("An exception was expected but was not thrown.", exceptionCaught);
    }

    @Test
    public void testEL30SemiColonOperator() throws Exception {
        test = "EL 3.0 Semi-colon Operator (Expected:8): ";
        expression = "x = 5; y = 3; z = x + y";
        expected = "8";
        testExpression(expression, expected, test);
    }

    /**
     * Evaluates an EL expression; prints "Test Failed!", along with expression info, if an expression does not
     * evaluate to the expected value.
     */
    private void testExpression(String expression, String expected, String test) throws Exception {
        String result = elp.eval(expression).toString();

        assertNotNull(result);
        assertEquals(test + " but was: " + result, expected, result);

    }

}
