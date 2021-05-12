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

package com.ibm.ws.wssecurity.fat.pwdigestclient;

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

import fats.cxf.pwdigest.wssec.SOAPServicePWDigest;

/**
 * Servlet implementation class CxfUntPWDigestSvcClient
 */
@WebServlet("/CxfUntPWDigestSvcClient")
public class CxfUntPWDigestSvcClient extends HttpServlet {

    static String strJksLocation = "sslServerTrust.jks";
    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://wssec.pwdigest.cxf.fats";
    private static String wsdlLocation = "";
    private static String thePort = "";
    private static String theType = "";

    private StringReader reqMsg = null;

    // new
    // StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.pwdigest.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");

    // QName serviceName = new QName(SERVICE_NS, "SOAPServicePWDigest");
    // QName portName = new QName(SERVICE_NS, "SOAPPortPWDigest");

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfUntPWDigestSvcClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doWorker(HttpServletRequest request,
                            HttpServletResponse response) throws ServletException, IOException {

        System.out.println("************************ "
                           + request.getParameter("testName")
                           + " ************************");
        // Extract default http port sent by client
        String httpPortNum = request.getParameter("httpDefaultPort");
        String httpSecurePortNum = request.getParameter("httpSecureDefaultPort");

        String clientWsdlFile = request.getParameter("clientWsdl");
        String theWsdl = request.getParameter("theWsdl");
        // set default WSDL parms
        wsdlLocation = "http://localhost:" + httpPortNum
                       + "/pwdigest/SOAPServicePWDigest?wsdl";
        QName serviceName = new QName(SERVICE_NS, "SOAPServicePWDigest");
        QName portName = new QName(SERVICE_NS, "SOAPPortPWDigest");

        if (httpSecurePortNum.equals("")) {
            System.out.println("Using Http");
            thePort = httpPortNum;
            theType = "http";
        } else {
            System.out.println("Using Https");
            thePort = httpSecurePortNum;
            theType = "https";
/*
 * System.setProperty("javax.net.ssl.trustStore", strJksLocation);
 * System.setProperty("javax.net.ssl.trustStorePassword",
 * "LibertyServer");
 */
        }

        if (theWsdl.matches("UsrTokenPWDigestWebSvc")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigest?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigest");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigest");
        }
        if (theWsdl.matches("UsrTokenPWDigestCreatedSvc")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigestCreated?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigestCreated");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigestCreated");
        }
        if (theWsdl.matches("UsrTokenPWDigestNonceCreatedSvc")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigestNonceCreated?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigestNonceCreated");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigestNonceCreated");
        }
        if (theWsdl.matches("UsrTokenPWDigestNonceSvc")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigestNonce?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigestNonce");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigestNonce");
        }
        if (theWsdl.matches("UsrTokenPWDigestNoPasswordSvc")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigestNoPassword?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigestNoPassword");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigestNoPassword");
        }
        if (theWsdl.matches("UsrTokenPWDigestWebSvcSSL")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigestWithSSL?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigestWithSSL");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigestWithSSL");
        }
        if (theWsdl.matches("UsrTokenPWDigestCreatedSvcSSL")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigestCreatedWithSSL?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigestCreatedWithSSL");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigestCreatedWithSSL");
        }
        if (theWsdl.matches("UsrTokenPWDigestNonceCreatedSvcSSL")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigestNonceCreatedWithSSL?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigestNonceCreatedWithSSL");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigestNonceCreatedWithSSL");
        }
        if (theWsdl.matches("UsrTokenPWDigestNonceSvcSSL")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigestNonceWithSSL?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigestNonceWithSSL");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigestNonceWithSSL");
        }
        if (theWsdl.matches("UsrTokenPWDigestNoPasswordSvcSSL")) {
            wsdlLocation = theType + "://localhost:" + thePort
                           + "/pwdigest/SOAPServicePWDigestNoPasswordWithSSL?wsdl";
            serviceName = new QName(SERVICE_NS, "SOAPServicePWDigestNoPasswordWithSSL");
            portName = new QName(SERVICE_NS, "SOAPPortPWDigestNoPasswordWithSSL");
        }
        // wsdlLocation = "http://localhost:" + httpPortNum +
        // "/pwdigest/UsrTokenPWDigestWebSvc?wsdl";
        String setTheId = request.getParameter("setId");

        if (!clientWsdlFile.isEmpty()) {
            wsdlLocation = "file:///" + clientWsdlFile;
        }
        System.out.println("wsdlLocation: " + wsdlLocation);
        URL wsdlURL = new URL(wsdlLocation);
        SOAPServicePWDigest servicePWDigest = new SOAPServicePWDigest(wsdlURL, serviceName);

        try {

            reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.pwdigest.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");
            Source src = new StreamSource(reqMsg);
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapReq = factory.createMessage();
            soapReq.getSOAPPart().setContent(src);
            soapReq.saveChanges();

            Dispatch<SOAPMessage> dispSOAPMsg = servicePWDigest.createDispatch(
                                                                               portName, SOAPMessage.class, Mode.MESSAGE);

            Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();

            System.out.println("setTheId: " + setTheId);
            // Set username token in reqcontext for non-ibm clients.
            if (setTheId.equals("user1")) {
                requestContext.put("ws-security.username", "user1");
                requestContext.put("ws-security.password", "security");
            }
            if (setTheId.equals("user3")) {
                requestContext.put("ws-security.username", "user3");
                requestContext.put("ws-security.password", "badPW3");
            }
            if (setTheId.equals("user4")) {
                requestContext.put("ws-security.username", "user4");
                requestContext.put("ws-security.password", "security");
            }
            if (setTheId.equals("user5")) {
                requestContext.put("ws-security.username", "user5");
                requestContext.put("ws-security.password", "badPW5");
            }
            if (setTheId.equals("user6")) {
                System.out.println("Setting user6 with NO password");
                requestContext.put("ws-security.username", "user6");
                //requestContext.put("ws-security.password", "security");
            }
            if (setTheId.equals("user77")) {
                requestContext.put("ws-security.username", "user77");
                requestContext.put("ws-security.password", "security");
            }
            if (setTheId.equals("NoId")) {
                requestContext.put("ws-security.password", "security");
            }

            if (setTheId.equals("NoIdBadPw")) {
                requestContext.put("ws-security.password", "BadPW22");
            }
            /*
             * orig from CL
             * if (setTheId.equals("altCallback1")) {
             * requestContext.put("ws-security.username", "user2");
             * requestContext.put("ws-security.password", "security"); //@av
             * requestContext
             * .put("ws-security.callback-handler",
             * "com.ibm.ws.wssecurity.fat.pwdigest.AltClientPWDigestCallbackHandler");
             * }
             */
            //Added 11/2020 to use client package pwdigestclient.AltClientPWDigestCallbackHandler
            if (setTheId.equals("altCallback1")) {
                requestContext.put("ws-security.username", "user2");
                requestContext.put("ws-security.password", "security");
                requestContext.put("ws-security.callback-handler",
                                   "com.ibm.ws.wssecurity.fat.pwdigestclient.AltClientPWDigestCallbackHandler");
            }
            /*
             * orig from CL
             * if (setTheId.equals("altCallback2")) {
             * requestContext.put("ws-security.username", "user4");
             * requestContext
             * .put("ws-security.callback-handler",
             * "com.ibm.ws.wssecurity.fat.pwdigest.AltClientPWDigestCallbackHandler");
             * }
             */
            //Added 11/2020 to use client package pwdigestclient.AltClientPWDigestCallbackHandler
            if (setTheId.equals("altCallback2")) {
                requestContext.put("ws-security.username", "user4");
                requestContext.put("ws-security.callback-handler",
                                   "com.ibm.ws.wssecurity.fat.pwdigestclient.AltClientPWDigestCallbackHandler");
            }
            if (setTheId.equals("badCallback")) {
                requestContext.put("ws-security.username", "user2");
                requestContext.put("ws-security.callback-handler",
                                   "com.ibm.ws.wssecurity.fat.pwdigest.MissingClientPWDigestCallbackHandler");
            }

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

            String answer = soapResp.getSOAPBody().getTextContent();
            System.out.println("PWDigest CXF SVC - response received: " + answer);

            PrintWriter rsp = response.getWriter();
            rsp.print("<html><head><title>CXF Password Digest Service Cleint</title></head><body>");
            rsp.print("<p>Request: Password Digest CXF Service Client</p>");
            rsp.print("<p>Response : " + answer + "</p>");
            rsp.print("</body></html>");

        } catch (Exception ex) {
            ex.printStackTrace();
            // throw new ServletException("Exception Thrown by server.", ex);
            ServletException e1 = new ServletException("The security token could not be authenticated or authorized");
            // Initializes the cause of this throwable to the specified value.
            // (The cause is the throwable that caused this throwable to get
            // thrown.)
            // chc//e1.initCause(ex);
            //throw e1;
            PrintWriter rsp = response.getWriter();
            rsp.print("<html><head><title>CXF Password Digest Service Cleint</title></head><body>");
            rsp.print("<p>Request: Password Digest CXF Service Client</p>");
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
