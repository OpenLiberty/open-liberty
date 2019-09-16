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
package com.ibm.ws.jaxws.ejblifecycle.client.servlet;

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

import com.ibm.ws.jaxws.ejblifecycle.client.SimpleEcho;
import com.ibm.ws.jaxws.ejblifecycle.client.SimpleEchoService;

@WebServlet("/EJBWSLifeCycleTestServlet")
public class EJBWSLifeCycleTestServlet extends HttpServlet {

    @WebServiceRef(SimpleEchoService.class)
    private SimpleEcho simpleEcho;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        String encodedUrl = request.getParameter("url");

        if (null != encodedUrl) {
            String url = URLDecoder.decode(encodedUrl, "utf-8");
            ((BindingProvider) simpleEcho).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        }
        PrintWriter writer = resp.getWriter();
        writer.println(simpleEcho.echo("Hello EJBWSLifeCycle"));
    }
}
