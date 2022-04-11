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

public class BackChannelLogout_logMsg_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final String logoutTokenParm = "logout_token";
    int count = 0;

    public BackChannelLogout_logMsg_Servlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        System.out.println("Reset counter");
        count = 0;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter writer = resp.getWriter();

        count = count + 1;

        Map<String, String[]> parms = req.getParameterMap();
        String logoutToken = null;

        if (parms != null) {
            String[] logoutTokens = parms.get(logoutTokenParm); // test code only calling this, so we control what we're sending and it will always be a string
            if (logoutTokens != null && logoutTokens.length > 0) {
                logoutToken = logoutTokens[0];
            }
        }

        System.out.println("BackChannelLogout_logMsg_Servlet - " + count + " logout_token: " + logoutToken);
        writer.println("BackChannelLogout_logMsg_Servlet - " + count + " logout_token: " + logoutToken);

        writer.flush();
        writer.close();
    }

}
