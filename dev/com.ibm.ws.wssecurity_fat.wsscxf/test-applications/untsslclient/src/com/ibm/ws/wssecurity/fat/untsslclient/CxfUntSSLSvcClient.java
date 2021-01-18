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

package com.ibm.ws.wssecurity.fat.untsslclient;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.io.StringReader;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier ;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPBody;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service.Mode;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.ibm.websphere.simplicity.log.Log;

import fats.cxf.basicssl.wssec.FVTVersionBAService;
import fats.cxf.basicssl.wssec.FVTVersionBA2Service;
import fats.cxf.basicssl.wssec.FVTVersionBA3Service;
import fats.cxf.basicssl.wssec.FVTVersionBA4Service;
import fats.cxf.basicssl.wssec.FVTVersionBA5Service;
import fats.cxf.basicssl.wssec.FVTVersionBA6Service;
import fats.cxf.basicssl.wssec.FVTVersionBA7Service;
import fats.cxf.basicssl.wssec.FVTVersionBA8Service;
import fats.cxf.basicssl.wssec.FVTVersionBA9Service;
import fats.cxf.basicssl.wssec.FVTVersionBAbService;
import fats.cxf.basicssl.wssec.FVTVersionBAcService;
import fats.cxf.basicssl.wssec.FVTVersionBAdService;

/**
 * Servlet implementation class CxfUntSSLSvcClient
 */
@WebServlet("/CxfUntSSLSvcClient")
public class CxfUntSSLSvcClient extends HttpServlet {

	// If we use port 8020 and the server is actually using a different port, all tests will
	// fail as the server won't start properly.
	// if we just set up this way, the server will start and only one test will be affected
	// if the default port is NOT 8020
	//@WebServiceRef(wsdlLocation="https://localhost:8020/untoken/FVTVersionBAService?wsdl")
	@WebServiceRef(value=FVTVersionBAService.class, wsdlLocation="BasicPlcyBA.wsdl")
	FVTVersionBAService baService ;
	@WebServiceRef(value=FVTVersionBA6Service.class, wsdlLocation="BasicPlcyBA.wsdl")
	FVTVersionBA6Service ba6Service ;
    static String strJksLocation = "sslServerTrust.jks";
    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://wssec.basicssl.cxf.fats";
    private static String wsdlLocation = "";
    private static String httpPortNum = null;
    private static String httpSecurePortNum = null;
    private static String thePort = "";
    private static String theType = "";
    QName serviceName = null;
    QName servicePort = null;
    private static String rawServiceName = null;
    private static String id = null;
    private static String pw = null;
    private static String msgId = null;
    private static String msgToBeUsed = null;
    private boolean replayTest = false;
    private static String testName = null;
    private static String managedClient = null ;

    private StringReader reqMsg = null;

    static String simpleSoapBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header/>"
            + "<soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\">"
            + "<invoke>WSSECFVT Simple Soap Msg</invoke>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    static String strOldNonceCreated = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header>"
            + "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" soapenv:mustUnderstand=\"1\">"
            + "<wsse:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"UnameToken-abc99\">"
            + "<wsse:Username>user1</wsse:Username>"
            + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">security</wsse:Password>"
            + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">FXqdCgbt0l92yfhvIY0JIw==</wsse:Nonce>"
            + "<wsu:Created>2012-10-30T19:08:28.615Z</wsu:Created>"
            + "</wsse:UsernameToken>"
            + "</wsse:Security>"
            + "</soapenv:Header>"
            + "<soapenv:Body>"
            + "<invoke>WSSECFVT Faked Msg</invoke>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    static String strOldHash = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header>"
            + "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" soapenv:mustUnderstand=\"1\">"
            + "<wsse:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"UnameToken-xyz99\">"
            + "<wsse:Username>user1</wsse:Username>"
            + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\">2Y1ljxZwu3tCnY2DITB9p6mRtkE=</wsse:Password>"
            + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">ESI2awZbyg686/nFGLaBNA==</wsse:Nonce>"
            + "<wsu:Created>2012-10-30T19:08:28.615Z</wsu:Created>"
            + "</wsse:UsernameToken>"
            + "</wsse:Security>"
            + "</soapenv:Header>"
            + "<soapenv:Body>"
            + "<invoke>WSSECFVT Old Hash Msg</invoke>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    final static String strOldHash2 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header>"
            + "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" soapenv:mustUnderstand=\"1\">"
            + "<wsse:UsernameToken wsu:Id=\"UnameToken-ijk123\">"
            + "<wsse:Username>user1</wsse:Username>"
            + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">security</wsse:Password>"
            + "<wsu:Created>2011-10-20T14:47:12.304Z</wsu:Created>"
            + "</wsse:UsernameToken>"
            + "</wsse:Security>"
            + "</soapenv:Header>"
            + "<soapenv:Body>"
            + "<invoke>WSSECFVT Version: 2.0</invoke>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    static String strFakedMsg = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header>"
            + "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" soapenv:mustUnderstand=\"1\">"
            + "<wsse:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"UnameToken-pqr321\">"
            + "<wsse:Username>user1</wsse:Username>"
            + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">security</wsse:Password>"
            + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">FXqdCgbt0l92yfhvIY0JIw==</wsse:Nonce>"
            + "</wsse:UsernameToken>"
            + "</wsse:Security>"
            + "</soapenv:Header>"
            + "<soapenv:Body>"
            + "<invoke>WSSECFVT Faked Msg</invoke>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    static String strFutureTimestamp = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header>"
            + "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" soapenv:mustUnderstand=\"1\">"
            + "<wsu:Timestamp wsu:Id=\"TS-8\">"
            + "<wsu:Created>2102-10-25T17:23:43.143Z</wsu:Created>"
            + "<wsu:Expires>2102-10-25T17:28:43.143Z</wsu:Expires>"
            + "</wsu:Timestamp>"
            + "<wsse:UsernameToken wsu:Id=\"UnameToken-iah456\">"
            + "<wsse:Username>user1</wsse:Username>"
            + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">security</wsse:Password>"
            + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">newTusaNGvZbjPF+aTbvBg==</wsse:Nonce>"
            + "</wsse:UsernameToken>"
            + "</wsse:Security>"
            + "</soapenv:Header>"
            + "<soapenv:Body>"
            + "<invoke>WSSECFVT Version: 2.0</invoke>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    static String strReplayNonce = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header>"
            + "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" soapenv:mustUnderstand=\"1\">"
            + "<wsse:UsernameToken wsu:Id=\"UnameToken-efg876\">"
            + "<wsse:Username>user1</wsse:Username>"
            + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">security</wsse:Password>"
            + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">ri4FTI6obnCa+M1IuRYPtA==</wsse:Nonce>"
            + "</wsse:UsernameToken>"
            + "</wsse:Security>"
            + "</soapenv:Header>"
            + "<soapenv:Body>"
            + "<invoke>WSSECFVT Version: 2.0</invoke>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    static String strReplay = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header>"
            + "<wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">"
            + "<wsu:Timestamp wsu:Id=\"TS-15\">"
            + "<wsu:Created>2012-11-28T19:44:56.851Z</wsu:Created>"
            + "<wsu:Expires>2022-11-28T19:49:56.851Z</wsu:Expires>"
            + "</wsu:Timestamp>"
            + "<wsse:UsernameToken wsu:Id=\"UnameToken-klm509\">"
            + "<wsse:Username>user1</wsse:Username>"
            + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">security</wsse:Password>"
            + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">tAiUWnxhaC8gO0eZ1+bfhQ==</wsse:Nonce>"
            + "</wsse:UsernameToken>"
            + "</wsse:Security>"
            + "</soapenv:Header>"
            + "<soapenv:Body>"
            + "<invoke>WSSECFVT Version: 2.0</invoke>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    static String strMissingTimestamp = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header>"
            + "<wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">"
            + "<wsu:Timestamp wsu:Id=\"TS-15\">"
            + "</wsu:Timestamp>"
            + "<wsse:UsernameToken wsu:Id=\"UnameToken-aus365\">"
            + "<wsse:Username>user1</wsse:Username>"
            + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">security</wsse:Password>"
            + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">tAiUWnxhaQxfC0eZ1+bfhQ==</wsse:Nonce>"
            + "</wsse:UsernameToken>"
            + "</wsse:Security>"
            + "</soapenv:Header>"
            + "<soapenv:Body>"
            + "<invoke>WSSECFVT Missing timestamp</invoke>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    // new
    // StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");

    // QName serviceName2 = new QName(SERVICE_NS, "SOAPService2");
    // QName portName2 = new QName(SERVICE_NS, "SoapPort2");

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfUntSSLSvcClient() {
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
            testName = request.getParameter("testName") ;
            System.out.println("************************ "
                    + testName
                    + " ************************");
            httpPortNum = request.getParameter("httpDefaultPort");
            httpSecurePortNum = request.getParameter("httpSecureDefaultPort");
            id = request.getParameter("id");
            pw = request.getParameter("pw");
            rawServiceName = request.getParameter("serviceName");
            replayTest = request.getParameter("replayTest").equals("true");
            serviceName = new QName(SERVICE_NS, rawServiceName);
            servicePort = new QName(SERVICE_NS, request
                    .getParameter("servicePort"));
            msgId = request.getParameter("msg");
            managedClient = request.getParameter("managedClient") ;
            if (httpSecurePortNum.equals("")) {
                thePort = httpPortNum;
                theType = "http";
            } else {
                thePort = httpSecurePortNum;
                theType = "https";
/*
                if( managedClient == null || managedClient.trim().length() == 0 ){
                  System.out.println("Setting Trust Manager to: " + strJksLocation);
                  System.setProperty("javax.net.ssl.trustStore", strJksLocation);
                  System.setProperty("javax.net.ssl.trustStorePassword",
                          "LibertyServer");
                }  
*/
            }
        } catch (Exception ex) {
            System.out.println("Failed to find all required parameters");
            ex.printStackTrace();
            ServletException e1 = new ServletException(
                    "Failed to find all required parameters");
            throw e1;
        }

        wsdlLocation = theType + "://localhost:" + thePort + "/untoken/"
                + rawServiceName + "?wsdl";

        URL wsdlURL = new URL(wsdlLocation);
        // SOAPService2 service2 = new SOAPService2(wsdlURL, serviceName2);
        System.out.println("wsdlLocation: " + wsdlLocation);

        try {

            System.out.println("Will send appropriate message for msgId: "
                    + msgId);
            if (msgId.equals("oldMsg")) {
                msgToBeUsed = strOldHash;
            } else {
                if (msgId.equals("oldMsg2")) {
                    msgToBeUsed = strOldHash2;
                } else {
                    if (msgId.equals("fakedMsg")) {
                        msgToBeUsed = strFakedMsg;
                    } else {
                        if (msgId.equals("futureTime")) {
                            msgToBeUsed = strFutureTimestamp;
                        } else {
                            if (msgId.equals("replayNonce")) {
                                msgToBeUsed = strReplayNonce;
                            } else {
                                if (msgId.equals("replay")) {
                                    msgToBeUsed = strReplay;
                                } else {
                                    if (msgId.equals("missingTimestamp")) {
                                        msgToBeUsed = strMissingTimestamp;
                                    } else {
                                        if (msgId.equals("oldTimeMsg")) {
                                            msgToBeUsed = strOldNonceCreated;
                                        } else {
                                            msgToBeUsed = simpleSoapBody;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            reqMsg = new StringReader(msgToBeUsed);
            Source src = new StreamSource(reqMsg);
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapReq = factory.createMessage();
            soapReq.getSOAPPart().setContent(src);
            soapReq.saveChanges();
            Dispatch<SOAPMessage> dispSOAPMsg = null;

            // may have to case this
            if (rawServiceName.equals("FVTVersionBAService")) {
                System.out.println("In FVTVersionBAService case");
                FVTVersionBAService service ;
                if( managedClient == null || managedClient.trim().length() == 0 ){
                  System.out.println("UnManaged FVTVersionBAService case");
                  service = new FVTVersionBAService(wsdlURL, serviceName);
                } else {
                  System.out.println("Managed FVTVersionBAService case");
                  // had to hard code the port in the wsdl - if the service is actually using a different
                  // port, the test will fail - skip this test if we're using a different port
                  if (thePort.equals("8020")) {
                    service = baService ;
                  } else {
                	  System.out.println("Port is not 8020 - we can't really run the test - just return");
                      PrintWriter rsp = response.getWriter();
                      rsp.print("Response: WSSECFVT FVTVersion_ba01");
                	  return ;
                  }
                }
                dispSOAPMsg = service.createDispatch(servicePort, SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBA2Service")) {
                System.out.println("In FVTVersionBA2Service case");
                FVTVersionBA2Service service = new FVTVersionBA2Service(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBA3Service")) {
                System.out.println("In FVTVersionBA3Service case");
                FVTVersionBA3Service service = new FVTVersionBA3Service(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBA4Service")) {
                System.out.println("In FVTVersionBA4Service case");
                FVTVersionBA4Service service = new FVTVersionBA4Service(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBA5Service")) {
                System.out.println("In FVTVersionBA5Service case");
                FVTVersionBA5Service service = new FVTVersionBA5Service(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBA6Service")) {
                System.out.println("In FVTVersionBA6Service case");
                FVTVersionBA6Service service ;
                if( managedClient == null || managedClient.trim().length() == 0 ){
                  System.out.println("UnManaged FVTVersionBA6Service case");
                  service = new FVTVersionBA6Service(wsdlURL, serviceName);
                } else {
                  System.out.println("Managed FVTVersionBA6Service case");
                  // had to hard code the port in the wsdl - if the service is actually using a different
                  // port, the test will fail - skip this test if we're using a different port
                  if (thePort.equals("8020")) {
                    service = ba6Service ;
                  } else {
            	      System.out.println("Port is not 8020 - we can't really run the test - just return");
                      PrintWriter rsp = response.getWriter();
                      rsp.print("Response: WSSECFVT FVTVersion_ba06");
            	      return ;
                  }
                }
                dispSOAPMsg = service.createDispatch(servicePort, SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBA7Service")) {
                System.out.println("In FVTVersionBA7Service case");
                FVTVersionBA7Service service = new FVTVersionBA7Service(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBA8Service")) {
                System.out.println("In FVTVersionBA8Service case");
                FVTVersionBA8Service service = new FVTVersionBA8Service(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBA9Service")) {
                System.out.println("In FVTVersionBA9Service case");
                FVTVersionBA9Service service = new FVTVersionBA9Service(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBAbService")) {
                System.out.println("In FVTVersionBAbService case");
                FVTVersionBAbService service = new FVTVersionBAbService(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBAcService")) {
                System.out.println("In FVTVersionBAcService case");
                FVTVersionBAcService service = new FVTVersionBAcService(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            if (rawServiceName.equals("FVTVersionBAdService")) {
                System.out.println("In FVTVersionBAdService case");
                FVTVersionBAdService service = new FVTVersionBAdService(
                        wsdlURL, serviceName);
                dispSOAPMsg = service.createDispatch(servicePort,
                        SOAPMessage.class, Mode.MESSAGE);
            }
            //
            Map<String, Object> requestContext = dispSOAPMsg
                    .getRequestContext();
            if (id != null) {
                System.out.println("Setting id: " + id + " with password: "
                        + pw);
                requestContext.put("ws-security.username", id);
                requestContext.put("ws-security.password", pw);
            }

            // System.out.println("Right before invoke") ;
            System.out.println("Service Client request Header: "
                    + soapReq.getSOAPHeader());
            System.out.println("Service Client request Body: "
                    + soapReq.getSOAPBody());
            System.out.println("Outgoing msg contains: " + msgToBeUsed);

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);
            if (replayTest) {
                if( testName.contains( "TwoAndMoreMinutes" ) ){
                    Thread.sleep(130000);   // 2 minute 10 second
                } 
                if( testName.contains( "OneAndMoreMinutes" ) ){
                    Thread.sleep( 80000);  // 1 minute 20 second
                } 
                String answerBeforeReplay = soapResp.getSOAPBody()
                        .getTextContent();
                System.out.println("Answer received from first invocation: "
                        + answerBeforeReplay);
                System.out.println("re-invoking msg - for replay attack");
                // requestContext = dispSOAPMsg.getRequestContext();
                // if (id != null) {
                // requestContext.put("ws-security.username", id);
                // requestContext.put("ws-security.password", pw);
                // }
                soapResp = dispSOAPMsg.invoke(soapReq);
            }

            String answer = soapResp.getSOAPBody().getTextContent();

            PrintWriter rsp = response.getWriter();
            System.out.println("Answer received: " + answer);

            rsp
                    .print("<html><head><title>CXF UNT SSL Service Cleint</title></head><body>");
            rsp.print("<p>Request: UNT CXF Service Client</p>");
            rsp.print("<p>Response: " + answer + "</p>");
            rsp.print("</body></html>");

        } catch (Exception ex) {
            ex.printStackTrace();
            PrintWriter rsp = response.getWriter();
            rsp
                    .print("<html><head><title>CXF UNT SSL Service Cleint</title></head><body>");
            rsp.print("<p>Request: UNT CXF Service Client</p>");
            rsp.print("<p>Response: " + ex + "</p>");
            rsp.print("</body></html>");
            if (ex instanceof java.net.SocketException) {
                java.security.Provider p[] = Security.getProviders();
                for (int i = 0; i < p.length; i++) {
                    System.out.println("java security providers: " +  p[i]);
                }
                System.out.println("java protocol handler pkgs property: " + java.security.Security.getProperty("java.protocol.handler.pkgs"));
            }
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
