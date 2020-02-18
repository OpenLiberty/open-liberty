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
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This Servlet is invoked with the same request as the SameSiteAddCookieSetCookieHeaderFilter.
 *
 * The SameSiteAddCookieSetCookieHeaderFilter will add a Cookie to the HttpServletResponse.
 *
 * Call the setHeader and addHeader methods on the HttpServletResponse for the Set-Cookie header.
 *
 */
@WebServlet(urlPatterns = "/TestAddCookieSetCookieHeader")
public class SameSiteAddCookieSetCookieHeaderServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        pw.println("Welcome to the SameSiteAddCookieSetCookieHeaderServlet!");
        pw.println("Set-Cookie headers:");
        ArrayList<String> headers = new ArrayList<String>(resp.getHeaders("Set-Cookie"));

        for (String header : headers) {
            pw.println(header);
        }
    }

}
