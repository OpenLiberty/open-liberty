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
package testservlet40.servlets;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/SessionTimeoutServlet")
public class SessionTimeoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public SessionTimeoutServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        HttpSession session = null;
        if (request.getParameter("TestSessionTimeout") != null) {
            if (request.getParameter("TestSessionTimeout").equals("new")) {
                ServletContext context = request.getServletContext();
                sos.println("Session Timeout: " + context.getSessionTimeout());
                session = request.getSession();
                sos.println("Session object: " + session);
                sos.println("Now that session has been created, drive a request with request parameter set to TestSessionTimeout=current");
            } else if (request.getParameter("TestSessionTimeout").equals("current")) {
                session = request.getSession(false);
                sos.println("Session object: " + session);
                if (session == null || !request.isRequestedSessionIdValid()) {
                    sos.println("Session Invalidated");
                }
            } else {
                sos.println("Please add request parameter TestSessionTimeout. Example: TestSessionTimeout=new or TestSessionTimeout=current");
            }
        } else {
            sos.println("Please add request parameter TestSessionTimeout. Example: TestSessionTimeout=new");
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
