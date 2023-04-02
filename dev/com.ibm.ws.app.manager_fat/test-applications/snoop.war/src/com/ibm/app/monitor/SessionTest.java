/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.app.monitor;

import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;

public class SessionTest extends HttpServlet {
    /**  */
    private static final long serialVersionUID = 1L;

    public SessionTest() {
        super();
    }

    @Override
    protected void doGet(javax.servlet.http.HttpServletRequest request,
                         javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, java.io.IOException {
        PrintWriter pw = response.getWriter();
        pw.print("Creating a Session");
        request.getSession();
        pw.print("Session created");
    }

    @Override
    protected void doPost(javax.servlet.http.HttpServletRequest request,
                          javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, java.io.IOException {
        // This was a no-op in the existing SessionTest.
        // Probably shouldn't be.
        PrintWriter pw = response.getWriter();
        pw.print("Post: Creating a Session");
        request.getSession();
        pw.print("Session created");
    }
}
