/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package incompatible.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Sends out SameSite=None cookies
 * These cookies should not make it back to the test as part of the response unless a compatible client is used
 */

@WebServlet(urlPatterns = "/IncompatibleClientServlet")
public class IncompatibleClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.print("Welcome to the IncompatibleClientServlet!");

        // Session cookie is configured to have SameSite=None via server.xml, calling getSession just creates the cookie
        request.getSession();
        response.setHeader("set-cookie", "SetHeaderCookie; SameSite=None");
        response.addHeader("set-cookie", "AddHeaderCookie; SameSite=None");
        response.addCookie(new Cookie("AddCookieCookie", "sugar"));

        // Have a base cookie that always gets sent and does not have SameSite set
        response.addHeader("set-cookie", "BasicCookie");
    }
}
