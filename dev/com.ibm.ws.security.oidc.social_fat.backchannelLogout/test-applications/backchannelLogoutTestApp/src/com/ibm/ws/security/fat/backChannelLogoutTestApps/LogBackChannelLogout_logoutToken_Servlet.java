/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.backChannelLogoutTestApps;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogBackChannelLogout_logoutToken_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    //private final String servletName = "BackChannelLogoutServlet";
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

        Map<String, String[]> parms = req.getParameterMap();
        if (parms != null) {
            String[] logoutTokens = parms.get(logoutTokenParm); // test code only calling this, so we control what we're sending and it will always be a string
            if (logoutTokens != null && logoutTokens.length > 0) {
                logoutToken = logoutTokens[0];
                System.out.println("Saving new logout_token.");
            } else {
                System.out.println("NO logout_token - we'll return the logout_token saved on the previous call which should have been from the back channel request.");
            }
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
