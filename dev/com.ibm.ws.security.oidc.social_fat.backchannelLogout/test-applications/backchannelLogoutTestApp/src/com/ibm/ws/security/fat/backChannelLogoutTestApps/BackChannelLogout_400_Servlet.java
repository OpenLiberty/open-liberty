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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BackChannelLogout_400_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public BackChannelLogout_400_Servlet() {
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

        System.out.println("BackChannelLogout_400_Servlet - returning status code of 400 ");
        writer.println("BackChannelLogout_400_Servlet - returning status code of 400 ");

        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        writer.flush();
        writer.close();
    }

}
