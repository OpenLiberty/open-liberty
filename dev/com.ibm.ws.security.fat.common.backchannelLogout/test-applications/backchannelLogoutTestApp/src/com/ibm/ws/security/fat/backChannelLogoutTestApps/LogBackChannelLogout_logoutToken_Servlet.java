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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogBackChannelLogout_logoutToken_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final String logoutTokenParm = "logout_token";
    private String logoutToken = null;

    public LogBackChannelLogout_logoutToken_Servlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();

        String newLogoutToken = req.getParameter(logoutTokenParm);

        if (newLogoutToken != null) {
            logoutToken = newLogoutToken;
            System.out.println("Saving new logout_token.");
        } else {
            System.out.println("NO logout_token - we'll return the logout_token saved on the previous call which should have been from the back channel request.");
        }

        if (logoutToken == null) {
            System.out.println("LogBackChannelLogout_logoutToken_Servlet - logout_token: NOT SET");
            writer.println("logout_token: NOT SET");
        } else {
            System.out.println("LogBackChannelLogout_logoutToken_Servlet - logout_token: " + logoutToken);
            writer.println("logout_token: " + logoutToken);
        }

        writer.flush();
        writer.close();
    }

}
