/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet base class that can be used to call remote methods. The servlet is
 * expected to be called via {@link FATServletClient}, and the servlet is
 * expected to contain test methods that match the names of the corresponding
 * methods in the JUnit test class.
 *
 * <p>Servlet test methods must have one of the following prototypes:
 * <ul>
 * <li>testMethod()
 * <li>testMethod(HttpServletRequest, HttpServletResponse)
 * </ul>
 *
 * <p><strong>Note:</strong> If you use this class, you should set {@code lib.componenttest=true} in your <em>package.properties</em> file.
 *
 * <p>Servlet test methods should indicate failure by throwing an exception,
 * typically {@link java.lang.AssertionError}. Servlets can use {@link Assert} if
 * junit.jar is visible to the application, typically by copying it
 * to {@code publish/servers/server/lib/global}.
 */
@SuppressWarnings("serial")
public abstract class FATServlet extends HttpServlet {
    public static final String SUCCESS = "SUCCESS";
    public static final String TEST_METHOD = "testMethod";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String method = request.getParameter(TEST_METHOD);

        System.out.println(">>> BEGIN: " + method);
        System.out.println("Request URL: " + request.getRequestURL() + '?' + request.getQueryString());
        PrintWriter writer = response.getWriter();
        if (method != null && method.length() > 0) {
            try {
                before();

                // Use reflection to try invoking various test method signatures:
                // 1)  method(HttpServletRequest request, HttpServletResponse response)
                // 2)  method()
                // 3)  use custom method invocation by calling invokeTest(method, request, response)
                try {
                    Method mthd = getClass().getMethod(method, HttpServletRequest.class, HttpServletResponse.class);
                    mthd.invoke(this, request, response);
                } catch (NoSuchMethodException nsme) {
                    try {
                        Method mthd = getClass().getMethod(method, (Class<?>[]) null);
                        mthd.invoke(this);
                    } catch (NoSuchMethodException nsme1) {
                        invokeTest(method, request, response);
                    }
                } finally {
                    after();
                }

                writer.println(SUCCESS);
            } catch (Throwable t) {
                if (t instanceof InvocationTargetException) {
                    t = t.getCause();
                }

                System.out.println("ERROR: " + t);
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                System.err.print(sw);

                writer.println("ERROR: Caught exception attempting to call test method " + method + " on servlet " + getClass().getName());
                t.printStackTrace(writer);
            }
        } else {
            System.out.println("ERROR: expected testMethod parameter");
            writer.println("ERROR: expected testMethod parameter");
        }

        writer.flush();
        writer.close();

        System.out.println("<<< END:   " + method);
    }

    /**
     * Override to mimic JUnit's {@code @Before} annotation.
     */
    protected void before() throws Exception {
    }

    /**
     * Override to mimic JUnit's {@code @After} annotation.
     */
    protected void after() throws Exception {
    }

    /**
     * Implement this method for custom test invocation, such as specific test method signatures
     */
    protected void invokeTest(String method, HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw new NoSuchMethodException("No such method '" + method + "' found on class "
                                        + getClass() + " with any of the following signatures:   "
                                        + method + "(HttpServletRequest, HttpServletResponse)   "
                                        + method + "()");
    }
}