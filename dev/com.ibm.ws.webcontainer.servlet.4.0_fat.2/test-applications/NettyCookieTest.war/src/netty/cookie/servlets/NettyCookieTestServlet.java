/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package netty.cookie.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */

@WebServlet(urlPatterns = "/NettyCookieTestServlet")
public class NettyCookieTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.println("Welcome to the NettyCookieTestServlet!");

        String test = request.getParameter("testname");

        Cookie[] cookies = request.getCookies();
        Cookie name1Cookie = null;
        for (Cookie cookie : cookies) {
            writer.println("Cookie: " + cookie.toString());
            if (cookie.getName().equals("name1")) {
                name1Cookie = cookie;
            }
        }

        int version = name1Cookie.getVersion();
        if (test != null) {
            if ("getVersionVer0Test".equals(test)) {

                writer.println("The value of the name1 Cookie is: " + name1Cookie.getValue());
                writer.println("The version of the name1 Cookie is: " + version);
                writer.println("The path of the name1 Cookie is: " + name1Cookie.getPath());
                writer.println("The domain of the name1 Cookie is: " + name1Cookie.getDomain());

                if (version == 0) {
                    writer.println("Test PASSED");
                } else {
                    writer.println("Test FAILED");
                }
            } else if ("getVersionVer1Test".equals(test)) {

                writer.println("The value of the name1 Cookie is: " + name1Cookie.getValue());
                writer.println("The version of the name1 Cookie is: " + version);
                writer.println("The path of the name1 Cookie is: " + name1Cookie.getPath());
                writer.println("The domain of the name1 Cookie is: " + name1Cookie.getDomain());

                if (version == 1) {
                    writer.println("Test PASSED");
                } else {
                    writer.println("Test FAILED");
                }
            } else if ("getPathTest".equals(test)) {
                writer.println("The value of the name1 Cookie is: " + name1Cookie.getValue());
                writer.println("The version of the name1 Cookie is: " + version);
                writer.println("The path of the name1 Cookie is: " + name1Cookie.getPath());
                writer.println("The domain of the name1 Cookie is: " + name1Cookie.getDomain());
                writer.println("The Request ContextPath is: " + request.getContextPath());

                String expected = request.getContextPath();
                String actual = name1Cookie.getPath();

                if (actual != null) {
                    if (!actual.equals(expected)) {
                        writer.println("Test FAILED");
                    } else {
                        writer.println("Test PASSED");
                    }
                } else {
                    writer.println("Test FAILED");
                }

            } else if ("getDomainTest".equals(test)) {
                writer.println("The value of the name1 Cookie is: " + name1Cookie.getValue());
                writer.println("The version of the name1 Cookie is: " + version);
                writer.println("The path of the name1 Cookie is: " + name1Cookie.getPath());
                writer.println("The domain of the name1 Cookie is: " + name1Cookie.getDomain());

                String host = request.getHeader("host");
                writer.println("The host header is: " + host);

                int col = host.indexOf(':');
                if (col > -1) {
                    host = host.substring(0, col).trim();
                }

                writer.println("The host header after manipulation is: " + host);

                String actual = name1Cookie.getDomain();

                if (actual != null) {
                    if (!actual.equalsIgnoreCase(host)) {
                        writer.println("Test FAILED");
                    } else {
                        writer.println("Test PASSED");
                    }
                } else {
                    writer.println("Test FAILED");
                }
            }

        }
    }
}
