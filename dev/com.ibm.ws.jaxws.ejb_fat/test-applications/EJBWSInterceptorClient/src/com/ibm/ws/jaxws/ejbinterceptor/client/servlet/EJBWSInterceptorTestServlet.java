/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxws.ejbinterceptor.client.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.SOAPFaultException;

import com.ibm.ws.jaxws.ejbinterceptor.client.SayHello;
import com.ibm.ws.jaxws.ejbinterceptor.client.SayHelloService;

@WebServlet("/EJBWSInterceptorTestServlet")
public class EJBWSInterceptorTestServlet extends HttpServlet {

    @WebServiceRef(SayHelloService.class)
    private SayHello sayHello;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
//        SayHelloService sayHelloSer = new SayHelloService();
//        SayHello sayHello = sayHelloSer.getSayHelloPort();
        String encodedUrl = request.getParameter("url");

        if (null != encodedUrl) {
            String url = URLDecoder.decode(encodedUrl, "utf-8");
            ((BindingProvider) sayHello).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        }
        PrintWriter writer = resp.getWriter();
        try {
            writer.println(sayHello.hello("EJBWSInterceptor"));
        } catch (SOAPFaultException sfex) {
            writer.println(sfex.getMessage());
        }
    }
}
