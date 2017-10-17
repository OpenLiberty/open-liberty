/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testservlet40.war.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/CookieFilter")
public class CookieFilter extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public CookieFilter() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();

        // Create multiple cookies
        String cookieName = "test";
        Cookie cookie1 = new Cookie(cookieName, "abc");
        cookie1.setPath("/TestServlet40");
        // The second cookie will overwrite the first cookie since we are using the same options
        Cookie cookie2 = new Cookie(cookieName, "def");
        cookie2.setPath("/TestServlet40");
        Cookie cookie3 = new Cookie(cookieName, "hij");
        cookie3.setPath("/TestServlet40/CookieFilter");
        Cookie cookie4 = new Cookie(cookieName, "klm");
        cookie4.setPath("/");

        // Add the cookies to the response
        response.addCookie(cookie1);
        response.addCookie(cookie2);
        response.addCookie(new Cookie("hello", "John"));
        response.addCookie(cookie3);
        response.addCookie(new Cookie("awesome", "thing"));
        response.addCookie(new Cookie("bye", "Ally"));
        response.addCookie(cookie4);

        // Get all the cookies
        sos.println("All the cookies");
        Cookie[] allCookies = request.getCookies();
        if (allCookies != null) {
            for (int i = 0; i < allCookies.length; i++) {
                sos.println("Cookie:" + " name: " + allCookies[i].getName() + "; value: " + allCookies[i].getValue());
            }
        }

        // Get all the cookies for a given name
        String query = request.getParameter("cookieName");
        if (query != null) {
            sos.println("Filtering cookies with name: " + query);
            Cookie[] filteredCookies = request.getCookies(query);
            if (filteredCookies != null) {
                for (int i = 0; i < filteredCookies.length; i++) {
                    sos.println("Cookie Filter:" + " name: " + filteredCookies[i].getName() + "; value: " + filteredCookies[i].getValue());
                }
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
