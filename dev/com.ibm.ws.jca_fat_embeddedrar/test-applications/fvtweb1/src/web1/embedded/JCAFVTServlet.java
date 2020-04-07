/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web1.embedded;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JCAFVTServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    /**
     * Message written to servlet to indicate that is has been successfully
     * invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        System.out.println("-----> " + test + " starting");
        try {
            getClass().getMethod(test, HttpServletRequest.class,
                                 HttpServletResponse.class)
                            .invoke(this, request, response);
            System.out.println("<----- " + test + " successful");
            out.println(test + " COMPLETED SUCCESSFULLY");
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
     * Verify that an Embedded Connection Factory cannot be looked up from another App
     *
     * @param request
     *                     HTTP request
     * @param response
     *                     HTTP response
     * @throws Exception
     *                       if an error occurs.
     */
    public void testEmbeddedConnectionFactoryFromDifferentEAR(
                                                              HttpServletRequest request, HttpServletResponse response) throws Throwable {

        try {
            javax.resource.cci.ConnectionFactory cf = (javax.resource.cci.ConnectionFactory) new InitialContext()
                            .lookup("ims/cf1");
            throw new Exception("Lookup passed for ims/cf1");
        } catch (NamingException ne) {
            System.out.println("Caught expected NamingException: " + ne);
        }
    }

    /**
     * Verify that an Embedded Queue and Topic cannot be looked up from another App
     *
     * @param request
     *                     HTTP request
     * @param response
     *                     HTTP response
     * @throws Exception
     *                       if an error occurs.
     */
    public void testEmbeddedAOFromDifferentEAR(
                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        try {
            Object obj = new InitialContext().lookup("jms/queue1");
            throw new Exception("Lookup passed for jms/queue1");
        } catch (NamingException ne) {
            System.out.println("Caught expected NamingException: " + ne);
        }
        try {
            Object obj = new InitialContext().lookup("jms/topic1");
            throw new Exception("Lookup passed for jms/topic1");
        } catch (NamingException ne) {
            System.out.println("Caught expected NamingException: " + ne);
        }
    }

    /**
     * Verify that an Embedded Connection Factory cannot be looked up from another App
     * indirectly
     *
     * @param request
     *                     HTTP request
     * @param response
     *                     HTTP response
     * @throws Exception
     *                       if an error occurs.
     */
    public void testEmbeddedConnectionFactoryFromDifferentEARIndirectLookup(
                                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {

        try {
            javax.resource.cci.ConnectionFactory cf = (javax.resource.cci.ConnectionFactory) new InitialContext()
                            .lookup("java:comp/env/ims/cf1");
            throw new Exception("Lookup passed for java:comp/env/ims/cf1");
        } catch (NamingException ne) {
            System.out.println("Caught expected NamingException: " + ne);
        }
    }

    /**
     * Verify that an Embedded Queue and Topic cannot be looked up from another App
     * indirectly
     *
     * @param request
     *                     HTTP request
     * @param response
     *                     HTTP response
     * @throws Exception
     *                       if an error occurs.
     */
    public void testEmbeddedAOFromDifferentEARIndirectLookup(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        try {
            Object obj = new InitialContext().lookup("java:comp/env/jms/queue1");
            throw new Exception("Lookup passed for java:comp/env/jms/queue1");
        } catch (NamingException ne) {
            System.out.println("Caught expected NamingException: " + ne);
        }
        try {
            Object obj = new InitialContext().lookup("java:comp/env/jms/topic1");
            throw new Exception("Lookup passed for java:comp/env/jms/topic1");
        } catch (NamingException ne) {
            System.out.println("Caught expected NamingException: " + ne);
        }
    }

}
