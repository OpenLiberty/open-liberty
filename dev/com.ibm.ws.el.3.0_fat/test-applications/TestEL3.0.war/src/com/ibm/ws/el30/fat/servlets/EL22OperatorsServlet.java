/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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
 * Servlet for testing the EL 2.2 operators, to make sure they function properly in the
 * EL 3.0 implementation.
 *
 */
@WebServlet({ "/EL22OperatorsServlet" })
public class EL22OperatorsServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    ELProcessor elp;
    String test = null;
    String expression = null;
    String expected = null;

    public EL22OperatorsServlet() {
        super();

        elp = new ELProcessor();
    }

    @Test
    public void testEL22MultiplicationOperator() throws Exception {
        test = "EL 2.2 Multiplication Operator (Expected:16): ";
        expression = "8*2";
        expected = "16";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22AdditionOperator() throws Exception {
        test = "EL 2.2 Addition Operator (+) (Expected:5): ";
        expression = "2+3";
        expected = "5";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22SubtractionOperator() throws Exception {
        test = "EL 2.2 Subtraction Operator (-) (Expected:1): ";
        expression = "5-4";
        expected = "1";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22DivisionOperator_1() throws Exception {
        test = "EL 2.2 Division Operator (/) (Expected:8.0): ";
        expression = "16/2";
        expected = "8.0";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22DivisionOperator_2() throws Exception {
        test = "EL 2.2 Division Operator (div) (Expected:8.0): ";
        expression = "16 div 2";
        expected = "8.0";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RemainderOperator_1() throws Exception {
        test = "EL 2.2 Remainder Operator (%) (Expected:1): ";
        expression = "19%2";
        expected = "1";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RemainderOperator_2() throws Exception {
        test = "EL 2.2 Remainder Operator (mod) (Expected:1): ";
        expression = "19 mod 2";
        expected = "1";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperator_Equals_1() throws Exception {
        test = "EL 2.2 Relational Operator (==) (Expected: true): ";
        expression = "3 == 3";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperator_Equals_2() throws Exception {
        test = "EL 2.2 Relational Operator (eq) (Expected: false): ";
        expression = "3 eq 4";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorNotEqual_1() throws Exception {
        test = "EL 2.2 Relational Operator (!=) (Expected: true): ";
        expression = "3 != 4";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorNotEqual_2() throws Exception {
        test = "EL 2.2 Relational Operator (ne) (Expected: false): ";
        expression = "3 ne 3";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorLessThan_1() throws Exception {

        test = "EL 2.2 Relational Operator (<) (Expected: true): ";
        expression = "3 < 4";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorLessThan_2() throws Exception {
        test = "EL 2.2 Relational Operator (lt) (Expected: false): ";
        expression = "5 lt 4";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorGreaterThan_1() throws Exception {
        test = "EL 2.2 Relational Operator (>) (Expected: false): ";
        expression = "3 > 4";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorGreaterThan_2() throws Exception {
        test = "EL 2.2 Relational Operator (gt) (Expected: true): ";
        expression = "5 gt 4";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorLessThanEqual_1() throws Exception {
        test = "EL 2.2 Relational Operator (<=) (Expected: true): ";
        expression = "3 <= 4";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorLessThanEqual_2() throws Exception {
        test = "EL 2.2 Relational Operator (le) (Expected: false): ";
        expression = "5 le 4";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorLessThanEqual_3() throws Exception {
        test = "EL 2.2 Relational Operator (le) (Expected: true): ";
        expression = "3 le 3";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorGreaterThanEqual_1() throws Exception {
        test = "EL 2.2 Relational Operator (>=) (Expected: true): ";
        expression = "5 >= 4";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorGreaterThanEqual_2() throws Exception {
        test = "EL 2.2 Relational Operator (ge) (Expected: false): ";
        expression = "3 ge 4";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorGreaterThanEqual_3() throws Exception {
        test = "EL 2.2 Relational Operator (ge) (Expected: true): ";
        expression = "3 ge 3";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorAnd_1() throws Exception {
        test = "EL 2.2 Logical Operator (&&) (Expected: false): ";
        expression = "true && false";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorAnd_2() throws Exception {
        test = "EL 2.2 Logical Operator (&&) (Expected: true): ";
        expression = "true && true";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorAnd_3() throws Exception {
        test = "EL 2.2 Logical Operator (and) (Expected: false): ";
        expression = "false and false";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorOr_1() throws Exception {
        test = "EL 2.2 Logical Operator (||) (Expected: true): ";
        expression = "true || false";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorOr_2() throws Exception {
        test = "EL 2.2 Logical Operator (||) (Expected: false): ";
        expression = "false || false";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22RelationalOperatorOr_3() throws Exception {
        test = "EL 2.2 Logical Operator (or) (Expected: true): ";
        expression = "true or false";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22LogicalOperatorNot_1() throws Exception {
        test = "EL 2.2 Logical Operator (!) (Expected: false): ";
        expression = "!true";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22LogicalOperatorNot_2() throws Exception {
        test = "EL 2.2 Logical Operator (!) (Expected: true): ";
        expression = "!false";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22LogicalOperatorNot_3() throws Exception {
        test = "EL 2.2 Logical Operator (not) (Expected: true): ";
        expression = "not false";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22EmptyOperator_null() throws Exception {
        test = "EL 2.2 Empty Operator (empty) (Expected: true): ";
        expression = "b=null; empty b";
        expected = "true";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22EmptyOperator_false() throws Exception {
        test = "EL 2.2 Empty Operator (empty) (Expected: false): ";
        expression = "x=5; empty x";
        expected = "false";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22EmptyOperator_Undefined() throws Exception {
        /*
         * This throws a javax.el.PropertyNotFoundException here; however, in a JSP it evaluates
         * to true. The property has not been defined so it makes sense for a PropertyNotFoundExcepion.
         */
        boolean exceptionThrown = false;
        String exceptionMessage = "ELResolver cannot handle a null base Object with identifier";
        test = "EL 2.2 Empty Operator (empty) (Expected: PropertyNotFoundException): ";
        expression = "empty z";
        expected = "PropertyNotFoundException";

        try {
            elp.eval(expression).toString();
        } catch (javax.el.PropertyNotFoundException pnfe) {
            exceptionThrown = true;
            String message = pnfe.getMessage();
            assertTrue("The exception did not contain the following message: " + exceptionMessage, message.contains(exceptionMessage) && message.contains("z"));
        }
        assertTrue("A PropertyNotFoundException was expected but was not thrown.", exceptionThrown);
    }

    @Test
    public void testEL22ConditionalOperator_1() throws Exception {
        test = "EL 2.2 Conditional Operator (A?B:C) (Expected: 2): ";
        expression = "1==1?2:3";
        expected = "2";
        testExpression(expression, expected, test);
    }

    @Test
    public void testEL22ConditionalOperator_2() throws Exception {
        test = "EL 2.2 Conditional Operator (A?B:C) (Expected: 3): ";
        expression = "1==2?2:3";
        expected = "3";
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

