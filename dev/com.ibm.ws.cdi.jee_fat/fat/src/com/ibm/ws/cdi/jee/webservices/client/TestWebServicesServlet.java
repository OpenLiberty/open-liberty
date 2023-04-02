/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee.webservices.client;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import org.junit.Test;

import com.ibm.ws.cdi.jee.webservices.client.services.SayHello;
import com.ibm.ws.cdi.jee.webservices.client.services.SayHelloPojoService;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/")
@SuppressWarnings("serial")
public class TestWebServicesServlet extends FATServlet {
    private static final String PROVIDER_CONTEXT_ROOT = "/resourceWebServicesProvider";

    @WebServiceRef(name = "service/SayHelloPojoService")
    SayHelloPojoService pojoService;

    /**
     * Verifies that a @Resource can be injected and used in a WebService that resides in a CDI implicit bean archive
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userName = req.getParameter("user");
        if (userName == null) {
            userName = "Bobby";
        }

        System.out.println("The test case is: " + "cdi injection + webservices");
        String resultString = "";
        SayHello sayHelloPort = getAndConfigClient(req, SayHello.class);
        resultString = sayHelloPort.sayHello(userName);

        assertEquals("Hello, Bobby from mySecondName in SayHelloPojoService", resultString);
    }

    @SuppressWarnings("unchecked")
    private <T> T getAndConfigClient(HttpServletRequest req, Class<T> clazz) {

        String schema = req.getParameter("schema");
        String host = req.getLocalName();
        String port = req.getParameter("port");
        String requestPath = req.getParameter("path");
        if (schema == null) {
            schema = req.getScheme();
        }
        if (port == null) {
            port = String.valueOf(req.getLocalPort());
        }
        if (requestPath == null) {
            requestPath = "/SayHelloPojoService";
        }

        T client = null;
        client = (T) pojoService.getSayHelloPojoPort();
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
}
