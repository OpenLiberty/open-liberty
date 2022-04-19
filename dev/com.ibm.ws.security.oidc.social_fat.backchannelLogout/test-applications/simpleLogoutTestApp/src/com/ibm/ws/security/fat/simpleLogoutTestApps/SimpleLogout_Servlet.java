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
package com.ibm.ws.security.fat.simpleLogoutTestApps;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleLogout_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    PrintWriter writer = null;
    private String servletName = "SimpleLogout_Servlet";

    public SimpleLogout_Servlet() {
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

        PrintWriter writer = resp.getWriter();
        StringBuffer sb = new StringBuffer();

        writeLine(sb, "ServletName: " + servletName);

        Cookie[] cookies = req.getCookies();
        for (Cookie cookie : cookies) {
            writeLine(sb, "cookie BEFORE logout: name: " + cookie.getName() + " value: " + cookie.getValue());
        }

        try {
            System.out.println("Test application class " + servletName + " is logging out");
            req.logout();
            writeLine(sb, "Test Application class \" + servletName + \" logged out\n");
        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

        cookies = req.getCookies();
        for (Cookie cookie : cookies) {
            writeLine(sb, "cookie AFTER logout: name: " + cookie.getName() + " value: " + cookie.getValue());
        }
        writer.write(sb.toString());
        writer.flush();
        writer.close();

    }

    protected void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + System.getProperty("line.separator"));
    }

}
