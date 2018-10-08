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

import javax.el.ELProcessor;
import javax.el.LambdaExpression;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.el30.fat.beans.Employee;

/**
 * Servlet implementation class TestVariousLambdaExpression
 */
@WebServlet("/TestVariousLambdaExpression")
public class TestVariousLambdaExpression extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public TestVariousLambdaExpression() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWork(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // not implemented
    }

    private void doWork(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String testNumber = request.getParameter("Test");

        ServletOutputStream sos = response.getOutputStream();
        sos.println("Test EL 3.0 VariousLambdaExpression Functionality using a Servlet");

        ELProcessor elp = new ELProcessor();

        if ("1".equalsIgnoreCase(testNumber)) {
            System.out.println("Test1_LambdaParam");
            LambdaExpression expression = (LambdaExpression) elp.eval("(x->x+1)");
            sos.println("Test1: " + expression.invoke("2").toString()); // add one
        } else if ("2".equalsIgnoreCase(testNumber)) {
            System.out.println("Test2_RejectExtraLambdaParam");
            sos.println("Test2: " + ((LambdaExpression) elp.eval("x->x+1")).invoke(2, 4).toString()); // extra param should be rejected
        } else if ("3".equalsIgnoreCase(testNumber)) {
            System.out.println("Test3_MultipleLambdaParams");
            sos.println("Test3: " + ((LambdaExpression) elp.eval("((x,y)->x+y)")).invoke(3, 4).toString()); // pass in two
        } else if ("4".equalsIgnoreCase(testNumber)) {
            try {
                System.out.println("Test4_CatchExeptionOnLessParam");
                sos.println("Test4: " + ((LambdaExpression) elp.eval("((x,y)->x+y)")).invoke(3).toString()); // must fail
                // expect Message,  Only [1] arguments were provided for a lambda expression that requires at least [2]
            } catch (Exception e) {
                sos.println("Test4: " + e.getMessage().toString());
            }
        } else if ("5".equalsIgnoreCase(testNumber)) {
            System.out.println("Test5_AssignedLambdaExp");
            sos.println("Test5: " + ((LambdaExpression) elp.eval("incr = x->x+1")).invoke(10).toString()); // assigned lambda expression
        } else if ("6".equalsIgnoreCase(testNumber)) {
            System.out.println("Test6_NoParam");
            sos.println("Test6: " + (elp.eval("()->64"))); // will return the evaluated value not an expression
        } else if ("7".equalsIgnoreCase(testNumber)) {
            try {
                System.out.println("Test7_OptionalParenthesis");
                sos.println("Test7: " + ((LambdaExpression) (elp.eval("x->64"))).invoke(3).toString()); //The parenthesis is optional if and only if there is one parameter.
            } catch (Exception e) {
                sos.println("Test7: e-->" + e.getMessage().toString());

            }

        } else if ("8".equalsIgnoreCase(testNumber)) {
            try {
                System.out.println("Test8_PrintFromBody");
                sos.println("Test8: " + (elp.eval("()->System.out.println(\"Hello World\")")));
                // eval will return null as println returns void, but console should have Hello World.
            } catch (Exception e) {
                sos.println("Test8: e-->" + e.getMessage().toString());

            }

        } else if ("9".equalsIgnoreCase(testNumber)) {
            System.out.println("Test9_InvokeFunctionImmediate");
            sos.println("Test9: " + (elp.eval("((x,y)->x+y)(3,4)"))); // invoked

        } else if ("10".equalsIgnoreCase(testNumber)) {
            System.out.println("Test10_InvokeFunctionIndirect");
            sos.println("Test10: " + (elp.eval("v = (x,y)->x+y; v(3,4)"))); // invoked indirectly

        } else if ("11".equalsIgnoreCase(testNumber)) {

            try {
                System.out.println("Test11_InvokeFunctionIndirectSeperate");
                sos.println("Test11: " + elp.eval("v = (x,y)->x+y"));
                sos.println("Test11: " + elp.eval("v(3,4)")); // seperate
            } catch (Exception e) {
                sos.println("Test10: e-->" + e.getMessage().toString());

            }

        } else if ("12".equalsIgnoreCase(testNumber)) {
            System.out.println("Test12_InvokeFunctionIndirect2");
            sos.println("Test12: " + elp.eval(" fact = n -> n==0? 1: n*fact(n-1); fact(5) ")); // should be 5*4*3*2*1

        } else if ("13".equalsIgnoreCase(testNumber)) {

            //employees.where(e->e.firstName == Bob)
            try {
                System.out.println("Test13_PassedAsArgumentToMethod");
                elp.defineBean("employee", new Employee("Charlie Brown", "Charlie", "Brown"));
                String name = (String) elp.eval("employee.name");
                sos.println("Test13: " + name);

                sos.println("Test13: " + (elp.eval("employee.sanitizeNames(e->e.firstname == 'Charlie')"))); // pass the Lambda as argument to method

            } catch (Exception e) {
                sos.println("Test13: e-->" + e.getMessage().toString());

            }
        } else if ("14".equalsIgnoreCase(testNumber)) {

            sos.println("Test14: -------------------------------------");
            System.out.println("Test14_CompareParameters");
            sos.println("Test14: " + ((LambdaExpression) (elp.eval("(a1, a2) -> a1 > a2"))).invoke(2, 3, 4).toString());
        } else if ("15".equalsIgnoreCase(testNumber)) {
            sos.println("Test15: -------------------------------------");
            System.out.println("Test15_ParameterCocerceToString");
            sos.println("Test15: " +
                        ((LambdaExpression) (elp.eval("(firstStr, secondStr)-> Integer.compare(firstStr.length(),secondStr.length())"))).invoke("First", "Second").toString());
        } else if ("16".equalsIgnoreCase(testNumber)) {
            sos.println("Test16: -------------------------------------");
            System.out.println("Test16_ParameterCocerceToInt");
            sos.println("Test16: " +
                        ((LambdaExpression) (elp.eval("( firstInt, secondInt)-> Integer.compare(firstInt,secondInt)"))).invoke(5, 6).toString());
        } else if ("18".equalsIgnoreCase(testNumber)) {
            sos.println("Test18: -------------------------------------");
            System.out.println("Test18_LambdaNestedFunction1");
            sos.println("Test18: " + elp.eval("parseMe = x -> (y -> (Integer.parseInt(y)))(x) + x ; parseMe(\"1234\")"));
        }
    }

}
