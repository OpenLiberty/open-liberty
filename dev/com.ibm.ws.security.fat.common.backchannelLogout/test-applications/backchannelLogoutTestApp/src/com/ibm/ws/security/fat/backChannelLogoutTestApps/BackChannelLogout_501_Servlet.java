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

public class BackChannelLogout_501_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public BackChannelLogout_501_Servlet() {
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

        System.out.println("BackChannelLogout_501_Servlet - returning status code of 501 ");
        writer.println("BackChannelLogout_501_Servlet - returning status code of 501 ");

        resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);

        writer.flush();
        writer.close();
    }

}
