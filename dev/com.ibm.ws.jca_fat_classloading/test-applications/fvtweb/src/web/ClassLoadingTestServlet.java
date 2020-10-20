/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class ClassLoadingTestServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "SUCCESS";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("testMethod");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        System.out.println("-----> " + test + " starting");
        try {
            getClass().getMethod(test, HttpServletRequest.class, PrintWriter.class).invoke(this, request, out);
            System.out.println("<----- " + test + " successful");
            out.println(test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    /**
     * Load a resource adapter class from a web application. Then retrieve a value from the resource adapter.
     *
     * @param request HTTP request
     * @param out     writer for the HTTP response
     * @throws Exception if an error occurs.
     */
    public void testLoadResourceAdapterClassFromSingleApp(HttpServletRequest request, PrintWriter out) throws Exception {

        if (!"Mr. Classy Loader".equals(ra.DummyResourceAdapter.verifyUserName())) {
            throw new Exception("Expected 'Mr. Classy Loader', instead got: " + ra.DummyResourceAdapter.verifyUserName());
        }
    }

    /**
     * Load a resource adapter class from a web application.
     *
     * @param request HTTP request
     * @param out     writer for the HTTP response
     * @throws Exception if an error occurs.
     */
    public void testApiTypeVisibilityNone(HttpServletRequest request, PrintWriter out) throws Exception {
        ClassLoader currentCl = this.getClass().getClassLoader();
        try {
            currentCl.loadClass("ra.DummyResourceAdapter");
        } catch (Exception e) {
            throw new Exception("RAR class ra.DummyResourceAdapter is not visible to the web application");
        }
    }

    /**
     * Load a resource adapter class from a web application.
     *
     * @param request HTTP request
     * @param out     writer for the HTTP response
     * @throws Exception if an error occurs.
     */
    public void testApiTypeVisibilityAll(HttpServletRequest request, PrintWriter out) throws Exception {
        ClassLoader currentCl = this.getClass().getClassLoader();
        try {
            currentCl.loadClass("ra.DummyResourceAdapter");
        } catch (Exception e) {
            throw new Exception("RAR class ra.DummyResourceAdapter is not visible to the web application");
        }
    }

    /**
     * Load a resource adapter class from a web application.
     *
     * @param request HTTP request
     * @param out     writer for the HTTP response
     * @throws Exception if an error occurs.
     */
    public void testApiTypeVisibilityMatch(HttpServletRequest request, PrintWriter out) throws Exception {
        ClassLoader currentCl = this.getClass().getClassLoader();
        try {
            currentCl.loadClass("ra.DummyResourceAdapter");
        } catch (Exception e) {
            throw new Exception("RAR class ra.DummyResourceAdapter is not visible to the web application");
        }
    }

    /**
     * Attempt to have the resource adapter load a third-party class when the ra and application
     * do not have apiTypeVisibility of type "third-party".
     */
    public void testClassSpaceRestriction(HttpServletRequest request, PrintWriter out) throws Exception {

        if (ra.DummyResourceAdapter.canGetThirdPartyClass())
            throw new Exception("Should not be able to load third party class.");
        else
            System.out.println("Was not able to load third party class, this is correct.");

    }
}
