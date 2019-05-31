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
package com.ibm.ws.test.stubclient.client;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

@WebServlet("/SimpleStubClientServlet")
public class SimpleStubClientServlet extends HttpServlet {

    private static final long serialVersionUID = 4838332634689830661L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int port = req.getLocalPort();
        String host = req.getLocalAddr();
        String wsdlLocation = "http://" + host + ":" + port + "/stubClient/SimpleEchoService?wsdl";
        SimpleEchoService simpleEchoService = new SimpleEchoService(new URL(wsdlLocation), new QName("http://stubclient.test.ws.ibm.com/", "SimpleEchoService"));
        SimpleEcho simpleEcho = simpleEchoService.getSimpleEchoPort();
        String response = simpleEcho.echo("echo");
        if (response != null && response.equals("echo")) {
            resp.getWriter().write("Pass");
        } else {
            resp.getWriter().write("Fail");
        }
    }
}