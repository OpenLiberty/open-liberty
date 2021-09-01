/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.JAXRSClientStandalone.service;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServiceServlet extends HttpServlet {

    private static final long serialVersionUID = 8688034399080432350L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        System.out.println("doGet - path = " + path);

        //Not setting the content-type header exposes an issue in the EE9 client
        // Intentionally not setting to ensure client can handle response with no c-t header
        //resp.setHeader("Content-type", "text/plain");

        PrintWriter pw = resp.getWriter();
        if (null != path && path.equals("/BasicResource/echo/alex")) {
            pw.write("[Basic Resource]:alex");
        }
        else {
            pw.write("You should not see me");
        }

        pw.flush();
        pw.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
