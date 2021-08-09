/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 *
 */
@WebServlet(urlPatterns = "/SessionCookieConfigTest")
public class SessionCookieConfigTest extends HttpServlet {

    private static final long serialVersionUID = 101L;

    /*
     * /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    @Override
    public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {

        HttpSession sess = ((HttpServletRequest) req).getSession(true);
        ServletContext sc = sess.getServletContext();

        SessionCookieConfig scc = sc.getSessionCookieConfig();

        PrintWriter pw = resp.getWriter();

        try {
            scc.setComment("Updated Comment");
            pw.println("SessionCookieConfigTest : setComment : Comment Updated");
        } catch (IllegalStateException exc) {
            pw.println("SessionCookieConfigTest : setComment : IllegalStateException");
        }

    }

}
