/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.servlet31.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.ws.fat.wc.servlet31.listeners.MySessionIdListener;

@WebServlet("/SessionIdListenerChangeServlet")
public class SessionIdListenerChangeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public SessionIdListenerChangeServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        if (request.getParameter("getSessionFirst") == null) {
            HttpSession session = request.getSession(false);
            if (session == null) {
                sos.println("Original session is null");
                try {
                    String sessionId = request.changeSessionId();
                    sos.println("INCORRECT RETURN VALUE: sessionId is " + sessionId);
                } catch (IllegalStateException e) {
                    if (e.getMessage().trim().equals("SRVE9014E: An attempt to change the session ID failed because no session " +
                                                     "was associated with the request for: " + request.getRequestURI())) {

                        sos.println("Expected IllegalStateException: " + e.getMessage());
                    }
                }
            } else {
                String sessionId = session.getId();
                sos.println("Original session id = <sessionid>" + sessionId + "</sessionid>");
                sos.println("Change count = " + session.getAttribute(MySessionIdListener.attributeName));
            }
        } else {
            HttpSession session = request.getSession();
            String sessionId = session.getId();
            sos.println("Original session id = <sessionid>" + sessionId + "</sessionid>");
            sessionId = request.changeSessionId();
            sos.println("Session id returned from changeSessionId = <sessionid>" + sessionId + "</sessionid>");
            HttpSession newSession = request.getSession();
            sessionId = newSession.getId();
            sos.println("Session id from getSession (again) = <sessionid>" + sessionId + "</sessionid>");
            sos.println("Change count = " + session.getAttribute(MySessionIdListener.attributeName));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
