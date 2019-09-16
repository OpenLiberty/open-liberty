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
package com.ibm.ws.test.overriddenuri.client.servlet;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.test.overriddenuri.client.SimpleEchoService;

@WebServlet("/TestOverriddenEndpointUriServlet")
public class TestOverriddenEndpointUriServlet extends HttpServlet {

    @WebServiceRef(name = "service/SimpleEchoService")
    private SimpleEchoService defaultSimpleEchoService;

/*
 * @WebServiceRef(name = "service/SimpleEchoService")
 * private SimpleEchoService testSimpleEchoService1;
 *
 * @WebServiceRef(name = "service/SimpleEchoService2")
 * private SimpleEchoService testSimpleEchoService2;
 */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //String endpintAddress = null;
        //TestInterceptor interceptor = new TestInterceptor();

        String result = null;
        try {
            result = defaultSimpleEchoService.getSimpleEchoPort().echo("Hello");
        } catch (Exception e) {
            result = e.getMessage();
        }

        Writer writer = resp.getWriter();
        writer.write(result != null ? result : "");
        writer.close();

    }
}
