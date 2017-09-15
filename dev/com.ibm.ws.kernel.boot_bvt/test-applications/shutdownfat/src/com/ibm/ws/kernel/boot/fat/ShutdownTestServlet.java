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
package com.ibm.ws.kernel.boot.fat;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
@SuppressWarnings("serial")
public class ShutdownTestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String exitMethodName = request.getParameter("exit");
        response.getWriter().println("exit=" + exitMethodName);

        // WebContainer "helpfully" blocks app stop until all servlets have
        // finished their requests.  That only makes the FAT take longer, so use
        // a secondary thread.
        new Thread() {
            @Override
            public void run() {
                if ("Runtime.exit".equals(exitMethodName)) {
                    System.out.println(this + ": calling Runtime.exit");
                    Runtime.getRuntime().exit(0);
                } else {
                    System.out.println(this + ": calling Shutdown.exit");
                    System.exit(0);
                }
            }
        }.start();
    }
}
