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

package com.ibm.ws.wssecurity.fat.x509encclient;

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

import test.wssecfvt.x509enc.X509XmlEncService1;
import test.wssecfvt.x509enc.X509XmlEncService10;
import test.wssecfvt.x509enc.X509XmlEncService1X;
import test.wssecfvt.x509enc.X509XmlEncService2;
import test.wssecfvt.x509enc.X509XmlEncService3;
import test.wssecfvt.x509enc.X509XmlEncService4;
import test.wssecfvt.x509enc.X509XmlEncService5;
import test.wssecfvt.x509enc.X509XmlEncService6;
import test.wssecfvt.x509enc.X509XmlEncService7;
import test.wssecfvt.x509enc.X509XmlEncService8;
import test.wssecfvt.x509enc.X509XmlEncService9;

/**
 * Servlet implementation class CxfX509EncSvcClient
 */
@WebServlet("/CxfX509EncSvcClient")
public class CxfX509EncSvcClient extends HttpServlet {

    static String strJksLocation = "sslServerTrust.jks";
    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://x509enc.wssecfvt.test";
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
                                   + "<soapenv:Body xmlns=\"http://x509enc.wssecfvt.test/types\">"
                                   + "<invoke>WSSECFVT Simple Soap Msg</invoke>"
                                   + "</soapenv:Body>"
                                   + "</soapenv:Envelope>";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfX509EncSvcClient() {
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
            //Added 11/2020
            System.out.println("In CxfX509EncSvcClient.doWorker...");

            httpPortNum = request.getParameter("httpDefaultPort");
            httpSecurePortNum = request.getParameter("httpSecureDefaultPort");
            id = request.getParameter("id");
            pw = request.getParameter("pw");
            rawServiceName = request.getParameter("serviceName");
            clientWsdl = request.getParameter("clientWsdl").trim();
            serviceName = new QName(SERVICE_NS, rawServiceName);
            servicePort = new QName(SERVICE_NS, request.getParameter("servicePort"));
            msgId = request.getParameter("msg");

            //Added 11/2020
            System.out.println("httpPortNum is: " + httpPortNum);
            System.out.println("id is: " + id);
            System.out.println("pw is: " + pw);
            System.out.println("rawServiceName is: " + rawServiceName);
            System.out.println("clientWsdl is: " + clientWsdl);
            System.out.println("serviceName is: " + serviceName);
            System.out.println("servicePort is: " + servicePort);
            System.out.println("msgId is: " + msgId);

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

        wsdlLocation = theType + "://localhost:" + thePort + "/x509enc/"
                       + rawServiceName + "?wsdl";
        //Added 11/2020
        System.out.println("wsdlLocation is: " + wsdlLocation);

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

            String endPtUrl = theType + "://localhost:" + thePort + "/x509enc/" + rawServiceName;
            // setup to handle multiple services later
            if (rawServiceName.equals("X509XmlEncService1")) {
                System.out.println("In CxfX509EncSvcClient.doWorker: In X509XmlEncService1 case");
                X509XmlEncService1 service = new X509XmlEncService1(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);

                //Added 11/2020
                System.out.println("Before invoking dispSOAPMsg to createDispatch ...");

                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);

                //Added 11/2020
                System.out.println("In CxfX509EncSvcClient.doWorker: After invoking dispSOAPMsg to createDispatch ...");

            }
            if (rawServiceName.equals("X509XmlEncService1X")) {
                System.out.println("In X509XmlEncService1X case");
                X509XmlEncService1X service = new X509XmlEncService1X(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("X509XmlEncService2")) {
                System.out.println("In X509XmlEncService2 case");
                X509XmlEncService2 service = new X509XmlEncService2(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("X509XmlEncService3")) {
                System.out.println("In X509XmlEncService3 case");
                X509XmlEncService3 service = new X509XmlEncService3(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("X509XmlEncService4")) {
                System.out.println("In X509XmlEncService4 case");
                X509XmlEncService4 service = new X509XmlEncService4(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("X509XmlEncService5")) {
                System.out.println("In X509XmlEncService5 case");
                X509XmlEncService5 service = new X509XmlEncService5(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }

            if (rawServiceName.equals("X509XmlEncService6")) {
                System.out.println("In X509XmlEncService6 case");
                X509XmlEncService6 service = new X509XmlEncService6(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }

            if (rawServiceName.equals("X509XmlEncService7")) {
                System.out.println("In X509XmlEncService7 case");
                X509XmlEncService7 service = new X509XmlEncService7(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }

            if (rawServiceName.equals("X509XmlEncService8")) {
                System.out.println("In X509XmlEncService8 case");
                X509XmlEncService8 service = new X509XmlEncService8(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }

            if (rawServiceName.equals("X509XmlEncService9")) {
                System.out.println("In X509XmlEncService9 case");
                X509XmlEncService9 service = new X509XmlEncService9(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }

            if (rawServiceName.equals("X509XmlEncService10")) {
                System.out.println("In X509XmlEncService10 case");
                X509XmlEncService10 service = new X509XmlEncService10(wsdlURL, serviceName);
                service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endPtUrl);
                dispSOAPMsg = service.createDispatch(servicePort,
                                                     SOAPMessage.class, Mode.MESSAGE);
            }

            //
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
            System.out.println("In CxfX509EncSvcClient.doWorker: Service Client request Header: "
                               + soapReq.getSOAPHeader());
            System.out.println("In CxfX509EncSvcClient.doWorker: Service Client request Body: "
                               + soapReq.getSOAPBody());
            System.out.println("In CxfX509EncSvcClient.doWorker: Outgoing msg contains: " + msgToBeUsed);

            //Added 11/2020
            System.out.println("In CxfX509EncSvcClient.doWorker: clientWsdl is");

            System.out.println(clientWsdl);

            //Added 11/2020
            System.out.println("In CxfX509EncSvcClient.doWorker: Before dispSOAPMsg.invoke ...");

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

            System.out.println("In CxfX509EncSvcClient.doWorker: After dispSOAPMsg.invoke ...");

            System.out.println("In CxfX509EncSvcClient.doWorker: DEBUG: Response SOAPMessage:");
            soapResp.writeTo(System.out);

            String answer = soapResp.getSOAPBody().getTextContent();

            PrintWriter rsp = response.getWriter();
            System.out.println("Answer received: " + answer);

            rsp.print("<html><head><title>CXF x509 Enc Service Cleint</title></head><body>");
            rsp.print("<p>Request: CXF x509 Enc Service Client</p>");
            rsp.print("<p>Response: " + answer + "</p>");
            rsp.print("</body></html>");

        } catch (Exception ex) {
            ex.printStackTrace();
            PrintWriter rsp = response.getWriter();
            rsp.print("<html><head><title>CXF x509 Enc Service Cleint</title></head><body>");
            rsp.print("<p>Request: CXF x509 Enc Service Client</p>");
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
