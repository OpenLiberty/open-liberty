/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.microprofile.metrics.internal.monitor_fat.jaxws.client;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

@WebServlet("/SimpleStubClientServlet")
public class SimpleStubClientServlet extends HttpServlet {

    private static final long serialVersionUID = 4838332634686520661L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SimpleEchoService simpleEchoService = new SimpleEchoService(null, new QName("http://jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io/", "SimpleEchoService"));
        SimpleEcho simpleEcho = simpleEchoService.getSimpleEchoPort();
        
        setEndpointAddress((BindingProvider) simpleEcho, req, "SimpleEchoService");
        
        String response = simpleEcho.echo("echo");
        if (response != null && response.equals("echo")) {
            resp.getWriter().write("Pass");
        } else {
            resp.getWriter().write("Fail");
        }
    }
    
	protected String getRequestBaseURL(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }
	
    protected void setEndpointAddress(BindingProvider bindingProvider, HttpServletRequest request, String endpointPath) {
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                getRequestBaseURL(request) + "/" + endpointPath);
    }
}