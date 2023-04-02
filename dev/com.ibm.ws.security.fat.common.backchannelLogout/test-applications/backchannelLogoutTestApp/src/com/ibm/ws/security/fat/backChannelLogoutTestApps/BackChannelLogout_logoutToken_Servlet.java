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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.fat.backChannelLogoutTestApps.utils.BackChannelLogout_utils;

public class BackChannelLogout_logoutToken_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final String logoutTokenParm = "logout_token";
    private BackChannelLogout_utils bclUtils = new BackChannelLogout_utils();
    String builtOutput = "";
    int counter = 0;
    PrintWriter writer = null;
    private final Lock lock = new ReentrantLock(true);

    public BackChannelLogout_logoutToken_Servlet() {
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Put - Reset logout_token map");
        builtOutput = "";
        counter = 0;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Get");
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Post");
        handleRequest(req, resp);
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        lock.lock();
        try {
            writer = resp.getWriter();

            String app = bclUtils.getAppName(req);
            String client = bclUtils.getClientNameFromAppName(req);

            String newLogoutToken = req.getParameter(logoutTokenParm);

            if (newLogoutToken != null) {
                counter = counter + 1;
                System.out.println("Saving logout_token" + newLogoutToken);
                builtOutput = builtOutput + "BackChannelLogout_logoutToken_Servlet:" + client + " - logout_token: " + newLogoutToken + System.getProperty("line.separator");
                //                tokenMap.put(client, logoutTokens[0]);
                //                System.out.println("Saving new logout_token for client: " + client);
            } else {
                System.out.println("NO logout_token - we'll return the logout_token saved on the previous call which should have been from the back channel request.");

            }
            if (app.endsWith("_postLogout")) {
                String splitterLine = "**********************************************************************";
                System.out.println(splitterLine);
                System.out.println("Number of logout tokens: " + counter);
                writer.println("Number of logout tokens: " + counter);
                System.out.println(builtOutput);
                writer.println(builtOutput);
                System.out.println(splitterLine);
            }

        } catch (Exception e) {
            System.out.println("Post exception: " + e.getMessage());
        } finally {
            System.out.flush();
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            lock.unlock();
        }
    }

}
