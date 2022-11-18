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

package com.ibm.ws.wssecurity.fat.wss11sigclient;

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
import javax.xml.ws.soap.SOAPBinding;

import test.wssecfvt.wss11sig.WSS11SigService1;
import test.wssecfvt.wss11sig.WSS11SigService2;
import test.wssecfvt.wss11sig.WSS11SigService4;
import test.wssecfvt.wss11sig.WSS11SigService5;
import test.wssecfvt.wss11sig.WSS11SigService6;
import test.wssecfvt.wss11sig.WSS11SigService6a;
import test.wssecfvt.wss11sig.WSS11SigService7;
import test.wssecfvt.wss11sig.WSS11SigService7a;
import test.wssecfvt.wss11sig.WSS11SigService8;

/**
 * Servlet implementation class CxfWss11SigSvcClient
 */
@WebServlet("/CxfWss11SigSvcClient")
public class CxfWss11SigSvcClient extends HttpServlet {

    static String strJksLocation = "sslServerTrust.jks";
    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://wss11sig.wssecfvt.test";
    private static String wsdlLocation = "";
    private static String httpPortNum = null;
    private static String httpSecurePortNum = null;
    private static String thePort = "";
    private static String theType = "";
    QName serviceName = null;
    QName servicePort = null;
    private static String clientWsdl = "";
    private static String rawServiceName = null;
    private static String id = null;
    private static String pw = null;
    private static String msgId = null;
    private static String msgToBeUsed = null;

    private StringReader reqMsg = null;

    static String simpleSoapBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                   + "<soapenv:Header/>"
                                   + "<soapenv:Body xmlns=\"http://wss11sig.wssecfvt.test/types\">"
                                   + "<invoke>WSSECFVT Simple Soap Msg</invoke>"
                                   + "</soapenv:Body>"
                                   + "</soapenv:Envelope>";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfWss11SigSvcClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doWorker(HttpServletRequest request,
                            HttpServletResponse response) throws ServletException, IOException {

        // Extract valuse sent by the client
        try {
            System.out.println("************************ "
                               + request.getParameter("testName")
                               + " ************************");
            httpPortNum = request.getParameter("httpDefaultPort");
            httpSecurePortNum = request.getParameter("httpSecureDefaultPort");
            id = request.getParameter("id");
            pw = request.getParameter("pw");
            rawServiceName = request.getParameter("serviceName");
            clientWsdl = request.getParameter("clientWsdl").trim();
            serviceName = new QName(SERVICE_NS, rawServiceName);
            servicePort = new QName(SERVICE_NS, request.getParameter("servicePort"));
            msgId = request.getParameter("msg");
            if (httpSecurePortNum.equals("")) {
                thePort = httpPortNum;
                theType = "http";
            } else {
                thePort = httpSecurePortNum;
                theType = "https";

            }
            System.out.println("clientWsdl:" + clientWsdl + ":end");
        } catch (Exception ex) {
            System.out.println("Failed to find all required parameters");
            ex.printStackTrace();
            ServletException e1 = new ServletException("Failed to find all required parameters");
            throw e1;
        }
        //System.setProperty("javax.net.ssl.trustStore", strJksLocation);
        //System.setProperty("javax.net.ssl.trustStorePassword", "LibertyServer");

        System.out.println("rawWsdl:" + clientWsdl + ":rawWsdl");

        wsdlLocation = theType + "://localhost:" + thePort + "/wss11sig/"
                       + rawServiceName + "?wsdl";
        if (!clientWsdl.equals("")) {
            wsdlLocation = "file:///" + clientWsdl;
        }

        URL wsdlURL = new URL(wsdlLocation);
        // SOAPService2 service2 = new SOAPService2(wsdlURL, serviceName2);
        System.out.println("wsdlLocation: " + wsdlLocation);
        System.out.println("wsdlUrl: " + wsdlURL);

        try {

            System.out.println("Will send appropriate message for msgId: "
                               + msgId);
            if (msgId.equals("someFutureSpecificMsg")) {
                msgToBeUsed = simpleSoapBody; // some future msg
            } else {
                msgToBeUsed = simpleSoapBody;
            }

            reqMsg = new StringReader(msgToBeUsed);
            Source src = new StreamSource(reqMsg);
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapReq = factory.createMessage();
            soapReq.getSOAPPart().setContent(src);
            soapReq.saveChanges();
            Dispatch<SOAPMessage> dispSOAPMsg = null;

            String endPtUrl = theType + "://localhost:" + thePort + "/wss11sig/" + rawServiceName;
            // setup to handle multiple services later
            if (rawServiceName.equals("WSS11SigService1")) {
                System.out.println("In WSS11SigService1 case");
                WSS11SigService1 service = new WSS11SigService1(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("WSS11SigService2")) {
                System.out.println("In WSS11SigService2 case");
                WSS11SigService2 service = new WSS11SigService2(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("WSS11SigService4")) {
                System.out.println("In WSS11SigService4 case");
                WSS11SigService4 service = new WSS11SigService4(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("WSS11SigService5")) {
                System.out.println("In WSS11SigService5 case");
                WSS11SigService5 service = new WSS11SigService5(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("WSS11SigService6")) {
                System.out.println("In WSS11SigService6 case");
                WSS11SigService6 service = new WSS11SigService6(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("WSS11SigService6a")) {
                System.out.println("In WSS11SigService6a case");
                WSS11SigService6a service = new WSS11SigService6a(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("WSS11SigService7")) {
                System.out.println("In WSS11SigService7 case");
                WSS11SigService7 service = new WSS11SigService7(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("WSS11SigService7a")) {
                System.out.println("In WSS11SigService7a case");
                WSS11SigService7a service = new WSS11SigService7a(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("WSS11SigService8")) {
                System.out.println("In WSS11SigService8 case");
                WSS11SigService8 service = new WSS11SigService8(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }

            Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
            if (id != null) {
                System.out.println("Setting id: " + id + " with password: "
                                   + pw);
                requestContext.put("ws-security.username", id);
                requestContext.put("ws-security.password", pw);
            }

            // may want to make this set the properties some times, and other
            // times use the defaults - tbd
            String strServerDir = System.getProperty("server.config.dir").replace('\\', '/');

            // System.out.println("Right before invoke") ;
            System.out.println("Service Client request Header: "
                               + soapReq.getSOAPHeader());
            System.out.println("Service Client request Body: "
                               + soapReq.getSOAPBody());
            System.out.println("Outgoing msg contains: " + msgToBeUsed);
            System.out.println(clientWsdl);

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);
            System.out.println("DEBUG: Response SOAPMessage:");
            soapResp.writeTo(System.out);

            String answer = soapResp.getSOAPBody().getTextContent();

            PrintWriter rsp = response.getWriter();
            System.out.println("Answer received: " + answer);

            rsp.print("<html><head><title>CXF WSS 11 Service Cleint</title></head><body>");
            rsp.print("<p>Request: CXF WSS 11 Service Client</p>");
            rsp.print("<p>Response: " + answer + "</p>");
            rsp.print("</body></html>");

        } catch (Exception ex) {
            ex.printStackTrace();
            PrintWriter rsp = response.getWriter();
            rsp.print("<html><head><title>CXF WSS 11 Service Cleint</title></head><body>");
            rsp.print("<p>Request: CXF WSS 11 Service Client</p>");
            rsp.print("<p>Response: " + ex + "</p>");
            rsp.print("</body></html>");

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
