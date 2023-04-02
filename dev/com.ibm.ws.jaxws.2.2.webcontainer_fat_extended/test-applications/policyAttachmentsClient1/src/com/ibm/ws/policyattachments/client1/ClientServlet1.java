/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.policyattachments.client1;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.Addressing;

import com.ibm.ws.policyattachments.client1.service1.HelloService;
import com.ibm.ws.policyattachments.client1.service1.HelloService1;
import com.ibm.ws.policyattachments.client1.service1.HelloService2;
import com.ibm.ws.policyattachments.client1.service1.HelloService3;
import com.ibm.ws.policyattachments.client1.service1.HelloService4;

/**
 * This is the Servlet responsible for invoking the Web Service endpoints via
 * WebServiceRef clients.
 */
@WebServlet("/ClientServlet1")
public class ClientServlet1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // NonAnonymous Client with HelloClientReplyToHandler Attached
    @Addressing(required = true, enabled = true)
    @WebServiceRef(name = "services/HelloService") // Setting name = "service/HelloService" is required to match name of <service-ref> in server.xml
    HelloService helloService;

    // Anonymous Client
    @Addressing(required = true, enabled = true)
    @WebServiceRef(name = "services/HelloService2") // Setting name = "service/HelloService2" is required to match name of <service-ref> in server.xml
    HelloService2 helloService2;

    // NonAnonymous Client with HelloClientReplyToHandler Attached
    @Addressing(required = true, enabled = true)
    @WebServiceRef(name = "services/HelloService3") // Setting name = "service/HelloService3" is required to match name of <service-ref> in server.xml
    HelloService3 helloService3;

    // Anonymous Client
    @Addressing(required = true, enabled = true)
    @WebServiceRef(name = "services/HelloService4") // Setting name = "service/HelloService4" is required to match name of <service-ref> in server.xml
    HelloService4 helloService4;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClientServlet1() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub

        String host = request.getLocalAddr();
        int port = request.getLocalPort();
        PrintWriter writer = response.getWriter();

        Enumeration<?> enu = request.getParameterNames();
        while (enu.hasMoreElements()) {
            String paraName = (String) enu.nextElement();
            System.out.println("Get parameter: " + paraName + " - "
                               + request.getParameter(paraName));
        }

        String portName = request.getParameter("portName");
        String methodName = request.getParameter("method");

        String result = "";

        if (methodName.equals("helloWithPolicy")) {

            // WS-Addressing Policy has been attached to the helloWithPolicy end points on
            // HelloService1, HelloService2, HelloService3, HelloService4
            if (portName.equals("HelloService")) {

                HelloService1 proxy = helloService.getHelloService1Port();
                result = proxy.helloWithPolicy();

            } else if (portName.equals("HelloService2")) {

                HelloService1 proxy = helloService2.getHelloService2Port();
                result = proxy.helloWithPolicy();

            } else if (portName.equals("HelloService3")) {

                HelloService1 proxy = helloService3.getHelloService3Port();
                result = proxy.helloWithPolicy();

            } else if (portName.equals("HelloService4")) {

                HelloService1 proxy = helloService4.getHelloService4Port();
                result = proxy.helloWithPolicy();
            }
        } else if (methodName.equals("helloWithOptionalPolicy")) {
            // TODO: Additional else if's on the Web Service's methods can be used if we can find a way to
            //       expand test coverage to cover additional <wsp:URL> values set in the Policy Attachment files
        } else if (methodName.equals("helloWithYouWant")) {
            // TODO: Additional else if's on the Web Service's methods can be used if we can find a way to
            //       expand test coverage to cover additional <wsp:URL> values set in the Policy Attachment files
        } else if (methodName.equals("helloWithoutPolicy")) {
            // TODO: Additional else if's on the Web Service's methods can be used if we can find a way to
            //       expand test coverage to cover additional <wsp:URL> values set in the Policy Attachment files
        } else {
            writer.println("No method invoked");
        }

        writer.println(result);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
    }

}
