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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *  Servlet creates a HttpSession to verify Partitoned attribute 
 *  is working correct on the session cookie
 */
@WebServlet(urlPatterns = "/TestPartitionedSession")
public class PartitionedSessionCreationServlet extends HttpServlet {
        /**  */
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            PrintWriter pw = resp.getWriter();
            pw.print("Welcome to the TestPartitionedSessionServlet!");

            // Trigger a session to be created. We should see a jsessionid cookie SET-COOKIE header
            // in the response.
            req.getSession(true);

        }
    
}
