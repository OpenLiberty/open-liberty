/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package servlettest.web;

import java.lang.AutoCloseable;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class SimpleServlet extends HttpServlet {

    private static final String TEST_WAR = "SimpleServlet";

    public static final String AutoMessage = "This is SimpleServlet.";

    private static final String TEST_NAME = "testName";
    private static final String OK = "OK";
    private static final String FAIL = "Test failed, check logs for output";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String testName = request.getParameter(TEST_NAME);        

        System.out.println("SimpleServlet.doGet: Starting test: " + testName);
        logMethods();

        try ( PrintWriter writer = response.getWriter() ) { 
            if ( testName == null ) {
                writer.println(AutoMessage);

            } else {
                Method testMethod;
                try {
                    testMethod = getClass().getMethod(testName);
                } catch ( Throwable th ) {
                    testMethod = null;
                    writer.println(FAIL);
                    writer.println("Error retrieving test method [ " + testName + " ]: [ " + th.getMessage() + " ]");
                }

                if ( testMethod != null ) {
                    try {
                        Object testResult = testMethod.invoke(this); 
                        writer.println(testResult);
                    } catch ( Throwable th ) {
                        th.printStackTrace();
                        writer.println(FAIL);
                        writer.println("Error running test method [ " + testName + " ]: [ " + th.getMessage() + " ]");
                    }
                }
            }

        } finally {
            System.out.println("SimpleServlet.doGet: Ending test: " + testName);
        }
    }

    private boolean didLogMethods;

    private void logMethods() {
        if ( didLogMethods ) {
            return;
        }

        didLogMethods = true;

        System.out.println("Test class [ " + getClass().getName() + " ]");
        System.out.println("==================================================");
        for ( Method declaredMethod : SimpleServlet.class.getDeclaredMethods() ) {
            System.out.println("  Method [ " + declaredMethod + " ]");
        }
        System.out.println("==================================================");
    }

    //

    public String testHello() {
        return "Hello";
    }
}
