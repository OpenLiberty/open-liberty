/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.transport.http.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple Servlet class to get and print out in the response the Remote Address,
 * RemoteHost, RemotePort, Scheme and isSecure.
 */
@WebServlet("/EndpointInformationServlet")
public class EndpointInformationServlet extends HttpServlet {

    public EndpointInformationServlet() {

    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.addHeader("My-Response-Header", "resHeader1");
        res.addHeader("My-Response-Header", "resHeader2");

        res.addHeader("Set-Cookie", "JSESSIONID=gorp1");
        res.addHeader("Set-Cookie", "JSESSIONID=gorp2");

        PrintWriter pw = res.getWriter();

        pw.println("Endpoint Information Servlet Test");

        pw.println("Remote Address: " + req.getRemoteAddr());
        pw.println("Remote Host: " + req.getRemoteHost());
        pw.println("Remote Port: " + req.getRemotePort());
        pw.println("Scheme: " + req.getScheme());
        pw.println("isSecure: " + req.isSecure());

    }

}
