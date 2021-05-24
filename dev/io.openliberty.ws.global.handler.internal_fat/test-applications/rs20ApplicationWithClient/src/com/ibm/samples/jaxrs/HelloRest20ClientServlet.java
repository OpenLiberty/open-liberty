/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.samples.jaxrs;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

@WebServlet("/HelloRest20ClientServlet")
public class HelloRest20ClientServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        String port = req.getParameter("port");
        if (port == null) {
            pw.write("Can not get port");
            return;
        }
        StringBuilder sb = new StringBuilder("http://localhost:").append(port).append("/rs20ApplicationWithClient/hello");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target(sb.toString());
        Response response = t.request().get();
        String result = response.readEntity(String.class);
        pw.write(result);
    }
}
