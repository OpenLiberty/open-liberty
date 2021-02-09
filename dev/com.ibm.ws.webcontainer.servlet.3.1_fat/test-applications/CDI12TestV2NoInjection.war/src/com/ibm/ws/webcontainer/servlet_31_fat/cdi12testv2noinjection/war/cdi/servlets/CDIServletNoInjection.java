/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2noinjection.war.cdi.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * CDI Tests: Verify that servlet, listener, and filter work with
 * CDI enabled but with no injections.
 */
@WebServlet(urlPatterns = { "/CDINoInjection" })
public class CDIServletNoInjection extends HttpServlet {
    //
    private static final long serialVersionUID = 1L;

    //

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        doPost(servletRequest, servletResponse); // throws ServletException, IOException
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        PrintWriter responseWriter = servletResponse.getWriter();
        responseWriter.println("Servlet Hello! No Injection");

        ServletContext requestContext = servletRequest.getServletContext();

        // Transfer any available injection results from an injected listener.
        String listenerResponse = (String) requestContext.getAttribute(CDIListenerNoInjection.LISTENER_DATA);
        if (listenerResponse != null) {
            responseWriter.print(listenerResponse);
        }
    }
}
