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
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.fat.backChannelLogoutTestApps.utils.BackChannelLogout_utils;

public class BackChannelLogout_logMsg_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final String logoutTokenParm = "logout_token";
    int count = 0;
    private final Lock lock = new ReentrantLock(true);
    private BackChannelLogout_utils bclUtils = new BackChannelLogout_utils();
    PrintWriter writer = null;

    public BackChannelLogout_logMsg_Servlet() {
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        System.out.println("Reset counter");
        count = 0;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        System.out.println("BackChannelLogout_logMsg_Servlet called number of times: " + count);
        PrintWriter writer = resp.getWriter();
        writer.println("BackChannelLogout_logMsg_Servlet called number of times: " + count);

        writer.flush();
        writer.close();

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        lock.lock();
        try {
            writer = resp.getWriter();

            String client = bclUtils.getClientNameFromAppName(req);
            count = count + 1;

            Map<String, String[]> parms = req.getParameterMap();
            String logoutToken = null;

            if (parms != null) {
                String[] logoutTokens = parms.get(logoutTokenParm); // test code only calling this, so we control what we're sending and it will always be a string
                if (logoutTokens != null && logoutTokens.length > 0) {
                    logoutToken = logoutTokens[0];
                }
            }

            System.out.println("BackChannelLogout_logMsg_Servlet: " + client + " - " + count + " logout_token: " + logoutToken);
            writer.println("BackChannelLogout_logMsg_Servlet: " + client + " - " + count + " logout_token: " + logoutToken);

        } catch (Exception e) {
            System.out.println("Post exception: " + e.getMessage());
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            lock.unlock();
        }
    }

}
