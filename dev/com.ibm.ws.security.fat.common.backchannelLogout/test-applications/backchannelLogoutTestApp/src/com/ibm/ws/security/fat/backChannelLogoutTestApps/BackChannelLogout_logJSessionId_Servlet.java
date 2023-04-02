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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.backChannelLogoutTestApps;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BackChannelLogout_logJSessionId_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public BackChannelLogout_logJSessionId_Servlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //        System.out.println("BackChannelLogout_logJSessionId_Servlet: GET");

        doWorker(req, resp);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //        System.out.println("BackChannelLogout_logJSessionId_Servlet: POST");

        doWorker(req, resp);

    }

    protected void doWorker(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter writer = resp.getWriter();

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            boolean found = false;
            for (Cookie cookie : cookies) {
                //                System.out.println("BackChannelLogout_logJSessionId_Servlet: ALL: cookie: " + cookie.getName() + " value: " + cookie.getValue());
                if (cookie.getName().contains("JSESSIONID")) {
                    found = true;
                    writer.println("BackChannelLogout_logJSessionId_Servlet: cookie: " + cookie.getName() + " value: " + cookie.getValue());
                    System.out.println("BackChannelLogout_logJSessionId_Servlet: cookie: " + cookie.getName() + " value: " + cookie.getValue());
                }
            }
            if (!found) {
                writer.println("BackChannelLogout_logJSessionId_Servlet: JSESSIONID NOT found");
                System.out.println("BackChannelLogout_logJSessionId_Servlet: JSESSIONID NOT found");
            }
        }

        writer.flush();
        writer.close();
    }

}
