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

package com.ibm.ws.wssecurity.fat.wss11encclient;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;

import javax.net.ssl.HostnameVerifier ;
import javax.xml.ws.soap.SOAPBinding;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPBody;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service.Mode;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.ibm.websphere.simplicity.log.Log;

import test.wssecfvt.wss11enc.WSS11EncService1 ;
import test.wssecfvt.wss11enc.WSS11EncService2 ;

/**
 * Servlet implementation class CxfWss11EncSvcClient
 */
@WebServlet("/CxfWss11EncSvcClient")
public class CxfWss11EncSvcClient extends HttpServlet {

	private static final String SERVICE_NS = "http://wss11enc.wssecfvt.test";
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

	private SOAPMessage reqMsg = null;
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CxfWss11EncSvcClient() {
		super();
	}
	
	       // create a request message with SOAP header
        private SOAPMessage createSOAPMessage() {
                
        String PREFIX      ="fvt";
        String URI         ="http://encryptedhdr/WSSECFVT/CXF";
        String NAMESPACE_URI =  "http://wss11enc.wssecfvt.test";

        SOAPMessage message = null;
        
        try {
           
            
            StringReader reqMsg = new StringReader("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"> "
                            +   "<SOAP-ENV:Header>"
                            +           "<fvt:CXF_FVT xmlns:fvt=\"http://encryptedhdr/WSSECFVT/CXF\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"Id-1809608452\"><fvt:id xmlns:fvt=\"http://encryptedhdr/WSSECFVT/CXF\">ENCHDR_TEST</fvt:id><fvt:password xmlns:fvt=\"http://encryptedhdr/WSSECFVT/CXF\">Good_and_Ok</fvt:password></fvt:CXF_FVT>"
                            +   "</SOAP-ENV:Header>"
                            +   "<SOAP-ENV:Body>"
                            +            "<ns1:EncHdrTest xmlns:ns1=\"http://wss11enc.wssecfvt.test\">"
                            +                "<value>SOAP Body Value</value>"
                            +           "</ns1:EncHdrTest>"
                            +   "</SOAP-ENV:Body>"
                            + "</SOAP-ENV:Envelope>");
        
        Source src = new StreamSource(reqMsg);
        MessageFactory factory = MessageFactory.newInstance();
        message = factory.createMessage();

        message.getSOAPPart().setContent(src);
        
        //SOAPPart part = message.getSOAPPart();
        //SOAPEnvelope env = part.getEnvelope();
        //SOAPHeader header = env.getHeader();
        //SOAPBody body = env.getBody();

        //SOAPFactory sFactory = SOAPFactory.newInstance();
        //System.out.println("CxfWss11EncSvcClient createSOAPMessage() SOAPFactory.newInstance(): " + sFactory);
        // Construct SOAP header
        //SOAPElement sHelem1 = sFactory.createElement("CXF_FVT",PREFIX,URI); 
        //System.out.println("CxfWss11EncSvcClient createSOAPMessage() sHelem1: " + sHelem1);
        
        //SOAPElement sCHelem11 = sFactory.createElement("id",PREFIX,URI);
        // attach value to id element 
        //sCHelem11.addTextNode("ENCHDR_TEST"); 
        //SOAPElement sCHelem12 = sFactory.createElement("password",PREFIX,URI);
        // attach value to password element  
        //sCHelem12.addTextNode("Good_and_Ok");
        //add child elements to the root element
        //sHelem1.addChildElement(sCHelem11); 
        //System.out.println("CxfWss11EncSvcClient createSOAPMessage()  sHelem1.addChildElement(sCHelem11): " + sHelem1);
        //sHelem1.addChildElement(sCHelem12);
        //System.out.println("CxfWss11EncSvcClient createSOAPMessage()  sHelem1.addChildElement(sCHelem13): " + sHelem1);
        // add SOAP element for header to SOAP header object  
        //header.addChildElement(sHelem1);
        //System.out.println("CxfWss11EncSvcClient createSOAPMessage()  header.addChildElement(sCHelem1): " + header);
        
        // Construct SOAP Body                                 
        //SOAPElement operation = body.addChildElement("EncHdrTest", "ns1", NAMESPACE_URI); 
        //SOAPElement value = operation.addChildElement("value");
        //value.addTextNode("SOAP Body Value"); 

        message.saveChanges();  
        } catch (Exception ex) {
                System.out.println("createSOAPMessage(): Failed to create SOAP message");
            ex.printStackTrace();
        }
        
        return message;
        }
        
	
	// Update the request message by adding a SOAP header element
	private void updateSOAPMessage(SOAPMessage msg) {
        String PREFIX      ="fvt";
        String URI         ="http://encryptedhdr/WSSECFVT/CXF";
        String NAMESPACE_URI =  "http://wss11enc.wssecfvt.test";
        
        try {
        SOAPPart part = msg.getSOAPPart();
        SOAPEnvelope env = part.getEnvelope();
        SOAPHeader header = env.getHeader();
        
        // Construct another SOAP header
        SOAPFactory sFactory = SOAPFactory.newInstance();
        SOAPElement sHelem2 = sFactory.createElement("CXF_FVT_TEST", PREFIX, URI); 
        header.addChildElement(sHelem2);

        msg.saveChanges();
    } catch (Exception ex) {
    	System.out.println("updateSOAPMessage(): Failed to update SOAP message");
        ex.printStackTrace();
    }

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
			servicePort = new QName(SERVICE_NS, request
					.getParameter("servicePort"));
			msgId = request.getParameter("msg");
			if (httpSecurePortNum.equals("")) {
				thePort = httpPortNum;
				theType = "http";
			} else {
				thePort = httpSecurePortNum;
				theType = "https";

			}
			System.out.println("clientWsdl:" + clientWsdl + ":end") ;
		} catch (Exception ex) {
			System.out.println("Failed to find all required parameters");
			ex.printStackTrace();
			ServletException e1 = new ServletException(
					"Failed to find all required parameters");
			throw e1;
		}
		
		//System.setProperty("javax.net.ssl.trustStorePassword", "LibertyServer");

		System.out.println("rawWsdl:" + clientWsdl + ":rawWsdl") ; 
		
		wsdlLocation = theType + "://localhost:" + thePort + "/wss11enc/"
					+ rawServiceName + "?wsdl";
		if (!clientWsdl.equals("")) {
			wsdlLocation = "file:///" + clientWsdl;
		}

		URL wsdlURL = new URL(wsdlLocation);
		
		System.out.println("wsdlLocation: " + wsdlLocation);
		System.out.println("wsdlUrl: " + wsdlURL);

		try {
			System.out.println("Will send appropriate message for msgId: " + msgId);

			SOAPMessage soapReq = createSOAPMessage();
			
	        Dispatch<SOAPMessage> dispSOAPMsg = null;
			String endPtUrl = theType + "://localhost:" + thePort + "/wss11enc/" + rawServiceName ;

			// setup to handle multiple services later
			if (rawServiceName.equals("WSS11EncService1")) {
				System.out.println("In WSS11EncService1 case");
				WSS11EncService1 service = new WSS11EncService1(wsdlURL,
						serviceName);
				service.addPort(servicePort,SOAPBinding.SOAP11HTTP_BINDING,endPtUrl);
				dispSOAPMsg = service.createDispatch(servicePort,
						SOAPMessage.class, Mode.MESSAGE);
				if (msgId.equals("multiHeaders")) {
					updateSOAPMessage(soapReq);
				}
			}
			if (rawServiceName.equals("WSS11EncService2")) {
				System.out.println("In WSS11EncService2 case");
				WSS11EncService2 service = new WSS11EncService2(wsdlURL,
						serviceName);
				service.addPort(servicePort,SOAPBinding.SOAP11HTTP_BINDING,endPtUrl);
				dispSOAPMsg = service.createDispatch(servicePort,
						SOAPMessage.class, Mode.MESSAGE);
			}
			
			Map<String, Object> requestContext = dispSOAPMsg
					.getRequestContext();
			if (id != null) {
				System.out.println("Setting id: " + id + " with password: "
						+ pw);
				requestContext.put("ws-security.username", id);
				requestContext.put("ws-security.password", pw);
			}

			// may want to make this set the properties some times, and other
			// times use the defaults - tbd
			String strServerDir = System.getProperty("server.config.dir")
					.replace('\\', '/');

			System.out.println("Service Client request Header: "
					+ soapReq.getSOAPHeader());
			System.out.println("Service Client request Body: "
					+ soapReq.getSOAPBody());
			System.out.println("Outgoing msg contains: " + msgToBeUsed);
			System.out.println(clientWsdl) ;

			SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);
			System.out.println("DEBUG: Response SOAPMessage:");
            soapResp.writeTo(System.out);

			String answer = soapResp.getSOAPBody().getTextContent();

			PrintWriter rsp = response.getWriter();
			System.out.println("Answer received: " + answer);

			rsp.print("<html><head><title>CXF WSS 11 Encryption Service Cleint</title></head><body>");
			rsp.print("<p>Request: CXF WSS 11 Encryption Service Client</p>");
			rsp.print("<p>Response: " + answer + "</p>");
			rsp.print("</body></html>");

		} catch (Exception ex) {
			ex.printStackTrace();
			PrintWriter rsp = response.getWriter();
			rsp.print("<html><head><title>CXF WSS 11 Encryption Service Cleint</title></head><body>");
			rsp.print("<p>Request: CXF WSS 11 Encryption Service Client</p>");
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
