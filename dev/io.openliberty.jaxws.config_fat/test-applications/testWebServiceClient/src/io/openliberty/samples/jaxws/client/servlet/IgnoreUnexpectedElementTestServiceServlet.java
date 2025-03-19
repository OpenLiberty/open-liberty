/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.samples.jaxws.client.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import io.openliberty.samples.jaxws.client.ClientSayHelloWithHandlerService;
import io.openliberty.samples.jaxws.client.SayHelloService;

@WebServlet("/IgnoreUnexpectedElementTestServiceServlet")
public class IgnoreUnexpectedElementTestServiceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @WebServiceRef(name = "services/serviceServlet/Hello")
    private ClientSayHelloWithHandlerService service;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        SayHelloService portWithHandler = service.getHelloServicePort();
        String target = req.getParameter("target");

	if (target.equals("AddReqContextProps"))  {
           System.out.println("REQ_CTX: Need to add reqcontext properties...");
           BindingProvider bp1 = (BindingProvider) portWithHandler;
           Map<String, Object> requestContext = bp1.getRequestContext();
           requestContext.put("cxf.add.gzip.out.interceptor", "true");
           requestContext.put("cxf.add.gzip.in.interceptor", "true");
           target = "AddedElement";
        }

        reConfigPorts(req, (BindingProvider) portWithHandler, "SayHelloService");

        PrintWriter out = null;

        try {
            out = resp.getWriter();
            out.println("The greeting from @WebService: " + portWithHandler.sayHello(target));
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
        portBindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://" + host + ":" + port + "/testWebServiceClient/" + serviceName);

    }
}
