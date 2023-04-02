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

public class BackChannelLogout_Sleep_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public BackChannelLogout_Sleep_Servlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(resp);
    }

    private void handleRequest(HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();

        System.out.println("BackChannelLogout_Sleep_Servlet - sleeping 10 seconds ");
        writer.println("BackChannelLogout_Sleep_Servlet - sleeping 10 seconds ");

        try {
            Thread.sleep(10 * 1000);
        } catch (Exception e) {
            throw new ServletException("Sleep failure: " + e);
        }

        writer.flush();
        writer.close();
    }

}
