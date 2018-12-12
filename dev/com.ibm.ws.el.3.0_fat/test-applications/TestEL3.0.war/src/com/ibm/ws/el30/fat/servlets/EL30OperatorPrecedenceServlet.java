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
 * This servlet test the EL 3.0 Operator Precedence by using the ELProcessor API
 */
@WebServlet("/EL30OperatorPrecedenceServlet")
public class EL30OperatorPrecedenceServlet extends FATServlet {

    private static final long serialVersionUID = 1L;
    private ELProcessor elp;

    /**
     * Constructor
     */
    public EL30OperatorPrecedenceServlet() {
        super();
        elp = new ELProcessor();

        // Create an instance of the InvocationMethodExpressionTest bean to be used with [] and . operators
        EL30InvocationMethodExpressionTestBean parent = new EL30InvocationMethodExpressionTestBean();

        // Define the bean within the ELProcessor object
        elp.defineBean("parent", parent);

        // Set the parent object with a name
        elp.eval("parent.setParentName('John Smith Sr.')");

        // EL 3.0 [] operator with Parenthesis operator
        elp.eval("parent.child.setChildName(parent['parentName'])");
    }

    @Test
    public void testOperatorPrecedenceTest1() throws Exception {
        String errorMessage = "EL 3.0 [] and . operators left-to-right (Expected:true):";
        evaluateOperatorPrecedence("parent.parentName == parent['parentName']", "true", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest2() throws Exception {
        String errorMessage = "EL 3.0 [] and . operators left-to-right (Expected:true):";
        evaluateOperatorPrecedence("parent.child.childName == parent.child['childName']", "true", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest3() throws Exception {
        String errorMessage = "EL 3.0 Parenthesis Operator with - (unary) (Expected:-14):";
        evaluateOperatorPrecedence("-8-(4+2)", "-14", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest4() throws Exception {
        String errorMessage = "EL 3.0 Parenthesis Operator with - (unary) (Expected:-10):";
        evaluateOperatorPrecedence("(-8-4)+2", "-10", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest5() throws Exception {
        String errorMessage = "EL 3.0 not ! empty operators left-to-right (Expected:true):";
        evaluateOperatorPrecedence("z=null; not false && empty z", "true", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest6() throws Exception {
        String errorMessage = "EL 3.0 Parenthesis Operator with not ! empty operators (Expected:true):";
        evaluateOperatorPrecedence("x=2; (empty x && not false) || !false", "true", errorMessage);

    }

    @Test
    public void testOperatorPrecedenceTest7() throws Exception {
        String errorMessage = "EL 3.0 Parenthesis Operator with not ! empty operators (Expected:false):";
        evaluateOperatorPrecedence("x=2; empty x && (not false || !false)", "false", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest8() throws Exception {
        String errorMessage = "EL 3.0 * / div % mod operators left-to-right (Expected:1.0):";
        evaluateOperatorPrecedence("4*8/8%3", "1.0", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest9() throws Exception {
        String errorMessage = "EL 3.0 Parenthesis Operator with * / div % mod operators (Expected:16.0):";
        evaluateOperatorPrecedence("4*8 div (8 mod 3)", "16.0", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest10() throws Exception {
        String errorMessage = "EL 3.0 + - operators left-to-right (Expected:5):";
        evaluateOperatorPrecedence("2+8-5", "5", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest11() throws Exception {
        String errorMessage = "EL 3.0 + - * / div operators (Expected:31.0):";
        evaluateOperatorPrecedence("2+4*8-24/8", "31.0", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest12() throws Exception {
        String errorMessage = "EL 3.0 Parenthesis Operator with + - * / div operators (Expected:45.0):";
        evaluateOperatorPrecedence("(2+4)*8-24/8", "45.0", errorMessage);

    }

    @Test
    public void testOperatorPrecedenceTest13() throws Exception {
        String errorMessage = "EL 3.0 String Concatenation Operator (+=) and + operator (Expected:3abc):";
        evaluateOperatorPrecedence("1 + 2 += \"abc\"", "3abc", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest14() throws Exception {
        String errorMessage = "EL 3.0 < > <= >= lt gt le ge relational operators left-to-right (Expected:true):";
        evaluateOperatorPrecedence("1 < 3 && 3 > 2 && 3 <= 3 && 2 >= 1 && 1 lt 3 && 3 gt 2 && 3 le 3 && 2 ge 1", "true", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest15() throws Exception {
        String errorMessage = "EL 3.0 < > relational operators with + - operators (Expected:false):";
        evaluateOperatorPrecedence("4 + 6 > 9 && 8 - 3 < 5", "false", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest16() throws Exception {
        String errorMessage = "EL 3.0 == != eq ne relational operators left-to-right (Expected:true):";
        evaluateOperatorPrecedence("3 == 3 && 3 != 4 && 5 eq 5 && 5 ne 6", "true", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest17() throws Exception {
        String errorMessage = "EL 3.0 == and <= relational operators (Expected:true):";
        evaluateOperatorPrecedence("true == 1 <= 1", "true", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest18() throws Exception {
        String errorMessage = "EL 3.0 != and > relational operators (Expected:false):";
        evaluateOperatorPrecedence("false != 1 > 1", "false", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest19() throws Exception {
        String errorMessage = "EL 3.0 && and || logical operators (Expected:true):";
        evaluateOperatorPrecedence("true || true && false", "true", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest20() throws Exception {
        String errorMessage = "EL 3.0 and or logical operators (Expected:true):";
        evaluateOperatorPrecedence("true or true and false", "true", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest21() throws Exception {
        String errorMessage = "EL 3.0 ? and : conditional operators (Expected:2):";
        evaluateOperatorPrecedence("1==1&&true?2:3", "2", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest22() throws Exception {
        String errorMessage = "EL 3.0 ? and : conditional operators (Expected:3):";
        evaluateOperatorPrecedence("1==1&&false?2:3", "3", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest23() throws Exception {
        String errorMessage = "EL 3.0 -> (lambda) operator (Expected:60):";
        evaluateOperatorPrecedence("((a, b) -> a>b?50:60)(2, 5)", "60", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest24() throws Exception {
        String errorMessage = "EL 3.0 Assignment (=) and Semi-colon (;) operators with concatenation operator (+=) (Expected:13):";
        evaluateOperatorPrecedence("a = 1; b = 3; w = a += b", "13", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest25() throws Exception {
        String errorMessage = "EL 3.0 Assignment (=) and Semi-colon (;) operators with lambda operator (->) (Expected:5):";
        evaluateOperatorPrecedence("v = (x->x+1)(3); v = v + 1", "5", errorMessage);
    }

    @Test
    public void testOperatorPrecedenceTest26() throws Exception {
        String errorMessage = "EL 3.0 Assignment (=) and Semi-colon (;) operators with lambda operator (->) (Expected:11):";
        evaluateOperatorPrecedence("(x->(a=x))(10); a = a + 1", "11", errorMessage);
    }

    /**
     * Helper method to evaluate the Operator Precedence
     *
     * @param expression Expression to be evaluated
     * @return the result of the evaluated expression
     */
    private void evaluateOperatorPrecedence(String expression, String expectedResult, String message) {
        Object obj = elp.eval(expression);
        String result = obj.toString();
        assertNotNull(obj);
        assertEquals(message + " but was: " + result, expectedResult, result);
    }

}
