/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package headers.servlets;

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

@WebServlet(urlPatterns = "/ResponseHeadersServlet")
public class ResponseHeadersServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.print("Welcome to the ResponseHeadersServlet!");

        response.addHeader("appVerificationHeader", "");

        String testCondition = request.getParameter("testCondition");

        if (testCondition != null) {
            if ("singleHeader".equals(testCondition)) {
                response.addHeader("customHeader", "appValue");
            } else if ("multipleHeaders".equals(testCondition)) {
                response.addHeader("customHeader", "appValue");
                response.addHeader("customHeader", "appValue2");
            } else if ("testCookies".equals(testCondition)) {
                response.addHeader("set-cookie", "chocolate=chip; SameSite=None");
                response.addCookie(new Cookie("vanilla", "sugar"));
            }
        }
    }
}
