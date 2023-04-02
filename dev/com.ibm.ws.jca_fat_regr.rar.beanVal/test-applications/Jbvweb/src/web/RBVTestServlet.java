/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class RBVTestServlet extends HttpServlet {

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";
    private final String servletName = "RBVTestServlet";
    private final static String TestAO = "TestAO",
                    TestAOSuccess = "TestAOSuccess",
                    TestEmbeddedAO = "TestEmbeddedAO",
                    TestMCFSuccess = "TestMCFSuccess",
                    TestMCFFailure = "TestMCFFailure";

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String test = request.getParameter("test");
        final PrintWriter out = response.getWriter();
        out.println(" ---> " + servletName + " is starting " + test + "<br>");
        System.out.println(" ---> " + servletName + " is starting test: " + test);

        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
            System.out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException)
                e = e.getCause();
            out.println("<pre>ERROR in " + test + ":");
            e.printStackTrace(out);
            out.println("</pre>");
            e.printStackTrace();
            out.println(" <--- " + test + " FAILED");
            System.out.println(" <--- " + test + " FAILED");
        } finally {
            out.flush();
            out.close();
        }
    }

    public void testJavaBeanValidationSuccessStandaloneAO(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String method = "testJavaBeanValidationSuccessStandaloneAO";
        final String administeredObjectName = TestAOSuccess;
        System.out.println("Entering " + method);
        try {
            Object JbvAO = new InitialContext().lookup("jms/" + administeredObjectName);
            System.out.println("As expected, Object is bound in JNDI for " + method + ": " + JbvAO);
        } catch (NamingException e) {
            e.printStackTrace(System.out);
            throw e;
        }
        System.out.println("Exiting " + method);
    }

    public void testJavaBeanValidationFailureStandaloneAO(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String method = "testJavaBeanValidationFailureStandaloneAO";
        final String administeredObjectName = TestAO;
        System.out.println("Entering " + method);
        Object JbvAO = null;
        try {
            JbvAO = new InitialContext().lookup("jms/" + administeredObjectName);
            throw new Exception("Opposite to expectation, Object is bound in JNDI for " + method + ": " + JbvAO);
        } catch (NamingException e) {
            System.out.println("As expected, Object is not bound in JNDI for " + method + ": " + JbvAO);
        }
        System.out.println("Exiting " + method);
    }

    public void testJavaBeanValidationSuccessEmbeddedAO(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String method = "testJavaBeanValidationSuccessEmbeddedAO";
        final String administeredObjectName = TestEmbeddedAO;
        final String jndiName = "jms/" + administeredObjectName;
        System.out.println("Entering " + method);
        try {
            Object impl = new InitialContext().lookup(jndiName);
            System.out.println("As expected, Object is bound in JNDI for " + method + ": " + impl);
        } catch (NamingException e) {
            e.printStackTrace(System.out);
            throw e;
        }
        System.out.println("Exiting " + method);
    }

    public void testJavaBeanValidationFailureEmbeddedAO(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String method = "testJavaBeanValidationFailureEmbeddedAO";
        final String jndiName = "jms/" + TestEmbeddedAO + "1";
        System.out.println("Entering " + method);
        Object ao = null;
        try {
            ao = new InitialContext().lookup(jndiName);
            throw new Exception("Opposite to expectation, Object is bound in JNDI for " + method + ": " + ao);
        } catch (Exception e) {
            System.out.println("As expected, Object is not bound in JNDI for " + method + ": " + ao);
        }
        System.out.println("Exiting " + method);
    }

    public void testJavaBeanValidationSuccessStandaloneMCF(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String method = "testJavaBeanValidationSuccessStandaloneMCF";
        final String jndiName = "jms/" + TestMCFSuccess;
        System.out.println("Entering " + method);
        try {
            Object mcf = new InitialContext().lookup(jndiName);
            // This message will be looked for in testJavaBeanValidationSuccessStandaloneMCF() in RarBeanValidationTest class
            System.out.println("09292014TE01 As expected Object is bound in JNDI for " + method + ": " + mcf);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        System.out.println("Exiting " + method);
    }

    public void testJavaBeanValidationFailureStandaloneMCF(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String method = "testJavaBeanValidationFailureStandaloneMCF";
        final String jndiName = "jms/" + TestMCFFailure;
        System.out.println("Entering " + method);
        Object mcf = null;
        try {
            mcf = new InitialContext().lookup(jndiName);
            throw new Exception("Opposite to expectation, Object is bound in JNDI for " + method + ": " + mcf);
        } catch (Exception e) {
            // This message will be looked for in testJavaBeanValidationFailureStandaloneMCF() in RarBeanValidationTest class
            System.out.println("09292014TE02 As expected Object is not bound in JNDI for " + method + ": " + mcf);
        }
        System.out.println("Exiting " + method);
    }
}
