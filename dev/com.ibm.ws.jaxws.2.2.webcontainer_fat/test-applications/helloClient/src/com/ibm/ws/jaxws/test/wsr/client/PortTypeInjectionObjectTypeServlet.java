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
package com.ibm.ws.jaxws.test.wsr.client;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.fat.util.TestUtils;
import com.ibm.ws.jaxws.test.wsr.server.stub.People;
import com.ibm.ws.jaxws.test.wsr.server.stub.PeopleService;

@WebServlet("/PortTypeInjectionObjectTypeServlet")
public class PortTypeInjectionObjectTypeServlet extends HttpServlet {
    private static final long serialVersionUID = -1L;

    // test port type injection with specified type
    @WebServiceRef(name = "services/portTypeInjectionObjectType", type = People.class, value = PeopleService.class)
    Object bill;

    public PortTypeInjectionObjectTypeServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String target = request.getParameter("target");

        //workaround for the hard-coded server addr and port in wsdl
        TestUtils.setEndpointAddressProperty((BindingProvider) bill, request.getLocalAddr(), request.getLocalPort());
        out.println(((People) bill).hello(target));

    }
}