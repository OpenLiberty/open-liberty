/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ExtendedMBeanServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904591351L;
    private final String className = "ExtendedMBeanServlet";

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
     * Test JCA Resource Adapter MBean to be sure it's not created when there is not enough data.
     */
    public void testResourceAdapterMBeanRegistrationWithNoModule(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testResourceAdapterMBeanRegistrationWithNoModule";
        System.out.println(className + ": " + methodName + " **  It's working");
        final String mBean_TYPE = "ResourceAdapterMBean";
        final String mBean_TYPE_J2EE = "ResourceAdapter";

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType=" + mBean_TYPE_J2EE + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        if (s.size() != 0) {
            // We found ResourceAdapterMBean when we shouldn't
            System.out.println("We should not have found any ResourceAdapterMBean, but we did");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
        }

    }

    /**
     * Test JCA Resource Adapter MBean to be sure it's not created when there is not enough data.
     */
    public void testResourceAdapterModuleMBeanRegistrationWithNoModule(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testResourceAdapterModuleMBeanRegistrationWithNoModule";
        System.out.println(className + ": " + methodName + " **  It's working");
        final String mBean_TYPE = "ResourceAdapterModuleMBean";
        final String mBean_TYPE_J2EE = "ResourceAdapterModule";

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType=" + mBean_TYPE_J2EE + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        if (s.size() != 0) {
            // We found ResourceAdapterModuleMBean when we shouldn't
            System.out.println("We should not have found any ResourceAdapterModuleMBean, but we did");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
        }

    }
}
