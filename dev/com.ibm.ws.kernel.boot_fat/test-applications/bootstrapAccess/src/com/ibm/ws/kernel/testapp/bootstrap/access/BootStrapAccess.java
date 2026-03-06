/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.testapp.bootstrap.access;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class BootStrapAccess extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String classname = request.getParameter("classname");
        final String resourcename = request.getParameter("resourcename");
        final boolean expectFailure = Boolean.valueOf(request.getParameter("expectFailure"));

        if (classname != null) {
            response.getWriter().println("classname=" + classname + " expectFailure=" + expectFailure);
            try {
                Class.forName(classname);
                if (expectFailure) {
                    throw new ServletException("Did not expect to load class: " + classname);
                }
            } catch (ClassNotFoundException e) {
                if (!expectFailure) {
                    throw new ServletException("Expected to load class: " + classname, e);
                }
            }
        }

        if (resourcename != null) {
            response.getWriter().println("resourcename=" + resourcename + " expectFailure=" + expectFailure);
            URL resource = getClass().getResource(resourcename);
            if (resource != null) {
                if (expectFailure) {
                    throw new ServletException("Did not expect to get resource: " + resourcename);
                }
            } else {
                if (!expectFailure) {
                    throw new ServletException("Expected to find resource: " + resourcename);
                }
            }
        }
    }
}
