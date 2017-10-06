/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Set;
import java.util.Properties;

import javax.mail.MailSessionDefinition;
import javax.mail.MailSessionDefinitions;
import javax.mail.Session;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.annotation.Resource;
import javax.ejb.EJB;
import fvtweb.ejb.JavamailTestLocal;

@MailSessionDefinitions({
@MailSessionDefinition(name="javamail/jm2Def",
                       from="jm2From",
                       description="jm2Desc",
                       storeProtocol="jm2StoreProtocol",
                       transportProtocol="jm2TransportProtocol",
                       user="jm2test",
                       password="testJm2test"),
@MailSessionDefinition(name="javamail/mergeDef",
                       user="mergeAnnotationUser",
                       from="mergeAnnotationFrom",
                       password="mergePass")})
public class JavamailFATServlet extends HttpServlet {
    

    @Resource(name="javamail/jm2",
              lookup="java:comp/env/javamail/jm2Def")
    private Session jm2;

    @Resource(name="javamail/mergeMS",
                    lookup="java:comp/env/javamail/mergeDef")
          private Session mergeMS;

    @EJB
    JavamailTestLocal jtBean;

    private static final long serialVersionUID = 7709282314904580334L;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        System.out.println("-----> " + test + " starting");
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
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
     * Verify a mail session is created from deployment descriptor config. Only tests 1 descriptor field (the merge test
     * covers the rest).
     */
    public void testDDJavamailSessionCreated(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        
        Session jm1 = (Session) new InitialContext().lookup("java:comp/env/javamail/jm1Def");
        Properties props = jm1.getProperties();
        System.out.println("JavamailFATServlet.testDDJavamailSessionCreated properties : " + props.toString());
        
        // Validate we got the session we expected
        String userValue = (String) jm1.getProperty("mail.user");
        if (("jm1test").equals(userValue)) {
            // Success!
        } else {
            throw new Exception("Did not find the user for mail session jm1 defined in server.xml");
        }
    }

    /**
     * Verify a mail session is created from annotation.  Tests every possible annotation field.
     */
    public void testAnnotationJavamailSessionCreated(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        
        if (jm2 != null) {
            Properties props = jm2.getProperties();
            System.out.println("JavamailFATServlet.testAnnotationJavamailSessionCreated properties : " + props.toString());
            
            // Validate we got the session we expected
            String userValue = (String) jm2.getProperty("mail.user");
            if (!("jm2test").equals(userValue)) {
                throw new Exception("Did not find the user for mail session jm2 defined as an annotation");
            }
            String fromValue = (String) jm2.getProperty("mail.from");
            if (!("jm2From").equals(fromValue)) {
                throw new Exception("Did not find the from value for mail session jm2 defined as an annotation");
            }
            String descValue = (String) jm2.getProperty("description");
            if (!("jm2Desc").equals(descValue)) {
                throw new Exception("Did not find the description for mail session jm2 defined as an annotation");
            }
            String spValue = (String) jm2.getProperty("mail.store.protocol");
            if (!("jm2StoreProtocol").equals(spValue)) {
                throw new Exception("Did not find the store.protocol for mail session jm2 defined as an annotation");
            }
            String tpValue = (String) jm2.getProperty("mail.transport.protocol");
            if (!("jm2TransportProtocol").equals(tpValue)) {
                throw new Exception("Did not find the transport.protocol for mail session jm2 defined as an annotation");
            }

            return;
        }
        throw new Exception("Annotated jm2 MailSession was null");
    }

    /**
     * Verify a mail session is merged between a deployment descript and annotations.
     */
    public void testMergedJavamailSessionCreated(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        
        if (mergeMS != null) {
            Properties props = mergeMS.getProperties();
            System.out.println("JavamailFATServlet.testMergedJavamailSessionCreated properties : " + props.toString());
            
            // Validate we got the session we expected
            String userValue = (String) mergeMS.getProperty("mail.user");
            if (!("mergeAnnotationUser").equals(userValue)) {
                throw new Exception("Did not find the user for mail session mergeMS defined as an annotation, instead found: " + userValue);
            }
            
            String descValue = (String) mergeMS.getProperty("description");
            if (!("mergeDescription").equals(descValue)) {
                throw new Exception("Did not find the description for mail session mergeMS defined in web.xml, instead found: " + descValue);
            }
            
            String spValue = (String) mergeMS.getProperty("mail.store.protocol");
            if (!("mergeStoreProtocol").equals(spValue)) {
                throw new Exception("Did not find the store-protocol for mail session mergeMS defined in web.xml, instead found: " + spValue);
            }
            
            String tpValue = (String) mergeMS.getProperty("mail.transport.protocol");
            if (!("mergeTransportProtocol").equals(tpValue)) {
                throw new Exception("Did not find the transport-protocol for mail session mergeMS defined in web.xml, instead found: " + tpValue);
            }

            /*
             * Current bug in javamail implementation does not propagate these
             * see:  https://javamail.java.net/nonav/docs/api/
             * 
             * Syntax below for the properties are not quite right, but can correct
             * once MailSessionService is propagating these.
             */
            String spcValue = (String) mergeMS.getProperty("mail." + spValue + ".class");
            if (!("mergeStoreProtocolClassName").equals(spcValue)) {
                throw new Exception("Did not find the store-protocol-class" + spValue + "for mail session mergeMS defined in web.xml, instead found: " + spcValue);
            }
            
            String tpcValue = (String) mergeMS.getProperty("mail." + tpValue + ".class");
            if (!("mergeTransportProtocolClassName").equals(tpcValue)) {
                throw new Exception("Did not find the transport-protocol-class for mail session mergeMS defined in web.xml, instead found: " + tpcValue);
            }
            
            
            // This tests that the web.xml (deployment descriptor) overrides the annotation in the servlet
            String fromValue = (String) mergeMS.getProperty("mail.from");
            if (!("mergeFrom").equals(fromValue)) {
                if(("mergeAnnotationFrom").equals(fromValue)) {
                    throw new Exception("Did not find the from value for mail session mergeMS defined in web.xml, instead got the annotation value from this servlet: " + fromValue);
                }
                throw new Exception("Did not find the from value for mail session mergeMS defined in web.xml, instead found: " + fromValue);
            }
            
            String hostValue = (String) mergeMS.getProperty("mail.host");
            if (!("mergeHost").equals(hostValue)) {
                throw new Exception("Did not find the host for mail session mergeMS defined in web.xml, instead found: " + hostValue);
            }
            
            return;
        }
        throw new Exception("Annotated mergeMS MailSession was null");
    }

    /**
     * Verify a mail session is created for an EJB from an annotation.
     */
    public void testEjbJavamailSessionCreated(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        jtBean.testLookupJavamailAnnotation();
    }
}
