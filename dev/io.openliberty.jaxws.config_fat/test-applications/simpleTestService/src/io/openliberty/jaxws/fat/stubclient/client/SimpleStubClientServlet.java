/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.fat.stubclient.client;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;

/*
 * SimpleStubClientServlet is the endpoint that invokes the Web Service by
 * using a "serviceRef" http request parameter passed by the test method.
 * The servlet checks the value and uses it to determine which instance of the
 * @WebServiceRef annotated service to use.
 */
@WebServlet("/SimpleStubClientServlet")
public class SimpleStubClientServlet extends HttpServlet {

    private static final long serialVersionUID = 4838332634689830661L;

    @WebServiceRef(name = "service/SimpleEchoServiceServer")
    SimpleEchoService simpleEchoServiceServer;
    

    @WebServiceRef(name = "service/SimpleEchoServiceBnd")
    SimpleEchoService simpleEchoServiceBnd;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	// name of the client to use for this test
        String serviceRef = req.getParameter("serviceRef");
        System.out.println("serviceRef = " + serviceRef);
        SimpleEcho simpleEcho;
        if(serviceRef.contains("bnd")) {
        	
            simpleEcho = simpleEchoServiceBnd.getSimpleEchoPort();
            
        } else {
        
        	simpleEcho = simpleEchoServiceServer.getSimpleEchoPort();
        	System.out.println("Using SimpleEchoServiceServer");
        }
         
        String response = simpleEcho.echo("echo");
        if (response != null && response.equals("echo")) {
            resp.getWriter().write("Pass");
        } else {
            resp.getWriter().write("Fail");
        }
    }
}