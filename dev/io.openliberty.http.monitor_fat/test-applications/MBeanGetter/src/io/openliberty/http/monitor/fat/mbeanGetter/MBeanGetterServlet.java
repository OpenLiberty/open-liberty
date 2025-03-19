/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package io.openliberty.http.monitor.fat.mbeanGetter;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used to test request timing mxbean
 */
@WebServlet(value = "/MBeanGetterServlet")
public class MBeanGetterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    //private static final String REQUESTMXBEAN = "WebSphere:type=RequestTimingStats,name=Servlet";
    public static MBeanServer mbeanConn = ManagementFactory.getPlatformMBeanServer();

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");

        String objectName = request.getParameter("objectname");

        if (objectName == null || objectName.isEmpty()) {
            System.out.println("Was not provided an objectName");
        } else {
            try {
                //returns/writes  boolean
                boolean isRegistered = mbeanConn.isRegistered(new ObjectName(objectName));

                //perhaps too slow. Give it some time/
                if (!isRegistered) {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        //don't really need to care for exception
                    }
                }

                StringBuilder sb = new StringBuilder();

                if (isRegistered) {
                    sb.append(isRegistered);
                } else { //lets print out the existing HTTP MBeans to help debug
                    Set<ObjectInstance> set;
                    set = mbeanConn.queryMBeans(new ObjectName("WebSphere:type=HttpServerStats,name=*"), null);
                    isRegistered = mbeanConn.isRegistered(new ObjectName(objectName));
                    //one more time.
                    if (isRegistered) {
                        sb.append(isRegistered);
                    } else {
                        set.stream().forEach(oj -> sb.append(oj + "\n"));
                    }

                }
                response.getWriter().println(sb.toString());

            } catch (MalformedObjectNameException | IOException e) {
                e.printStackTrace();
            }
        }

    }

}