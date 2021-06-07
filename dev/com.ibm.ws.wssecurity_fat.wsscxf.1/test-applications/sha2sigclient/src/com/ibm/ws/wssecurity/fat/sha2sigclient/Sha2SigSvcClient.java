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

package com.ibm.ws.wssecurity.fat.sha2sigclient;

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

import test.wssecfvt.sha2sig.Sha2SigService1;
import test.wssecfvt.sha2sig.Sha2SigService2;
import test.wssecfvt.sha2sig.Sha2SigService3;
import test.wssecfvt.sha2sig.Sha2SigService4;
import test.wssecfvt.sha2sig.Sha2SigService5;
import test.wssecfvt.sha2sig.Sha2SigService6;
import test.wssecfvt.sha2sig.Sha2SigService7;
import test.wssecfvt.sha2sig.Sha2SigService8;

/**
 * Servlet implementation class CxfX509SigSvcClient
 */
@WebServlet("/Sha2SigSvcClient")
public class Sha2SigSvcClient extends HttpServlet {

    static String strJksLocation = "sslServerTrust.jks";
    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://sha2sig.wssecfvt.test";
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
                                   + "<soapenv:Body xmlns=\"http://sha2sig.wssecfvt.test/types\">"
                                   + "<invoke>WSSECFVT Simple Soap Msg</invoke>"
                                   + "</soapenv:Body>"
                                   + "</soapenv:Envelope>";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public Sha2SigSvcClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doWorker(HttpServletRequest request,
                            HttpServletResponse response) throws ServletException, IOException {

        // Extract values sent by the client
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

        System.out.println("rawWsdl:" + clientWsdl + ":rawWsdl");

        wsdlLocation = theType + "://localhost:" + thePort + "/sha2sig/"
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

            // setup to handle multiple services later
            if (rawServiceName.equals("Sha2SigService1")) {
                System.out.println("In Sha2SigService1 case");
                Sha2SigService1 service = new Sha2SigService1(wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("Sha2SigService2")) {
                System.out.println("In Sha2SigService2 case");
                Sha2SigService2 service = new Sha2SigService2(wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("Sha2SigService3")) {
                System.out.println("In Sha2SigService3 case");
                Sha2SigService3 service = new Sha2SigService3(wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("Sha2SigService4")) {
                System.out.println("In Sha2SigService4 case");
                Sha2SigService4 service = new Sha2SigService4(wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("Sha2SigService5")) {
                System.out.println("In Sha2SigService5 case");
                Sha2SigService5 service = new Sha2SigService5(wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("Sha2SigService6")) {
                System.out.println("In Sha2SigService6 case");
                Sha2SigService6 service = new Sha2SigService6(wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("Sha2SigService7")) {
                System.out.println("In Sha2SigService7 case");
                Sha2SigService7 service = new Sha2SigService7(wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("Sha2SigService8")) {
                System.out.println("In Sha2SigService8 case");
                Sha2SigService8 service = new Sha2SigService8(wsdlURL, serviceName);
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

            rsp.print("<html><head><title>CXF x509 Sig Service Cleint</title></head><body>");
            rsp.print("<p>Request: CXF Sha2 Service Client</p>");
            rsp.print("<p>Response: " + answer + "</p>");
            rsp.print("</body></html>");

        } catch (Exception ex) {
            ex.printStackTrace();
            PrintWriter rsp = response.getWriter();
            rsp.print("<html><head><title>CXF Sha2 Sig Service Client</title></head><body>");
            rsp.print("<p>Request: CXF Sha2 Sig Service Client</p>");
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
