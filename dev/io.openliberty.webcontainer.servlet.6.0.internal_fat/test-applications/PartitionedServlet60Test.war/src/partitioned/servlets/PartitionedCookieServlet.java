/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package partitioned.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Creates cookies and adds attributes via Cookie#SetAttribute method.
 */
@WebServlet(urlPatterns = "/TestSetAttributePartitionedCookie")
public class PartitionedCookieServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            PrintWriter pw = resp.getWriter();
            pw.print("Welcome to the TestSetAttributePartitionedCookieServlet!");
            
            // No Attributes 
            Cookie plainCookie = new Cookie("Cookie_Plain_Name","AddCookie_Plain_Value");
            resp.addCookie(plainCookie);

            //Only SameSite=None
            Cookie sameSiteNoneOnlyCookie = new Cookie("AddCookie_SameSiteNoneOnly_Name","AddCookie_SameSiteNoneOnly_Value");
            sameSiteNoneOnlyCookie.setAttribute("SameSite", "None");
            resp.addCookie(sameSiteNoneOnlyCookie);

            //Sets both SameSite=None and Partitioned
            Cookie sameSiteNonepartitionedCookie = new Cookie("AddCookie_SameSiteNone_Name","AddCookie_SameSiteNone_Value");
            sameSiteNonepartitionedCookie.setAttribute("SameSite", "None");
            sameSiteNonepartitionedCookie.setAttribute("Partitioned", "");
            resp.addCookie(sameSiteNonepartitionedCookie);

            Cookie sameSiteLaxpartitionedCookie = new Cookie("AddCookie_SameSiteLax_Name","AddCookie_SameSiteLax_Value");
            sameSiteLaxpartitionedCookie.setAttribute("SameSite", "Lax");
            sameSiteLaxpartitionedCookie.setAttribute("Partitioned", ""); // will be ignored by server since partitioned doesn't apply to lax
            resp.addCookie(sameSiteLaxpartitionedCookie);

        }
    
}
