/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.jaxws.transport.client.security.servlet;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import com.ibm.ws.jaxws.transport.security.SayHello;
import com.ibm.ws.jaxws.transport.security.SayHelloPojoService;
import com.ibm.ws.jaxws.transport.security.SayHelloSingletonService;
import com.ibm.ws.jaxws.transport.security.SayHelloStatelessService;

@WebServlet("/TestUnmanagedTransportSecurityServlet")
public class TestUnmanagedJaxWsTransportSecurityServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = -5011080702176253305L;

    private static final String PROVIDER_CONTEXT_ROOT = "/TransportSecurityProvider";

    private SayHelloPojoService pojoService;
    private SayHelloStatelessService statelessService;
    private SayHelloSingletonService singletonService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        String userName = req.getParameter("user");
        String testMethod = req.getParameter("testMethod");

        System.out.println("The test case is: " + testMethod);
        Writer out = null;

        try {
            String resultString = "";
            out = resp.getWriter();

            SayHello sayHelloPort = getAndConfigClient(req, SayHello.class);
            resultString = sayHelloPort.sayHello(userName);

            out.write(resultString);
        } catch (Exception e) {
            out.write(getThrowableMessage(e));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    @SuppressWarnings("unchecked")
    private <T> T getAndConfigClient(HttpServletRequest req, Class<T> clazz) throws MalformedURLException {
        String serviceType = req.getParameter("serviceType");
        String schema = req.getParameter("schema");
        String host = req.getLocalAddr();
        String port = req.getParameter("port");
        String requestPath = req.getParameter("path");

        T client = null;

        if ("pojo".equals(serviceType)) {
            pojoService = new SayHelloPojoService(new URL("http://" + host + ":" + req.getLocalPort() + PROVIDER_CONTEXT_ROOT + "/unauthorized/employPojoService?wsdl"),
                                                  new QName("http://ibm.com/ws/jaxws/transport/security/", "SayHelloPojoService"));
            client = (T) pojoService.getSayHelloPojoPort();
        } else if ("stateless".equals(serviceType)) {
            statelessService = new SayHelloStatelessService(new URL("http://" + host + ":" + req.getLocalPort() + PROVIDER_CONTEXT_ROOT
                                                                    + "/unauthorized/employStatelessService?wsdl"),
                                                            new QName("http://ibm.com/ws/jaxws/transport/security/", "SayHelloStatelessService"));
            client = (T) statelessService.getSayHelloStatelessPort();
        } else if ("singleton".equals(serviceType)) {
            singletonService = new SayHelloSingletonService(new URL("http://" + host + ":" + req.getLocalPort() + PROVIDER_CONTEXT_ROOT
                                                                    + "/unauthorized/employSingletonService?wsdl"),
                                                            new QName("http://ibm.com/ws/jaxws/transport/security/", "SayHelloSingletonService"));
            client = (T) singletonService.getSayHelloSingletonPort();
        } else {
            throw new IllegalArgumentException("The serviceType=" + serviceType + " is unrecognized.");
        }

        BindingProvider provider = (BindingProvider) client;

        StringBuilder sBuilder = new StringBuilder(schema).append("://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(PROVIDER_CONTEXT_ROOT)
                        .append(requestPath);
        String urlPath = sBuilder.toString();
        System.out.println(clazz.getSimpleName() + ": The request web service url is: " + urlPath);
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, urlPath);

        return client;
    }

    private String getThrowableMessage(Throwable origThrowable) {
        StringBuilder twBuilder = new StringBuilder();

        Throwable tmp = null;
        do {
            twBuilder.append(origThrowable.getMessage())
                            .append("\n");
            tmp = origThrowable;
        } while (null != origThrowable.getCause() && (origThrowable = origThrowable.getCause()) != tmp);

        return twBuilder.substring(0, twBuilder.length() - 1);
    }

}
