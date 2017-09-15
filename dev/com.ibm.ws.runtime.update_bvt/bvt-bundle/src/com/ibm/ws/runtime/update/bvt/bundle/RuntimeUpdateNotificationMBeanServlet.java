/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.update.bvt.bundle;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RuntimeUpdateNotificationMBeanServlet extends HttpServlet {

    private static final long serialVersionUID = -1932653930044734282L;
    
    /*
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(200);
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter pw = resp.getWriter();
        testRuntimeUpdateNotificationMBean(req, pw);
        pw.flush();
        pw.close();
    }
    
    private void testRuntimeUpdateNotificationMBean(HttpServletRequest req, PrintWriter pw) {
        try {
            final ObjectName name = new ObjectName("WebSphere:name=com.ibm.websphere.runtime.update.RuntimeUpdateNotificationMBean");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            if (mbs.isRegistered(name)) {
                boolean supportsUpdateNotifications = false;
                MBeanInfo info = mbs.getMBeanInfo(name);
                OUTER : for (MBeanNotificationInfo notInfo : info.getNotifications()) {
                    if (Notification.class.getName().equals(notInfo.getName())) {
                        for (String type : notInfo.getNotifTypes()) {
                            if ("com.ibm.websphere.runtime.update.notification".equals(type)) {
                                supportsUpdateNotifications = true;
                                break OUTER;
                            }
                        }
                    }
                }
                if (supportsUpdateNotifications) {
                    pw.println("Runtime update notifications supported");
                }
                else {
                    pw.println("Registered but does not support runtime update notifications");
                }
            }
            else {
                pw.println("Not registered with the MBeanServer");
            }
        }
        catch (Exception e) {
            pw.println("Unexpected Exception during processing: " + e.getMessage());
            e.printStackTrace(pw);
        }
    }
}
