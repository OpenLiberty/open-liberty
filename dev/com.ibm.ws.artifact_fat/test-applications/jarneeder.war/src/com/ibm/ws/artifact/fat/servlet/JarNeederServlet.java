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
package com.ibm.ws.artifact.fat.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/JarNeederServlet")
public class JarNeederServlet extends HttpServlet {
    public static final String CLASS_NAME = "JarNeederServlet";
    
    private static final long serialVersionUID = 1L;

    //

    public static final String MARKER_RESOURCE_URL = "WEB-INF/classes/myresource.something";

    public JarNeederServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String methodName = "doGet";

        PrintWriter responseWriter = response.getWriter();
        responseWriter.println(CLASS_NAME + "." + methodName + ": ENTER");

        Enumeration<URL> resourceUrls =
            getClass().getClassLoader().getResources(MARKER_RESOURCE_URL);
        while ( resourceUrls.hasMoreElements() ) {
            URL resourceUrl = resourceUrls.nextElement();

            // 'FATResourceProtocol.validateResourceProtocol'
            // validates the response using these lines and requires that
            // their format be exactly as provided below.

            responseWriter.println("URL [ " + resourceUrl + " ]");
            responseWriter.println("Protocol [ " + resourceUrl.getProtocol() + " ]");
            responseWriter.println("Path [ " + resourceUrl.getPath() + " ]");
            responseWriter.println("Content [ " + resourceUrl.getContent() + " ]");
        }

        responseWriter.println(CLASS_NAME + "." + methodName + ": RETURN");
    }
}
