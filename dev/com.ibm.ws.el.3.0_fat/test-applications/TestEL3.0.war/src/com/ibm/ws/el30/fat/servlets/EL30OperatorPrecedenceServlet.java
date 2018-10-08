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
import java.io.PrintWriter;

import javax.el.ELProcessor;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.el30.fat.beans.EL30InvocationMethodExpressionTestBean;

/**
 * This servlet test the EL 3.0 Operator Precedence by using the ELProcessor API
 */
@WebServlet("/EL30OperatorPrecedenceServlet")
public class EL30OperatorPrecedenceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public EL30OperatorPrecedenceServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ELProcessor elp = new ELProcessor();
        PrintWriter pw = response.getWriter();
        int testCounter = 0;

        // Create an instance of the InvocationMethodExpressionTest bean to be used with [] and . operators
        EL30InvocationMethodExpressionTestBean parent = new EL30InvocationMethodExpressionTestBean();

        // Define the bean within the ELProcessor object
        elp.defineBean("parent", parent);

        // Set the parent object with a name
        evaluateOperatorPrecedence(pw, elp, "parent.setParentName('John Smith Sr.')");

        Object obj = evaluateOperatorPrecedence(pw, elp, "parent.parentName == parent['parentName']");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 [] and . operators left-to-right (Expected:true): " + obj + "</p>");

        // EL 3.0 [] operator with Parenthesis operator
        evaluateOperatorPrecedence(pw, elp, "parent.child.setChildName(parent['parentName'])");

        obj = evaluateOperatorPrecedence(pw, elp, "parent.child.childName == parent.child['childName']");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 [] and . operators left-to-right (Expected:true): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "-8-(4+2)");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 Parenthesis Operator with - (unary) (Expected:-14): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "(-8-4)+2");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 Parenthesis Operator with - (unary) (Expected:-10): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "z=null; not false && empty z");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 not ! empty operators left-to-right (Expected:true): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "x=2; (empty x && not false) || !false");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 Parenthesis Operator with not ! empty operators (Expected:true): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "empty x && (not false || !false)");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 Parenthesis Operator with not ! empty operators (Expected:false): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "4*8/8%3");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 * / div % mod operators left-to-right (Expected:1.0): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "4*8 div (8 mod 3)");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 Parenthesis Operator with * / div % mod operators (Expected:16.0): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "2+8-5");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 + - operators left-to-right (Expected:5): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "2+4*8-24/8");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 + - * / div operators (Expected:31.0): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "(2+4)*8-24/8");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 Parenthesis Operator with + - * / div operators (Expected:45.0): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "1 + 2 += \"abc\"");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 String Concatenation Operator (+=) and + operator (Expected:3abc): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "1 < 3 && 3 > 2 && 3 <= 3 && 2 >= 1 && 1 lt 3 && 3 gt 2 && 3 le 3 && 2 ge 1");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 < > <= >= lt gt le ge relational operators left-to-right (Expected:true): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "4 + 6 > 9 && 8 - 3 < 5");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 < > relational operators with + - operators (Expected:false): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "3 == 3 && 3 != 4 && 5 eq 5 && 5 ne 6");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 == != eq ne relational operators left-to-right (Expected:true): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "true == 1 <= 1");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 == and <= relational operators (Expected:true): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "false != 1 > 1");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 != and > relational operators (Expected:false): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "true || true && false");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 && and || logical operators (Expected:true): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "true or true and false");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 and or logical operators (Expected:true): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "1==1&&true?2:3");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 ? and : conditional operators (Expected:2): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "1==1&&false?2:3");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 ? and : conditional operators (Expected:3): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "((a, b) -> a>b?50:60)(2, 5)");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 -> (lambda) operator (Expected:60): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "a = 1; b = 3; w = a += b");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with concatenation operator (+=) (Expected:13): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "v = (x->x+1)(3); v = v + 1");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with lambda operator (->) (Expected:5): " + obj + "</p>");

        obj = evaluateOperatorPrecedence(pw, elp, "(x->(a=x))(10); a = a + 1");
        pw.print("<p><b>Test " + ++testCounter + ":</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with lambda operator (->) (Expected:11): " + obj + "</p>");

    }

    /**
     * Helper method to evaluate the Operator Precedence
     *
     * @param pw PrintWriter
     * @param elp The ELProcessor
     * @param expression Expression to be evaluated
     * @return the result of the evaluated expression
     */
    private Object evaluateOperatorPrecedence(PrintWriter pw, ELProcessor elp, String expression) {
        Object obj = null;
        try {
            obj = elp.eval(expression);
        } catch (Exception e) {
            pw.print("Exception caught: " + e.getMessage() + "<br/>");
            pw.print("An exception was thrown: " + e.toString() + "<br/>");
        }
        return obj;
    }

}
