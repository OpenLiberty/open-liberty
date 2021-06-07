/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.samlcxfclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.io.StringReader;
import java.util.Map;

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
import javax.xml.ws.soap.SOAPBinding;

import fats.cxf.basic.wssec.SAMLSOAPService2;
import fats.cxf.basic.wssec.SAMLSOAPService5;
import fats.cxf.basic.wssec.SamlTokenTransportSecure;
import fats.cxf.basic.wssec.SAMLSymSignService;
import fats.cxf.basic.wssec.SAMLSymEncrService;
import fats.cxf.basic.wssec.SAMLSymSignEncrService;
import fats.cxf.basic.wssec.SAMLAsymSignService;
import fats.cxf.basic.wssec.SAMLAsymEncrService;
import fats.cxf.basic.wssec.SAMLAsymSignEncrService;
import fats.cxf.basic.wssec.SAMLAsyncX509Service;
//import fats.cxf.basic.wssec.SAMLSOAPService3;
//import fats.cxf.basic.wssec.SAMLSOAPService4;


/**
 * Servlet implementation class CxfSamlSvcClient
 */
//@WebServlet("/CxfSamlSvcClient")
public class CxfSamlSvcClient extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://wssec.basic.cxf.fats";
    private static String wsdlLocation = "";
    private static URL wsdlURL = null ; 
    private static SOAPMessage soapReq ;

    QName serviceName2 = new  QName(SERVICE_NS, "SAMLSOAPService2");
    QName portName2 =    new  QName(SERVICE_NS, "SAMLSoapPort2");
//    QName serviceName3 = new  QName(SERVICE_NS, "SAMLSOAPService3");
//    QName portName3 =    new  QName(SERVICE_NS, "SAMLSoapPort3");
//    QName serviceName4 = new  QName(SERVICE_NS, "SAMLSOAPService4");
//    QName portName4 =    new  QName(SERVICE_NS, "SAMLSoapPort4");
    QName secureServiceName = new QName(SERVICE_NS, "SamlTokenTransportSecure") ;
    QName securePortName = new QName(SERVICE_NS, "SamlTokenTransportSecurePort") ;
    QName symSignServiceName = new QName(SERVICE_NS, "SAMLSymSignService");
    QName symSignPortName = new QName(SERVICE_NS, "SAMLSymSignPort") ;
    QName symEncrServiceName = new QName(SERVICE_NS, "SAMLSymEncrService");
    QName symEncrPortName = new QName(SERVICE_NS, "SAMLSymEncrPort") ;    
    QName symSignEncrServiceName = new QName(SERVICE_NS, "SAMLSymSignEncrService");
    QName symSignEncrPortName = new QName(SERVICE_NS, "SAMLSymSignEncrPort") ;
    QName asymSignServiceName = new QName(SERVICE_NS, "SAMLAsymSignService");
    QName asymSignPortName = new QName(SERVICE_NS, "SAMLAsymSignPort") ;
    QName asymEncrServiceName = new QName(SERVICE_NS, "SAMLAsymEncrService");
    QName asymEncrPortName = new QName(SERVICE_NS, "SAMLAsymEncrPort") ;    
    QName asymSignEncrServiceName = new QName(SERVICE_NS, "SAMLAsymSignEncrService");
    QName asymSignEncrPortName = new QName(SERVICE_NS, "SAMLAsymSignEncrPort") ;
    QName asymSignEncrAsyncServiceName = new QName(SERVICE_NS, "SAMLAsyncX509Service");
    QName asymSignEncrAsyncPortName = new QName(SERVICE_NS, "SAMLAsyncX509Port") ;
    

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfSamlSvcClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Extract default http port sent by client
        String httpPortNum = request.getParameter("httpDefaultPort");
        System.out.println("httpPortNum: " + httpPortNum);
        String httpSecurePortNum = request.getParameter("httpDefaultSecurePort");
        System.out.println("httpSecurePortNum: " + httpSecurePortNum);
        // Extract client type to set correct test scenario
        String clientType = request.getParameter("samlClient");
        System.out.println("clientType: " + clientType);
        String clientWsdl = request.getParameter("clntWsdlLocation");
        System.out.println("clientWsdl: " + clientWsdl);
        String serviceName = request.getParameter("serviceName") ;
        System.out.println("serviceName: " + serviceName) ;
        String id = request.getParameter("id") ;
        System.out.println("id: " + id) ;
        String pw = request.getParameter("pw") ;
        System.out.println("pw: " + pw) ;
        SOAPMessage soapResp = null;
        Dispatch<SOAPMessage> dispSOAPMsg = null;
        String answer = "";

        StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");

        if (clientWsdl != null) {
            wsdlLocation = "file:///" + clientWsdl;
        } else {
            if (httpSecurePortNum != null) {
                wsdlLocation = "https://localhost:" + httpSecurePortNum + "/samltoken/" + serviceName + "?wsdl";
            } else {                
                wsdlLocation = "http://localhost:" + httpPortNum + "/samltoken/" + serviceName + "?wsdl";
            }
        }
        System.out.println("wsdlLocation: " + wsdlLocation);

        try {

            wsdlURL = new URL(wsdlLocation);

            Source src = new StreamSource(reqMsg);
            MessageFactory factory = MessageFactory.newInstance();
            soapReq = factory.createMessage();
            soapReq.getSOAPPart().setContent(src);
            soapReq.saveChanges();

            if (serviceName2.toString().contains(serviceName)) {
                System.out.println("soap2");
                SAMLSOAPService2 service = new SAMLSOAPService2(wsdlURL, serviceName2);
                dispSOAPMsg = service.createDispatch(portName2, SOAPMessage.class, Mode.MESSAGE); 
            }
            if (secureServiceName.toString().contains(serviceName)) {
                System.out.println("secure") ;
                SamlTokenTransportSecure service = new SamlTokenTransportSecure(wsdlURL, secureServiceName);
                dispSOAPMsg = service.createDispatch(securePortName, SOAPMessage.class, Mode.MESSAGE);
            }
            if (symSignServiceName.toString().contains(serviceName)) {
                System.out.println("SymSign") ;
                SAMLSymSignService service = new SAMLSymSignService(wsdlURL, symSignServiceName);
//                service.addPort(symSignPortName, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:8010/samltoken/SAMLSymSignService");
                dispSOAPMsg = service.createDispatch(symSignPortName, SOAPMessage.class, Mode.MESSAGE);
            }
            if (symEncrServiceName.toString().contains(serviceName)) {
                System.out.println("SymEncr") ;
                SAMLSymEncrService service = new SAMLSymEncrService(wsdlURL, symEncrServiceName);
//                service.addPort(symEncrPortName, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:8010/samltoken/SAMLSymEncrService");
                dispSOAPMsg = service.createDispatch(symEncrPortName, SOAPMessage.class, Mode.MESSAGE);
            }
            if (symSignEncrServiceName.toString().contains(serviceName)) {
                System.out.println("SymSignEncr") ;
                SAMLSymSignEncrService service = new SAMLSymSignEncrService(wsdlURL, symSignEncrServiceName);
//                service.addPort(symSignEncrPortName, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:8010/samltoken/SAMLSymSignEncrService");
                dispSOAPMsg = service.createDispatch(symSignEncrPortName, SOAPMessage.class, Mode.MESSAGE);
            }
            if (asymSignServiceName.toString().contains(serviceName)) {
                System.out.println("AsymSign") ;
                SAMLAsymSignService service = new SAMLAsymSignService(wsdlURL, asymSignServiceName);
//                service.addPort(asymSignPortName, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:8010/samltoken/SAMLAsymSignService");
                dispSOAPMsg = service.createDispatch(asymSignPortName, SOAPMessage.class, Mode.MESSAGE);
            }
            if (asymEncrServiceName.toString().contains(serviceName)) {
                System.out.println("AsymEncr") ;
                SAMLAsymEncrService service = new SAMLAsymEncrService(wsdlURL, asymEncrServiceName);
//                service.addPort(asymEncrPortName, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:8010/samltoken/SAMLAsymEncrService");
                dispSOAPMsg = service.createDispatch(asymEncrPortName, SOAPMessage.class, Mode.MESSAGE);
            }
            if (asymSignEncrServiceName.toString().contains(serviceName)) {
                System.out.println("AsymSignEncr") ;
                SAMLAsymSignEncrService service = new SAMLAsymSignEncrService(wsdlURL, asymSignEncrServiceName);
//                service.addPort(asymSignEncrPortName, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:8010/samltoken/SAMLAsymSignEncrService");
                dispSOAPMsg = service.createDispatch(asymSignEncrPortName, SOAPMessage.class, Mode.MESSAGE);
            }
            if (asymSignEncrAsyncServiceName.toString().contains(serviceName)) {
                System.out.println("AsymSignEncrAsync") ;
                SAMLAsyncX509Service service = new SAMLAsyncX509Service(wsdlURL, asymSignEncrAsyncServiceName);
//                service.addPort(asymSignEncrPortName, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:8010/samltoken/SAMLAsyncX509Service");
                dispSOAPMsg = service.createDispatch(asymSignEncrAsyncPortName, SOAPMessage.class, Mode.MESSAGE);
            }
            if (dispSOAPMsg == null) {
                throw new Exception("Could not build request to unknown service") ;
            }

            Map<String, Object> requestContext =dispSOAPMsg.getRequestContext();

            if (id != null) {
                requestContext.put("ws-security.username", id);
            }
            if (pw != null) {
                requestContext.put("ws-security.password", pw);
            }

            soapResp = dispSOAPMsg.invoke(soapReq);

            System.out.println("DEBUG: Response received:");
            soapResp.writeTo(System.out);

            answer = soapResp.getSOAPBody().getTextContent();

        } catch (Exception ex) {
            System.out.println("Exception invoking my Web Service");
            System.out.println("Exception answer message is: " + ex.getMessage());
            // ex.printStackTrace();
            answer = ex.getMessage();
        }

        PrintWriter rsp = response.getWriter();
        rsp.print("<html><head><title>CXF SAML Service Cleint</title></head><body>");
        rsp.print("<p>Request: SAML CXF Service Client</p>");
        rsp.print("<p>Response: "+answer+"</p>");
        rsp.print("</body></html>");

        return;
    }

//    private Dispatch<SOAPMessage> invokeService2(String portNum, String securePortNum, String clntType) throws Exception
//    {	
//
//        try {
//
//            SAMLSOAPService2 service2 = new SAMLSOAPService2(wsdlURL, serviceName2);
//
//            Dispatch<SOAPMessage> dispSOAPMsg = 
//                            service2.createDispatch(portName2,
//                                                    SOAPMessage.class, Mode.MESSAGE);
//
//            return dispSOAPMsg ;
////            Map<String, Object> requestContext =dispSOAPMsg.getRequestContext();
////            // Set username token in reqcontext for non-ibm clients.
////            if (clntType.equals("cxf") ) {
////                requestContext.put("ws-security.username", "user1");
////                requestContext.put("ws-security.password", "user1pwd");
////            }
////            else if (clntType.equals("cxfbaduser")) {
////                requestContext.put("ws-security.username", "baduser123");
////                requestContext.put("ws-security.password", "security");
////            }
////            else if (clntType.equals("cxfbadpswd")) {
////                requestContext.put("ws-security.username", "user1");
////                requestContext.put("ws-security.password", "badpswd123");
////            }
////
////            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);
////
////            return soapResp;
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw e;
//        }
//
//    }
//
//    private Dispatch<SOAPMessage> invokeSamlTokenTransportSecure(String portNum, String securePortNum, String clientWsdl) throws Exception
//    {       
//
//        try {
//
//            SamlTokenTransportSecure service = new SamlTokenTransportSecure(wsdlURL, secureServiceName);
//
////            Source src = new StreamSource(reqMsg);
////            MessageFactory factory = MessageFactory.newInstance();
////            SOAPMessage soapReq = factory.createMessage();
////            soapReq.getSOAPPart().setContent(src);
////            soapReq.saveChanges();
////
//            Dispatch<SOAPMessage> dispSOAPMsg = 
//                            service.createDispatch(securePortName,
//                                                    SOAPMessage.class, Mode.MESSAGE);
//            return dispSOAPMsg ;
//
////            Map<String, Object> requestContext =dispSOAPMsg.getRequestContext();
////            // Set username token in reqcontext for non-ibm clients.
//////            if (clntType.equals("cxf") ) {
////                requestContext.put("ws-security.username", "user1");
////                requestContext.put("ws-security.password", "user1pwd");
//////            }
//////            else if (clntType.equals("cxfbaduser")) {
//////                requestContext.put("ws-security.username", "baduser123");
//////                requestContext.put("ws-security.password", "security");
//////            }
//////            else if (clntType.equals("cxfbadpswd")) {
//////                requestContext.put("ws-security.username", "user1");
//////                requestContext.put("ws-security.password", "badpswd123");
//////            }
////
////            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);
////
////            return soapResp;
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw e;
//        }
//
//    }
//
//    
//
//
//    private SOAPMessage xinvokeSamlTokenTransportSecure(String securePortNum, String clientWsdl) throws Exception
//    {       
//
//        // String wsdlLocation; 
//        if (clientWsdl != null) {
//            wsdlLocation = "file:///" + clientWsdl;
//        } else {
//            wsdlLocation = "https://localhost:" + securePortNum + "/samltoken/SamlTokenTransportSecure?wsdl";
//        }
//
//      System.out.println("secure 1");
//      StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");
//      System.out.println("secure 2");
//        Source src = new StreamSource(reqMsg);
//      System.out.println("secure 3");
//        MessageFactory factory = MessageFactory.newInstance();
//      System.out.println("secure 4");
//        SOAPMessage soapReq = factory.createMessage();
//      System.out.println("secure 5");
//       soapReq.getSOAPPart().setContent(src);
//      System.out.println("secure 6");
//        soapReq.saveChanges();
//        System.out.println("secure 7");
//
//
////        
//        System.out.println("wsdlLocation: " + wsdlLocation);
////        System.out.println("secure 1");
//        URL wsdlURL = new URL(wsdlLocation);
////        System.out.println("secure 1a");
//        System.out.println("secure 8");
//        SamlTokenTransportSecure secureService = new SamlTokenTransportSecure(wsdlURL, secureServiceName);
//        System.out.println("secure 9");
////        System.out.println("secure 1b");
////        StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");
////
////        System.out.println("secure 2");
////
////        Source src = new StreamSource(reqMsg);
////        System.out.println("secure 3");
////       MessageFactory factory = MessageFactory.newInstance();
////       System.out.println("secure 4");
////        SOAPMessage soapReq = factory.createMessage();
////        System.out.println("secure 5");
////        soapReq.getSOAPPart().setContent(src);
////        System.out.println("secure 6");
////        soapReq.saveChanges();
////        System.out.println("about to invoke message");
////
//        Dispatch<SOAPMessage> dispSOAPMsg = 
//                        secureService.createDispatch(securePortName,
//                                                SOAPMessage.class, Mode.MESSAGE);
//
//        Map<String, Object> requestContext =dispSOAPMsg.getRequestContext();
//        SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);
//        System.out.println("about to return from invokeSamlTokenTransportSecure");
//
//        return soapResp;
//    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response) ;
        return ; 
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doWorker(request, response) ;
        return ; 	}

}
