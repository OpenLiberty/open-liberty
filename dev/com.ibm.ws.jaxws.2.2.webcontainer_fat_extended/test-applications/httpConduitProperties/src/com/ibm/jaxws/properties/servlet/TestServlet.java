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
package com.ibm.jaxws.properties.servlet;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;

import com.ibm.jaxws.properties.echo.client.SimpleEcho;
import com.ibm.jaxws.properties.hello.client.Hello;
import com.ibm.jaxws.properties.interceptor.TestConduitInterceptor;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {
    @WebServiceRef(name = "service/SimpleEchoService")
    private com.ibm.jaxws.properties.echo.client.SimpleEchoService echoService;

    @WebServiceRef(name = "service/HelloService")
    private com.ibm.jaxws.properties.hello.client.HelloService helloService;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public TestServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String target = request.getParameter("target");
        Writer writer = response.getWriter();

        Object port = null;
        if ("SimpleEchoService".equals(target)) {
            port = echoService.getSimpleEchoPort();
        } else if ("HelloService".equals(target)) {
            port = helloService.getHelloPort();
        }

        if (null != port) {
            writer.write(getProperties(port));
        }
        writer.close();
    }

    private String getProperties(Object port) {
        String result = null;
        TestConduitInterceptor testedInterceptor = new TestConduitInterceptor();

        Client client = ClientProxy.getClient(port);
        client.getOutInterceptors().add(testedInterceptor);

        if (port instanceof SimpleEcho) {
            ((SimpleEcho) port).echo("Hello SimpleEchoService");
            result = testedInterceptor.getTestedProperties();
        } else if (port instanceof Hello) {
            ((Hello) port).sayHello();
            result = testedInterceptor.getTestedProperties();
        }

        return result;
    }
}
