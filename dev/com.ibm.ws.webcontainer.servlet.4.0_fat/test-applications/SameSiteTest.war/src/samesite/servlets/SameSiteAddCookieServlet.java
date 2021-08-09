/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package samesite.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This Servlet will add a number of Cookies to the HttpServletResponse depending on the value of the
 * cookieToAdd parameter. The server configuration will define a SameSite value for each of them and
 * the tests will ensure that each of the Cookies has the proper SameSite attribute added to the
 * Set-Cookie header.
 */
@WebServlet(urlPatterns = "/SameSiteAddCookieServlet")
public class SameSiteAddCookieServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        pw.print("Welcome to the SameSiteAddCookieServlet! ");

        String cookieToAdd = req.getParameter("cookieToAdd");

        // Configure server to make this Cookie sameSite=Lax
        if (cookieToAdd != null) {
            if (cookieToAdd.equals("add_one_cookie")) {
                Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
                resp.addCookie(cookieOne);
                pw.print("Adding cookieOne!");
            } else if (cookieToAdd.equals("add_one_cookie_secure")) {
                // Configure server to make this Cookie SameSite=None , cookie already has secure flag set
                Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
                cookieOne.setSecure(true);
                resp.addCookie(cookieOne);
                pw.print("Adding cookieOne with Secure!");
            } else if (cookieToAdd.equals("add_two_cookies")) {
                Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
                Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
                resp.addCookie(cookieOne);
                resp.addCookie(cookieTwo);
                pw.print("Adding two Cookies!");
            } else if (cookieToAdd.equals("add_two_cookies_secure")) {
                Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
                cookieOne.setSecure(true);
                Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
                cookieTwo.setSecure(true);
                resp.addCookie(cookieOne);
                resp.addCookie(cookieTwo);
                pw.print("Adding two Cookies with Secure!");
            } else if (cookieToAdd.equals("add_same_cookie_twice")) {
                Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
                Cookie cookieTwo = new Cookie("cookieOne", "cookieOne");
                resp.addCookie(cookieOne);
                resp.addCookie(cookieTwo);
                pw.print("Adding two Cookies with the same name!");
            } else if (cookieToAdd.equals("add_two_cookies_different_case")) {
                Cookie cookieone = new Cookie("cookieone", "cookieone");
                Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
                resp.addCookie(cookieone);
                resp.addCookie(cookieOne);
                pw.print("Adding two Cookies with the same name but different case!");
            }
        }
    }

}
