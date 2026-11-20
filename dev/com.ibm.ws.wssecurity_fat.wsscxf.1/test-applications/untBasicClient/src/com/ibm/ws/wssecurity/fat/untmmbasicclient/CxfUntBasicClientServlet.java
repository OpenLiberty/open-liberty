/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.untmmbasicclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;

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
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service.Mode;

import fats.cxf.basic.wssec.SOAPService2;

/**
 * Multi-module EAR client servlet. Invokes SOAPService2 in the untBasicProvider module.
 * The client-side WS-SecurityPolicy comes from this module's WEB-INF/policy-attachments-client.xml.
 */
@WebServlet("/CxfUntBasicClientServlet")
public class CxfUntBasicClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://wssec.basic.cxf.fats";

    private final QName serviceName = new QName(SERVICE_NS, "SOAPService2");
    private final QName portName = new QName(SERVICE_NS, "SoapPort2");

    private static final String REQUEST_MSG = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>";

    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String httpPortNum = request.getParameter("httpDefaultPort");
        String clientType = request.getParameter("untClient");
        String answer = "";

        try {
            SOAPMessage soapResp = invokeService(httpPortNum, clientType);
            answer = soapResp.getSOAPBody().getTextContent();
        } catch (Exception ex) {
            System.out.println("Exception invoking Web Service: " + ex);
            answer = ex.getMessage();
        }

        PrintWriter rsp = response.getWriter();
        rsp.print("<html><head><title>CxfUntBasicClientServlet</title></head><body>");
        rsp.print("<p>Request: CxfUntBasicClientServlet</p>");
        rsp.print("<p>Response: " + answer + "</p>");
        rsp.print("</body></html>");
    }

    private SOAPMessage invokeService(String portNum, String clntType) throws Exception {

        URL wsdlURL = new URL("http://localhost:" + portNum + "/untBasicProvider/SOAPService2?wsdl");
        SOAPService2 service = new SOAPService2(wsdlURL, serviceName);

        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage();
        Source src = new StreamSource(new StringReader(REQUEST_MSG));
        soapReq.getSOAPPart().setContent(src);
        soapReq.saveChanges();

        Dispatch<SOAPMessage> dispSOAPMsg = service.createDispatch(portName, SOAPMessage.class, Mode.MESSAGE);

        Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
        if (clntType.equals("cxf")) {
            requestContext.put("ws-security.username", "user1");
            requestContext.put("ws-security.password", "security");
        } else if (clntType.equals("cxfbaduser")) {
            requestContext.put("ws-security.username", "baduser123");
            requestContext.put("ws-security.password", "security");
        } else if (clntType.equals("cxfbadpswd")) {
            requestContext.put("ws-security.username", "user1");
            requestContext.put("ws-security.password", "badpswd123");
        }

        return dispSOAPMsg.invoke(soapReq);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWorker(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWorker(request, response);
    }
}
