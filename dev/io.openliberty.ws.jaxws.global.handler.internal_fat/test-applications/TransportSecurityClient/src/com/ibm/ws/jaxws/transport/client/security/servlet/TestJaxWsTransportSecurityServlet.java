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
import java.io.StringReader;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.transport.security.SayHello;
import com.ibm.ws.jaxws.transport.security.SayHelloPojoService;
import com.ibm.ws.jaxws.transport.security.SayHelloSingletonService;
import com.ibm.ws.jaxws.transport.security.SayHelloStatelessService;

@WebServlet("/TestTransportSecurityServlet")
public class TestJaxWsTransportSecurityServlet extends HttpServlet {
    /**  */
    private static final long serialVersionUID = -2515871642810337333L;

    private static final String PROVIDER_CONTEXT_ROOT = "/TransportSecurityProvider";

    private static final QName POJO_PORT_QNAME = new QName("http://ibm.com/ws/jaxws/transport/security/", "SayHelloPojoPort");
    private static final QName STATELESS_PORT_QNAME = new QName("http://ibm.com/ws/jaxws/transport/security/", "SayHelloStatelessPort");
    private static final QName SINGLETON_PORT_QNAME = new QName("http://ibm.com/ws/jaxws/transport/security/", "SayHelloSingletonPort");

    @WebServiceRef(name = "service/SayHelloPojoService")
    SayHelloPojoService pojoService;

    @WebServiceRef(name = "service/SayHelloStatelessService")
    SayHelloStatelessService statelessService;

    @WebServiceRef(name = "service/SayHelloSingletonService")
    SayHelloSingletonService singletonService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        String userName = req.getParameter("user");
        String type = req.getParameter("testMode");
        String testMethod = req.getParameter("testMethod");

        System.out.println("The test case is: " + testMethod);
        Writer out = null;
        StringReader reqMsg = null;
        try {
            String resultString = "";
            out = resp.getWriter();
            if ("dispatch".equals(type)) {
                @SuppressWarnings("unchecked")
                Dispatch<SOAPMessage> sayHelloDispatch = getAndConfigClient(req, Dispatch.class);

                reqMsg = new StringReader(createMessage(userName));
                Source src = new StreamSource(reqMsg);
                MessageFactory factory = MessageFactory.newInstance();
                SOAPMessage soapReq = factory.createMessage();
                soapReq.getSOAPPart().setContent(src);
                soapReq.saveChanges();

                SOAPMessage retMsg = sayHelloDispatch.invoke(soapReq);
                resultString = retMsg.getSOAPBody().getTextContent();
            } else {
                SayHello sayHelloPort = getAndConfigClient(req, SayHello.class);
                resultString = sayHelloPort.sayHello(userName);
            }

            out.write(resultString);
            System.out.println("The resultString is: " + resultString);
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
    private <T> T getAndConfigClient(HttpServletRequest req, Class<T> clazz) {
        String serviceType = req.getParameter("serviceType");
        String schema = req.getParameter("schema");
        String host = req.getLocalAddr();
        String port = req.getParameter("port");
        String requestPath = req.getParameter("path");

        T client = null;
        if (Dispatch.class.isAssignableFrom(clazz)) {
            if ("pojo".equals(serviceType)) {
                client = (T) pojoService.createDispatch(POJO_PORT_QNAME, SOAPMessage.class, Mode.MESSAGE);
            } else if ("stateless".equals(serviceType)) {
                client = (T) statelessService.createDispatch(STATELESS_PORT_QNAME, SOAPMessage.class, Mode.MESSAGE);
            } else if ("singleton".equals(serviceType)) {
                client = (T) singletonService.createDispatch(SINGLETON_PORT_QNAME, SOAPMessage.class, Mode.MESSAGE);
            } else {
                throw new IllegalArgumentException("The serviceType=" + serviceType + " is unrecognized.");
            }
        } else {
            if ("pojo".equals(serviceType)) {
                client = (T) pojoService.getSayHelloPojoPort();
            } else if ("stateless".equals(serviceType)) {
                client = (T) statelessService.getSayHelloStatelessPort();
            } else if ("singleton".equals(serviceType)) {
                client = (T) singletonService.getSayHelloSingletonPort();
            } else {
                throw new IllegalArgumentException("The serviceType=" + serviceType + " is unrecognized.");
            }
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

    private String createMessage(String userName) {
        StringBuilder sBuilder = new StringBuilder("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">")
                        .append("<soap:Body>")
                        .append("<ns2:sayHello xmlns:ns2=\"http://ibm.com/ws/jaxws/transport/security/\">")
                        .append("<arg0>").append(userName).append("</arg0>")
                        .append("</ns2:sayHello>")
                        .append("</soap:Body>")
                        .append("</soap:Envelope>");
        return sBuilder.toString();

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
