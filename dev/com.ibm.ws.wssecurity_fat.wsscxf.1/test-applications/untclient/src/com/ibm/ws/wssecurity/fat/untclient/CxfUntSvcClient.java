/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.untclient;

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
import fats.cxf.basic.wssec.SOAPService3;
import fats.cxf.basic.wssec.SOAPService4;

/**
 * Servlet implementation class CxfUntSvcClient
 */
@WebServlet("/CxfUntSvcClient")
public class CxfUntSvcClient extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://wssec.basic.cxf.fats";
    private static String wsdlLocation = "";

    QName serviceName2 = new QName(SERVICE_NS, "SOAPService2");
    QName portName2 = new QName(SERVICE_NS, "SoapPort2");
    QName serviceName3 = new QName(SERVICE_NS, "SOAPService3");
    QName portName3 = new QName(SERVICE_NS, "SoapPort3");
    QName serviceName4 = new QName(SERVICE_NS, "SOAPService4");
    QName portName4 = new QName(SERVICE_NS, "SoapPort4");

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfUntSvcClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Extract default http port sent by client
        String httpPortNum = request.getParameter("httpDefaultPort");
        // Extract client type to set correct test scenario
        String clientType = request.getParameter("untClient");
        String clientWsdl = request.getParameter("clntWsdlLocation");
        SOAPMessage soapResp = null;
        String answer = "";

        try {
            if (clientType.equals("nonce1")) {
                soapResp = invokeService3(httpPortNum);
            } else if (clientType.equals("nonceExpected")) {
                System.out.println("Invoking SOAPService4...");
                soapResp = invokeService4(httpPortNum, clientWsdl);
            } else {
                soapResp = invokeService2(httpPortNum, clientType);
            }

            System.out.println("DEBUG: Response received:");
            soapResp.writeTo(System.out);

            answer = soapResp.getSOAPBody().getTextContent();

        } catch (Exception ex) {
            System.out.println("Exception invoking Web Service");
            // ex.printStackTrace();
            answer = ex.getMessage();
        }

        PrintWriter rsp = response.getWriter();
        rsp.print("<html><head><title>CXF UNT Service Cleint</title></head><body>");
        rsp.print("<p>Request: UNT CXF Service Client</p>");
        rsp.print("<p>Response: " + answer + "</p>");
        rsp.print("</body></html>");

        return;
    }

    private SOAPMessage invokeService2(String portNum, String clntType) throws Exception {

        String wsdlLocation = "http://localhost:" + portNum + "/untoken/SOAPService2?wsdl";
        StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");
        URL wsdlURL = new URL(wsdlLocation);
        SOAPService2 service2 = new SOAPService2(wsdlURL, serviceName2);

        Source src = new StreamSource(reqMsg);
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage();
        soapReq.getSOAPPart().setContent(src);
        soapReq.saveChanges();

        Dispatch<SOAPMessage> dispSOAPMsg = service2.createDispatch(portName2,
                                                                    SOAPMessage.class, Mode.MESSAGE);

        Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
        // Set username token in reqcontext for non-ibm clients.
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

        SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

        return soapResp;
    }

    private SOAPMessage invokeService3(String portNum) throws Exception {

        String wsdlLocation = "http://localhost:" + portNum + "/untoken/SOAPService3?wsdl";
        StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");
        URL wsdlURL = new URL(wsdlLocation);
        SOAPService3 service3 = new SOAPService3(wsdlURL, serviceName3);

        Source src = new StreamSource(reqMsg);
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage();
        soapReq.getSOAPPart().setContent(src);
        soapReq.saveChanges();

        Dispatch<SOAPMessage> dispSOAPMsg = service3.createDispatch(portName3,
                                                                    SOAPMessage.class, Mode.MESSAGE);

        Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
        SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

        return soapResp;
    }

    private SOAPMessage invokeService4(String portNum, String clientWsdl) throws Exception {

        // String wsdlLocation = "http://localhost:" + portNum + "/untoken/SOAPService4?wsdl";
        String wsdlLocation = "file:///" + clientWsdl;

        StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");
        URL wsdlURL = new URL(wsdlLocation);
        SOAPService4 service4 = new SOAPService4(wsdlURL, serviceName4);

        Source src = new StreamSource(reqMsg);
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage();
        soapReq.getSOAPPart().setContent(src);
        soapReq.saveChanges();

        Dispatch<SOAPMessage> dispSOAPMsg = service4.createDispatch(portName4,
                                                                    SOAPMessage.class, Mode.MESSAGE);

        Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
        SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

        return soapResp;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response);
        return;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response);
        return;
    }

}
