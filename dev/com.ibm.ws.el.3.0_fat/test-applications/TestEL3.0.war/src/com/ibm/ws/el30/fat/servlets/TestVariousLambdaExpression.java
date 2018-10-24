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

import static org.junit.Assert.assertTrue;

import javax.el.ELException;
import javax.el.ELProcessor;
import javax.el.LambdaExpression;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.el30.fat.beans.Employee;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class TestVariousLambdaExpression
 */
@WebServlet("/TestVariousLambdaExpression")
public class TestVariousLambdaExpression extends FATServlet {
    private static final long serialVersionUID = 1L;

    private ELProcessor elp = new ELProcessor();

    public TestVariousLambdaExpression() {
        super();
    }

    @Test
    public void testLambdaParam() throws Exception {
        LambdaExpression expression = (LambdaExpression) elp.eval("(x->x+1)");

        int result = Integer.valueOf(expression.invoke("2").toString());
        assertTrue("The expression did not evaluate to 3 : " + result, result == 3);
    }

    @Test
    public void testRejectExtraLambdaParam() throws Exception {
        LambdaExpression expression = (LambdaExpression) elp.eval("x->x+1");

        int result = Integer.valueOf(expression.invoke(2, 4).toString()); // extra param should be rejected
        assertTrue("The expression did not evaluate to 3: " + result, result == 3);
    }

    @Test
    public void testMultipleLambdaParams() throws Exception {
        LambdaExpression expression = (LambdaExpression) elp.eval("((x,y)->x+y)");

        int result = Integer.valueOf(expression.invoke(3, 4).toString());
        assertTrue("The expression did not evaluate to 7: " + result, result == 7);
    }

    @Test
    public void testCatchExeptionOnLessParam() throws Exception {
        String expectedText = "Only [1] arguments were provided for a lambda expression that requires at least [2]";

        LambdaExpression expression = (LambdaExpression) elp.eval("((x,y)->x+y)");

        boolean result = false;
        String exceptionText = "";
        try {
            expression.invoke(3).toString();
        } catch (ELException elEx) {
            exceptionText = elEx.getMessage();
            result = exceptionText.contains(expectedText);
        }

        assertTrue("The exception message did not contain: " + expectedText + " but was: " + exceptionText, result);
    }

    /**
     * Assigned Lambda expression
     * incr = x->x+1")).invoke(10)
     *
     * @throws Exception
     */
    @Test
    public void testAssignedLambdaExp() throws Exception {
        LambdaExpression expression = (LambdaExpression) elp.eval("incr = x->x+1");

        int result = Integer.valueOf(expression.invoke(10).toString());

        assertTrue("The expression did not evaluate to 11: " + result, result == 11);

        // ensure we can use the incr identifier that the lambda was assigned to
        result = Integer.valueOf(elp.eval("incr(11)").toString());

        assertTrue("The expression did not evaluate to 12: " + result, result == 12);
    }

    /**
     * return the evaluated value not an expression
     * ()->64
     *
     * @throws Exception
     */
    @Test
    public void testNoParam() throws Exception {
        Integer result = Integer.valueOf(elp.eval("()->64").toString()); // will return the evaluated value not an expression

        assertTrue("The expression did not evaluate to 64: " + result, result == 64);
    }

    /**
     * The parenthesis is optional if and only if there is one parameter
     * "x->64"))).invoke(3)
     *
     * @throws Exception
     */
    @Test
    public void testOptionalParenthesis() throws Exception {
        //The parenthesis is optional if and only if there is one parameter.
        LambdaExpression expression = (LambdaExpression) elp.eval("x->64");

        Integer result = Integer.valueOf(expression.invoke(3).toString());

        assertTrue("The expression did not evaluate to 64: " + result, result == 64);
    }

    /**
     * eval will return null as println returns void, but console should have Hello World.
     * ()->System.out.println(\"Hello World\")
     *
     * @throws Exception
     */
    @Test
    public void testPrintFromBody() throws Exception {
        // eval will return null as println returns void, but console should have Hello World.
        Object result = elp.eval("()->System.out.println(\"Hello World\")");

        assertTrue("The expression did not evaluate to null: " + result, result == null);
    }

    /**
     * parameters passed in ,invoked directly
     * ((x,y)->x+y)(3,4))
     *
     * @throws Exception
     */
    @Test
    public void testInvokeFunctionImmediate() throws Exception {
        Integer result = Integer.valueOf(elp.eval("((x,y)->x+y)(3,4)").toString()); // invoked

        assertTrue("The expression did not evaluate to 7: " + result, result == 7);
    }

    /**
     * parameters passed in ,invoked indirectly
     * v = (x,y)->x+y; v(3,4)
     *
     * @throws Exception
     */
    @Test
    public void testInvokeFunctionIndirect() throws Exception {
        Integer result = Integer.valueOf(elp.eval("v = (x,y) ->x+y; v(3,4)").toString()); // invoked indirectly

        assertTrue("The expression did not evaluate to 7: " + result, result == 7);
    }

    /**
     * parameters passed in ,invoked indirectly and separate
     * t = (x,y)->x+y;
     * t(3,4)
     *
     * @throws Exception
     */
    @Test
    public void testInvokeFunctionIndirectSeperate() throws Exception {
        elp.eval("t = (x,y)->x+y"); // Separate
        Integer result = Integer.valueOf(elp.eval("t(3,4)").toString());

        assertTrue("The expression did not evaluate to 7: " + result, result == 7);
    }

    /**
     *
     * fact = n -> n==0? 1: n*fact(n-1); fact(5)
     *
     * @throws Exception
     */
    @Test
    public void testInvokeFunctionIndirect2() throws Exception {
        Integer result = Integer.valueOf(elp.eval(" fact = n -> n==0? 1: n*fact(n-1); fact(5)").toString()); // should be 5*4*3*2*1

        assertTrue("The expression did not evaluate to 120: " + result, result == 120);
    }

    /**
     *
     * employees.where(e->e.firstName == ‘Charlie’)
     *
     * @throws Exception
     */
    @Test
    public void testPassedAsArgumentToMethod() throws Exception {
        elp.defineBean("employee", new Employee("Charlie Brown", "Charlie", "Brown"));
        String name = (String) elp.eval("employee.name");

        assertTrue("The name expected is Charlie Brown but was: " + name, name.equals("Charlie Brown"));

        String result = (String) elp.eval("employee.sanitizeNames(e->e.firstname == 'Charlie')"); // // pass the Lambda as argument to method

        assertTrue("The result did not evaluate to NAME MATCHES: CHARLIE: " + result, result.equals("NAME MATCHES: Charlie"));
    }

    /**
     *
     * (elp.eval("(a1, a2) -> a1 > a2"))).invoke(2,3,4)
     *
     * @throws Exception
     */
    @Test
    public void testCompareParameters() throws Exception {
        LambdaExpression expression = (LambdaExpression) elp.eval("(a1, a2) -> a1 > a2");
        Boolean result = (Boolean) expression.invoke(2, 3, 1); // also tests that the extra parameter is ignored

        assertTrue("The result did not evaluate to false: " + result.toString(), result.equals(false));
    }

    /**
     *
     * (firstStr, secondStr)-> Integer.compare(firstStr.length(),secondStr.length())"))).invoke("First","Second")
     * should coerce to String
     *
     * @throws Exception
     */
    @Test
    public void testParameterCoerceToString() throws Exception {
        LambdaExpression expression = (LambdaExpression) elp.eval("(firstStr, secondStr)-> Integer.compare(firstStr.length(),secondStr.length())");
        String result = expression.invoke("First", "Second").toString();

        assertTrue("The result did not evaluate to -1: " + result, result.equals("-1"));
    }

    /**
     *
     * ( firstInt, secondInt)-> Integer.compare(firstInt,secondInt)"))).invoke(5,6)
     * should coerce to int
     *
     * @throws Exception
     */
    @Test
    public void testParameterCoerceToInt() throws Exception {
        LambdaExpression expression = (LambdaExpression) elp.eval("( firstInt, secondInt)-> Integer.compare(firstInt,secondInt)");
        String result = expression.invoke(5, 6).toString();

        assertTrue("The result did not evaluate to -1: " + result, result.equals("-1"));
    }

    /**
     * ${parseMe = x -> (y -> (Integer.parseInt(y)))(x) + x ; parseMe("1234")
     *
     * @throws Exception
     */
    @Test
    public void testNestedFunction1() throws Exception {
        String result = elp.eval("parseMe = x -> (y -> (Integer.parseInt(y)))(x) + x ; parseMe(\"1234\")").toString();

        assertTrue("The result did not evaluate to 2468: " + result, result.equals("2468"));
    }

}
