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
package com.ibm.ws.jaxws.virtualhost.services;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;

/**
 * Servlet implementation class Endpoint
 */
@WebServlet("/Endpoint")
public class Endpoint extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public Endpoint() {
        super();
        // TODO Auto-generated constructor stub
    }

//    @Inject
//    PassivatedBean bean;
//    wsdlLocation="http://localhost:9080/cutDownTest/HelloWeb?wsdl"
    @WebServiceRef
    MyServiceClient helloWeb;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter pw = response.getWriter();
        pw.println(helloWeb.getMyServiceEndpointPort().tellSomething("demo1"));
//		 HelloWeb hello = bean.getHelloWeb();
//		pw.println ("<br/>BeanSays says " + bean.getHelloWeb().sayHello("Mark"));
        pw.close();
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
    }

}
