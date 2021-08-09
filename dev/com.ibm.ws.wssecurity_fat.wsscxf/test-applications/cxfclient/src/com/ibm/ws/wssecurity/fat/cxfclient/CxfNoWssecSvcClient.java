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

package com.ibm.ws.wssecurity.fat.cxfclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;

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

// import componenttest.topology.impl.LibertyServer;
// import componenttest.topology.impl.LibertyServerFactory;

import fats.cxf.basic.wssec.SOAPService1;

/**
 * Servlet implementation class CxfNoWssecSvcClient
 */
@WebServlet("/CxfNoWssecSvcClient")
public class CxfNoWssecSvcClient extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // private static LibertyServer server = LibertyServerFactory.getLibertyServer("wssecServer1");
    private static final String SERVICE_NS = "http://wssec.basic.cxf.fats";

    private static String wsdlLocation = "";

    private final StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");

    QName serviceName1 = new QName(SERVICE_NS, "SOAPService1");
    QName portName1 = new QName(SERVICE_NS, "SoapPort1");

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfNoWssecSvcClient() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Extract default http port sent by client
        String httpPortNum = request.getParameter("httpDefaultPort");

        wsdlLocation = "http://localhost:" + httpPortNum + "/nowssec/SOAPService1?wsdl";
        System.out.println("wsdlLocation: " + wsdlLocation);

        URL wsdlURL = new URL(wsdlLocation);
        SOAPService1 service1 = new SOAPService1(wsdlURL, serviceName1);

        try {

            Source src = new StreamSource(reqMsg);
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapReq1 = factory.createMessage();
            soapReq1.getSOAPPart().setContent(src);
            soapReq1.saveChanges();

            Dispatch<SOAPMessage> dispSOAPMsg = service1.createDispatch(portName1,
                                                                        SOAPMessage.class, Mode.MESSAGE);

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq1);

            String answer = soapResp.getSOAPBody().getTextContent();

            PrintWriter rsp = response.getWriter();
            rsp.print("<html><head><title>CXF NoWssec Service Cleint</title></head><body>");
            rsp.print("<p>Request: NoWssec CXF Service Client</p>");
            rsp.print("<p>Response: " + answer + "</p>");
            rsp.print("</body></html>");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return;
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
