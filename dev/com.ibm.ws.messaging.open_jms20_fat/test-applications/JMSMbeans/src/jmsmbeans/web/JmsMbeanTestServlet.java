/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jmsmbeans.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JmsMbeanTestServlet extends HttpServlet {
    public static final String JMXMessage = "This is MessagingMBeanServlet";

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        final TraceComponent tc = Tr.register(JmsMbeanTestServlet.class);
        Tr.entry(this, tc, test);
        try {
            System.out.println(" Starting : " + test);
            getClass().getMethod(test, HttpServletRequest.class,
                                 HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" Ending : " + test);
            Tr.exit(this, tc, test);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            Tr.exit(this, tc, test, x);
            out.println("<pre>ERROR in " + test + ":");
            System.out.println(" Ending : " + test);
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    public void testJmsProviderMbean(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        try {

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            //Construct ObjectName for JMSProvider.
            ObjectName queryObject = new ObjectName("WebSphere:j2eeType=JMSResource,name=JMS-2.0Provider,J2EEServer=TestServer");

            //Query with MBean server for JMSProivder objectName
            Set<ObjectName> ons = mbs.queryNames(queryObject, null);

            System.out.println("with Proper name returnObjectName  :" + ons.toString());

            if (!ons.contains(queryObject)) {
                //the return set does not have Provider MBean Object.. then throw the error.
                throw new WrongException("JMSProvider MBean could not be retrieved");
            }

        } catch (Error e) {
            throw new WrongException(e.toString());
        }

    }

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

}
