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
package com.ibm.ws.jaxws.test.jmx.client;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 * Servlet implementation class JMXClientServlet
 */
@WebServlet("/JMXClientServlet")
public class JMXClientServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        URL wsTestEndpointWSDL = new URL("http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/WSTestEndpointService?wsdl");
        Service.create(wsTestEndpointWSDL, new QName("http://jaxws.samples.ibm.com.jmx/", "WSTestEndpointService"));
        response.getWriter().write("This is JMXClientServlet");
    }
}
