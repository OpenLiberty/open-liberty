/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.client.echoapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

@WebServlet("/ClientConfigTestServlet")
public class ClientConfigTestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClientConfigTestServlet() {
        super();
    }

    /**
     * new up a jax-rs client. We don't care that the request can't be invoked, what we are
     * interested in is listing out the configuration properties. The client config test will examine these
     * properties and assess if they are consistent with the WebTargets declared in server.xml
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        OutputStream os = response.getOutputStream();
        PrintStream ps = new PrintStream(os);
        String url = request.getParameter("url");
        if (url == null)
            url = "http://localhost:56789";
        try {
            ps.println("creating new client for " + url);
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(url).path("resource");
            ps.println("--- for resource------");
            ps.println("Webtarget: " + target);
            Map config = target.getConfiguration().getProperties();
            java.util.Iterator<String> it = config.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                ps.println("url=resource key:=" + key + " value=" + config.get(key));
            }

            WebTarget nextTarget = target.path("/foo");
            config = nextTarget.getConfiguration().getProperties();
            it = config.keySet().iterator();
            ps.println("--- for resource/foo,------");
            while (it.hasNext()) {
                String key = it.next();
                ps.println("url=resource/foo key=" + key + " value=" + config.get(key));
            }

            // there's no point in submitting since we have no service on the other end.
            //Response resp = nextTarget.request("text/html").get();

            //ps.println("request submitted, status = " + resp.getStatus());
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream baps = new PrintStream(baos);

            ps.println("Caught exception: " + e);
            // add some html so the stacktrace is readable.
            e.printStackTrace(baps);
            String buf = baos.toString();
            buf = buf.replace("at ", "<br>at ");
            ps.println("<br>stack trace: <br>");
            ps.println(buf);
        }

    }
}
