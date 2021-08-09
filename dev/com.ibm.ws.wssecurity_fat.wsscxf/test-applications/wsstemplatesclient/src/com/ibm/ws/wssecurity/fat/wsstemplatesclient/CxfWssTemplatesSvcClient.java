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

package com.ibm.ws.wssecurity.fat.wsstemplatesclient;

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

import test.wssecfvt.wsstemplates.WSSTemplatesService1 ;
import test.wssecfvt.wsstemplates.WSSTemplatesService2 ;
import test.wssecfvt.wsstemplates.WSSTemplatesService3 ;
import test.wssecfvt.wsstemplates.WSSTemplatesService4 ;
import test.wssecfvt.wsstemplates.WSSTemplatesService5 ;
import test.wssecfvt.wsstemplates.WSSTemplatesService6 ;

/**
 * Servlet implementation class CxfWssTemplatesSvcClient
 */
@WebServlet("/CxfWssTemplatesSvcClient")
public class CxfWssTemplatesSvcClient extends HttpServlet {

	private static final String SERVICE_NS = "http://wsstemplates.wssecfvt.test";
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
    private static String managedClient = null ;

	static String simpleSoapBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+ "<soapenv:Header/>"
			+ "<soapenv:Body xmlns=\"http://wsstemplates.wssecfvt.test/types\">"
			+ "<invoke>WSSECFVT Simple Soap Msg</invoke>"
			+ "</soapenv:Body>"
			+ "</soapenv:Envelope>";
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CxfWssTemplatesSvcClient() {
		super();
	}
	
	// create a request message 
	private SOAPMessage createSOAPMessage() {
		
        SOAPMessage message = null;
        
        try {
        	StringReader reqMsg = new StringReader(simpleSoapBody);
			Source src = new StreamSource(reqMsg);
			MessageFactory factory = MessageFactory.newInstance();
			message = factory.createMessage();
			message.getSOAPPart().setContent(src);
			message.saveChanges();
        	
        } catch (Exception ex) {
        	System.out.println("createSOAPMessage(): Failed to create SOAP message");
            ex.printStackTrace();
        }
        
        return message;
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
            managedClient = request.getParameter("managedClient") ;
			if (httpSecurePortNum.equals("")) {
				thePort = httpPortNum;
				theType = "http";
			} else {
				thePort = httpSecurePortNum;
				theType = "https";

	            // set up some SSL attribute. This is a bug-to-be-fixed, customers do not need to do so.
/*
	            String strServerDir   = System.getProperty("server.config.dir").replace('\\', '/');
	            String strJksLocation = strServerDir + "/sslServerTrust.jks";
	            
	            System.setProperty("javax.net.ssl.trustStore"        , strJksLocation);
	            System.setProperty("javax.net.ssl.trustStorePassword", "LibertyServer");
	            
	            System.out.println("set javax.net.ssl.trustStore to " + strJksLocation);
	            System.out.println("set javax.net.ssl.trustStorePassword to " + "LibertyServer");
*/
			}

			System.out.println("clientWsdl:" + clientWsdl + ":end") ;
		} catch (Exception ex) {
			System.out.println("Failed to find all required parameters");
			ex.printStackTrace();
			ServletException e1 = new ServletException(
					"Failed to find all required parameters");
			throw e1;
		}
		
		System.out.println("rawWsdl:" + clientWsdl + ":rawWsdl") ; 
		
		wsdlLocation = theType + "://localhost:" + thePort + "/wsstemplates/"
					+ rawServiceName + "?wsdl";
		if (!clientWsdl.equals("")) {
			wsdlLocation = "file:///" + clientWsdl;
		}

		URL wsdlURL = new URL(wsdlLocation);
		
		System.out.println("wsdlLocation: " + wsdlLocation);
		System.out.println("wsdlUrl: " + wsdlURL);

		try {
			System.out.println("Will send appropriate message for msgId: " + msgId);
			
			if (msgId.equals("someFutureSpecificMsg")) {
				msgToBeUsed = simpleSoapBody; // some future msg
			} else {
				msgToBeUsed = simpleSoapBody;
			}

			SOAPMessage soapReq = createSOAPMessage();
			
	        Dispatch<SOAPMessage> dispSOAPMsg = null;
			String endPtUrl = theType + "://localhost:" + thePort + "/wsstemplates/" + rawServiceName ;

			// setup to handle multiple services later
			if (rawServiceName.equals("WSSTemplatesService1")) {
				System.out.println("In WSSTemplatesService1 case");
				WSSTemplatesService1 service = new WSSTemplatesService1(wsdlURL,
						serviceName);
				service.addPort(servicePort,SOAPBinding.SOAP11HTTP_BINDING,endPtUrl);
				dispSOAPMsg = service.createDispatch(servicePort,
						SOAPMessage.class, Mode.MESSAGE);
			}
			if (rawServiceName.equals("WSSTemplatesService2")) {
				System.out.println("In WSSTemplatesService2 case");
				WSSTemplatesService2 service = new WSSTemplatesService2(wsdlURL,
						serviceName);
				service.addPort(servicePort,SOAPBinding.SOAP11HTTP_BINDING,endPtUrl);
				dispSOAPMsg = service.createDispatch(servicePort,
						SOAPMessage.class, Mode.MESSAGE);
			}
			if (rawServiceName.equals("WSSTemplatesService3")) {
				System.out.println("In WSSTemplatesService3 case");
				WSSTemplatesService3 service = new WSSTemplatesService3(wsdlURL,
						serviceName);
				service.addPort(servicePort,SOAPBinding.SOAP11HTTP_BINDING,endPtUrl);
				dispSOAPMsg = service.createDispatch(servicePort,
						SOAPMessage.class, Mode.MESSAGE);
			}
			if (rawServiceName.equals("WSSTemplatesService4")) {
				System.out.println("In WSSTemplatesService4 case");
				WSSTemplatesService4 service = new WSSTemplatesService4(wsdlURL,
						serviceName);
				service.addPort(servicePort,SOAPBinding.SOAP11HTTP_BINDING,endPtUrl);
				dispSOAPMsg = service.createDispatch(servicePort,
						SOAPMessage.class, Mode.MESSAGE);
			}
			if (rawServiceName.equals("WSSTemplatesService5")) {
				System.out.println("In WSSTemplatesService5 case");
				WSSTemplatesService5 service = new WSSTemplatesService5(wsdlURL,
						serviceName);
				service.addPort(servicePort,SOAPBinding.SOAP11HTTP_BINDING,endPtUrl);
				dispSOAPMsg = service.createDispatch(servicePort,
						SOAPMessage.class, Mode.MESSAGE);
			}
			if (rawServiceName.equals("WSSTemplatesService6")) {
				System.out.println("In WSSTemplatesService6 case");
				WSSTemplatesService6 service = new WSSTemplatesService6(wsdlURL,
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

			System.out.println("Service Client request Header: "
					+ soapReq.getSOAPHeader());
			System.out.println("Service Client request Body: "
					+ soapReq.getSOAPBody().getTextContent());
			System.out.println("Outgoing msg contains: " + msgToBeUsed);
			System.out.println(clientWsdl) ;

			SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);
			System.out.println("DEBUG: Response SOAPMessage:");
            soapResp.writeTo(System.out);

			String answer = soapResp.getSOAPBody().getTextContent();

			PrintWriter rsp = response.getWriter();
			System.out.println("Answer received: " + answer);

			rsp.print("<html><head><title>CXF WSS Templates Test Service Client</title></head><body>");
			rsp.print("<p>Request: CXF WSS Templates Test Service Client</p>");
			rsp.print("<p>Response: " + answer + "</p>");
			rsp.print("</body></html>");

		} catch (Exception ex) {
			ex.printStackTrace();
			PrintWriter rsp = response.getWriter();
			rsp.print("<html><head><title>CXF WSS Templates Test Service Client</title></head><body>");
			rsp.print("<p>Request: CXF WSS Templates Test Service Client</p>");
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
