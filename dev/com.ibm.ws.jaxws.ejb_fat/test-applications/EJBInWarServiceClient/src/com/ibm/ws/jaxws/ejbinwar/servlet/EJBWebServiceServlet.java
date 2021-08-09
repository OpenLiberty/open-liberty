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
package com.ibm.ws.jaxws.ejbinwar.servlet;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.ejbinwar.ejb.SayHello;
import com.ibm.ws.jaxws.ejbinwar.ejb.SayHelloPOJOService;
import com.ibm.ws.jaxws.ejbinwar.ejb.SayHelloSingletonService;
import com.ibm.ws.jaxws.ejbinwar.ejb.SayHelloStatelessService;

@WebServlet("/EJBWebServiceServlet")
@SuppressWarnings("serial")
public class EJBWebServiceServlet extends HttpServlet {
    @WebServiceRef
    SayHelloSingletonService singletonService;

    @WebServiceRef
    SayHelloStatelessService statelessService;

    @WebServiceRef
    SayHelloPOJOService pojoService;

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        String queryMethod = req.getParameter("testMethod");

        SayHello singletonSayHello = getAndConfigPort(singletonService, req);
        SayHello statelessSayHello = getAndConfigPort(statelessService, req);
        SayHello pojoSayHello = getAndConfigPort(pojoService, req);

        Writer out = null;
        try {
            out = resp.getWriter();
            if ("testSayHelloFromStateless".equals(queryMethod)) {
                out.write(statelessSayHello.sayHello("user"));
            } else if ("testSayHelloFromSingle".equals(queryMethod)) {
                out.write(singletonSayHello.sayHello("user"));
            } else if ("testInvokeOtherFromStateless".equals(queryMethod)) {
                out.write(statelessSayHello.invokeOther());
            } else if ("testInvokeOtherFromSingle".equals(queryMethod)) {
                out.write(singletonSayHello.invokeOther());
            } else if ("testSayHelloFromPojo".equals(queryMethod)) {
                out.write(pojoSayHello.sayHello("user"));
            } else if ("testInvokeOtherFromPojo".equals(queryMethod)) {
                out.write(pojoSayHello.invokeOther());
            } else {
                out.write("Inexistent method: " + queryMethod);
            }
        } catch (Exception e) {
            out.write(e.getMessage());
        } finally {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private SayHello getAndConfigPort(Service service, HttpServletRequest req) {
        SayHello helloPort = null;
        String path = null;
        if (service instanceof SayHelloSingletonService) {
            helloPort = ((SayHelloSingletonService) service).getSayHelloSingletonPort();
            path = "/EJBInWarService/SayHelloSingletonService";
        } else if (service instanceof SayHelloStatelessService) {
            helloPort = ((SayHelloStatelessService) service).getSayHelloStalelessPort();
            path = "/EJBInWarService/SayHelloStatelessService";
        } else {
            helloPort = ((SayHelloPOJOService) service).getSayHelloPOJOPort();
            path = "/EJBInWarService/SayHelloPOJOService";
        }

        int port = req.getLocalPort();
        String host = req.getParameter("hostName");
        BindingProvider provider = (BindingProvider) helloPort;
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                         "http://" + host + ":" + port + path);
        return helloPort;
    }

}
