/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fvtweb.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.naming.InitialContext;

import org.junit.Test;

//import fvtweb.ejb.JavamailTestLocal;
import jakarta.annotation.Resource;
import jakarta.mail.MailSessionDefinition;
import jakarta.mail.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@MailSessionDefinition(name = "javamail/jm2Def",
                       from = "jm2From",
                       description = "jm2Desc",
                       storeProtocol = "jm2StoreProtocol",
                       transportProtocol = "jm2TransportProtocol",
                       properties = { "test=jm2Def_MailSession" },
                       user = "jm2test",
                       password = "testJm2test")
@MailSessionDefinition(name = "javamail/mergeDef",
                       user = "mergeAnnotationUser",
                       from = "mergeAnnotationFrom",
                       password = "mergePass",
                       properties = { "test=mergeDef_MailSession" })
@WebServlet("/*")
public class JavamailFATServlet extends HttpServlet {
    public static final String SUCCESS = "SUCCESS";
    public static final String TEST_METHOD = "testMethod";

    @Resource(name = "javamail/jm2", lookup = "java:comp/env/javamail/jm2Def")
    private Session jm2;

    @Resource(name = "javamail/mergeMS", lookup = "java:comp/env/javamail/mergeDef")
    private Session mergeMS;

    //@EJB
    //JavamailTestLocal jtBean;

    private static final long serialVersionUID = 7709282314904580334L;

    /**
     * Verify a mail session is created from deployment descriptor config. Only tests 1 descriptor field (the merge test
     * covers the rest).
     */
    @Test
    public void testDDJavamailSessionCreated() throws Throwable {
        Session jm1 = (Session) new InitialContext().lookup("java:comp/env/javamail/jm1Def");
        Properties props = jm1.getProperties();
        System.out.println("JavamailFATServlet.testDDJavamailSessionCreated properties : " + props.toString());

        // Validate we got the session we expected
        String userValue = jm1.getProperty("mail.user");
        if (("jm1test").equals(userValue)) {
            // Success!
        } else {
            throw new Exception("Did not find the user for mail session jm1 defined in server.xml");
        }
    }

    /**
     * Verify a mail session is created from annotation. Tests every possible annotation field.
     */
    @Test
    public void testAnnotationJavamailSessionCreated() throws Throwable {

        if (jm2 != null) {
            Properties props = jm2.getProperties();
            System.out.println("JavamailFATServlet.testAnnotationJavamailSessionCreated properties : " + props.toString());

            // Validate we got the session we expected
            String userValue = jm2.getProperty("mail.user");
            if (!("jm2test").equals(userValue)) {
                throw new Exception("Did not find the user for mail session jm2 defined as an annotation");
            }
            String fromValue = jm2.getProperty("mail.from");
            if (!("jm2From").equals(fromValue)) {
                throw new Exception("Did not find the from value for mail session jm2 defined as an annotation");
            }
            String descValue = jm2.getProperty("description");
            if (!("jm2Desc").equals(descValue)) {
                throw new Exception("Did not find the description for mail session jm2 defined as an annotation");
            }
            String spValue = jm2.getProperty("mail.store.protocol");
            if (!("jm2StoreProtocol").equals(spValue)) {
                throw new Exception("Did not find the store.protocol for mail session jm2 defined as an annotation");
            }
            String tpValue = jm2.getProperty("mail.transport.protocol");
            if (!("jm2TransportProtocol").equals(tpValue)) {
                throw new Exception("Did not find the transport.protocol for mail session jm2 defined as an annotation");
            }
            //Vaidate the property "test" returns the value added with the annotation
            String testValue = jm2.getProperty("test");
            if (testValue == null || !testValue.equals("jm2Def_MailSession")) {
                throw new Exception("Did not find the test property for mail session mergeMS defined as an annotation, instead found: " + testValue);
            }
            return;
        }
        throw new Exception("Annotated jm2 MailSession was null");
    }

    /**
     * Verify a mail session is merged between a deployment descript and annotations.
     */
    @Test
    public void testMergedJavamailSessionCreated() throws Throwable {
        if (mergeMS != null) {
            Properties props = mergeMS.getProperties();
            System.out.println("JavamailFATServlet.testMergedJavamailSessionCreated properties : " + props.toString());

            // Validate we got the session we expected
            String userValue = mergeMS.getProperty("mail.user");
            if (!("mergeAnnotationUser").equals(userValue)) {
                throw new Exception("Did not find the user for mail session mergeMS defined as an annotation, instead found: " + userValue);
            }

            String descValue = mergeMS.getProperty("description");
            if (!("mergeDescription").equals(descValue)) {
                throw new Exception("Did not find the description for mail session mergeMS defined in web.xml, instead found: " + descValue);
            }

            String spValue = mergeMS.getProperty("mail.store.protocol");
            if (!("mergeStoreProtocol").equals(spValue)) {
                throw new Exception("Did not find the store-protocol for mail session mergeMS defined in web.xml, instead found: " + spValue);
            }

            String tpValue = mergeMS.getProperty("mail.transport.protocol");
            if (!("mergeTransportProtocol").equals(tpValue)) {
                throw new Exception("Did not find the transport-protocol for mail session mergeMS defined in web.xml, instead found: " + tpValue);
            }

            /*
             * Current bug in javamail implementation does not propagate these
             * see: https://javamail.java.net/nonav/docs/api/
             *
             * Syntax below for the properties are not quite right, but can correct
             * once MailSessionService is propagating these.
             */
            String spcValue = mergeMS.getProperty("mail." + spValue + ".class");
            if (!("mergeStoreProtocolClassName").equals(spcValue)) {
                throw new Exception("Did not find the store-protocol-class" + spValue + "for mail session mergeMS defined in web.xml, instead found: " + spcValue);
            }

            String tpcValue = mergeMS.getProperty("mail." + tpValue + ".class");
            if (!("mergeTransportProtocolClassName").equals(tpcValue)) {
                throw new Exception("Did not find the transport-protocol-class for mail session mergeMS defined in web.xml, instead found: " + tpcValue);
            }

            // This tests that the web.xml (deployment descriptor) overrides the annotation in the servlet
            String fromValue = mergeMS.getProperty("mail.from");
            if (!("mergeFrom").equals(fromValue)) {
                if (("mergeAnnotationFrom").equals(fromValue)) {
                    throw new Exception("Did not find the from value for mail session mergeMS defined in web.xml, instead got the annotation value from this servlet: "
                                        + fromValue);
                }
                throw new Exception("Did not find the from value for mail session mergeMS defined in web.xml, instead found: " + fromValue);
            }

            String hostValue = mergeMS.getProperty("mail.host");
            if (!("mergeHost").equals(hostValue)) {
                throw new Exception("Did not find the host for mail session mergeMS defined in web.xml, instead found: " + hostValue);
            }

            //Vaidate the property "test" returns the value added with the annotation
            String testValue = mergeMS.getProperty("test");
            if (testValue == null || !testValue.equals("mergeDef_MailSession")) {
                throw new Exception("Did not find the test property for mail session mergeMS defined as an annotation, instead found: " + testValue);
            }
            return;
        }
        throw new Exception("Annotated mergeMS MailSession was null");
    }

    /**
     * Verify a mail session is created for an EJB from an annotation.
     * TODO: Enable when EJB 4.0 is available
     */
    //@Test
    /*
     * public void testEjbJavamailSessionCreated() throws Throwable {
     * jtBean.testLookupJavamailAnnotation();
     * }
     */

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
    protected void before() throws Exception {}

    /**
     * Override to mimic JUnit's {@code @After} annotation.
     */
    protected void after() throws Exception {}

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
