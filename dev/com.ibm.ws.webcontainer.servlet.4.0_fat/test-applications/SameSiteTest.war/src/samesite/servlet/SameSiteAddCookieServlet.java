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
package samesite.servlet;

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
        pw.println("Welcome to the SameSiteAddCookieServlet!");

        String cookieToAdd = req.getParameter("cookieToAdd");

        // Configure server to make this Cookie sameSite=Lax
        if (cookieToAdd.contentEquals("cookieOne")) {
            Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
            resp.addCookie(cookieOne);
            pw.println("Adding cookieOne");
        } else if (cookieToAdd.contentEquals("cookieTwo")) {
            // Configure server to make this Cookie SameSite=Strict
            Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
            resp.addCookie(cookieTwo);
            pw.println("Adding cookieTwo");
        } else if (cookieToAdd.contentEquals("cookieThree")) {
            // Configure server to make this Cookie SameSite=None , cookie already has secure flag set
            Cookie cookieThree = new Cookie("cookieThree", "cookieThree");
            cookieThree.setSecure(true);
            resp.addCookie(cookieThree);
            pw.println("Adding cookieThree");
        } else if (cookieToAdd.contentEquals("cookieFour")) {
            // Configure server to make this Cookie SameSite=None, cookie should have secure flag added
            Cookie cookieFour = new Cookie("cookieFour", "cookieFour");
            resp.addCookie(cookieFour);
            pw.println("Adding cookieFour");
        }
    }

}
