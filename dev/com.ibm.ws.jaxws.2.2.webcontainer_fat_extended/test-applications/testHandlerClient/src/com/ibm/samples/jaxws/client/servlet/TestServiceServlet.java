/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.samples.jaxws.client.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.samples.jaxws.client.ClientSayHelloService;
import com.ibm.samples.jaxws.client.ClientSayHelloWithHandlerService;
import com.ibm.samples.jaxws.client.SayHelloService;

public class TestServiceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Resource(name = "services/serviceServlet/HelloFromResource")
    @HandlerChain(file = "handler/handler-test-client.xml")
    private ClientSayHelloService serviceFromRes;

    @WebServiceRef(name = "services/serviceServlet/HelloFromWSRef")
    @HandlerChain(file = "handler/handler-test-client.xml")
    private ClientSayHelloService serviceFromRef;

    @WebServiceRef(name = "services/serviceServlet/HelloWithHandler")
    @HandlerChain(file = "handler/handler-test-client.xml")
    private ClientSayHelloWithHandlerService serviceWithHandler;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        SayHelloService portFromRes = serviceFromRes.getHelloServicePort();
        SayHelloService portFromRef = serviceFromRef.getHelloServicePort();
        SayHelloService portWithHandler = serviceWithHandler.getHelloServicePort();

        reConfigPorts(req, (BindingProvider) portFromRes, "SayHelloService");
        reConfigPorts(req, (BindingProvider) portFromRef, "SayHelloService");
        reConfigPorts(req, (BindingProvider) portWithHandler, "SayHelloServiceWithHandler");

        PrintWriter out = null;
        String target = req.getParameter("target");

        try {
            out = resp.getWriter();

            out.println("The greeting from @Resource: " + portFromRes.sayHello(target));
            out.println("The greeting from @WebServiceRef: " + portFromRef.sayHello(target));
            out.println("The greeting from @WebServiceWithHandler: " + portWithHandler.sayHello(target));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private void reConfigPorts(HttpServletRequest request, BindingProvider portBindingProvider, String serviceName) {
        String host = request.getLocalAddr();
        int port = request.getLocalPort();

        // Config port
        portBindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://" + host + ":" + port + "/testHandlerProvider/" + serviceName);

    }
}
