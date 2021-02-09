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

package com.ibm.ws.wssecurity.fat.x509Asyncclient;

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
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

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
import javax.xml.ws.Response;

import com.ibm.websphere.simplicity.log.Log;

import test.wssecfvt.x509async.UrnX509AsyncType;
import test.wssecfvt.x509async.X509AsyncService;
import test.wssecfvt.x509async.types.RequestString;
import test.wssecfvt.x509async.types.ResponseString;
import test.wssecfvt.x509async.types.ObjectFactory;

/**
 * Servlet implementation class CxfX509AsyncSvcClient
 */
@WebServlet("/CxfX509AsyncSvcClient")
public class CxfX509AsyncSvcClient extends HttpServlet {

    static String strJksLocation = "sslServerTrust.jks";
    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://x509async.wssecfvt.test";
    private static String testName = "" ;
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
                                   + "<soapenv:Body xmlns=\"http://x509async.wssecfvt.test/types\">"
                                   + "<invokeAsync>WSSECFVT Simple Soap Msg</invokeAsync>"
                                   + "</soapenv:Body>"
                                   + "</soapenv:Envelope>";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfX509AsyncSvcClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
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
            System.out.println("clientWsdl:" + clientWsdl + ":end");
        } catch (Exception ex) {
            System.out.println("Failed to find all required parameters");
            ex.printStackTrace();
            ServletException e1 = new ServletException(
                            "Failed to find all required parameters");
            throw e1;
        }
        System.out.println("rawWsdl:" + clientWsdl + ":rawWsdl");

        wsdlLocation = theType + "://localhost:" + thePort + "/x509aSync/"
                       + rawServiceName + "?wsdl";

        if (!clientWsdl.equals("")) {
            wsdlLocation = "file:///" + clientWsdl;
        }

        URL wsdlURL = new URL(wsdlLocation);
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

            UrnX509AsyncType servicePort2 = null;
            // setup to handle multiple services later
            if (rawServiceName.equals("X509AsyncService")) {
                System.out.println("In X509AsyncService case");
                X509AsyncService service = new X509AsyncService(wsdlURL,
                                            serviceName);
                servicePort2 = service.getX509AsyncPort();

            }

            Map<String, Object> requestContext = ((javax.xml.ws.BindingProvider) servicePort2).getRequestContext();

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

            RequestString reqString = new RequestString();
            reqString.setStringreq(simpleSoapBody);
            
            PrintWriter rsp = response.getWriter();
            String answer = "" ;
            
            if (testName.contains("Block") ) {
              ResponseString soapResp = null;
              Response<ResponseString> responseRef = servicePort2.invokeAsync(reqString);
              
              if (request.getParameter("testName").contains("testCxfAsyncInvokeNonBlocking")) {
                System.out.println("Non-Blocking Case");
                int waitCount = 0;
                while (!responseRef.isDone() && waitCount < 15) {
                    Thread.sleep(1000);
                    waitCount++;
                }
                if (responseRef.isDone() && waitCount < 15) {
                    soapResp = responseRef.get();
                } else {
                    rsp.print("<html><head><title>CXF x509 Async Service Cleint</title></head><body>");
                    rsp.print("<p>Request: CXF x509 Async Service Client</p>");
                    rsp.print("<p>Response: TIMEOUT: Exited wait loop with no indication that request has completed </p>");
                    rsp.print("</body></html>");
                }
              } else {
                
                    System.out.println("Blocking Case");
                    try {
                        soapResp = responseRef.get(120L, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (TimeoutException te) {
                        te.printStackTrace();
                        rsp.print("<html><head><title>CXF x509 Async Service Cleint</title></head><body>");
                        rsp.print("<p>Request: CXF x509 Async Service Client</p>");
                        rsp.print("<p>Response: TIMEOUT: " + te + "</p>");
                        rsp.print("</body></html>");
                    }
              }
              answer = soapResp.getStringres();
              
            } else {
                System.out.println("Callback Case");
                TestAsyncHandler testAsyncHandler = new TestAsyncHandler() ;
                Future<?> fresponseRef = servicePort2.invokeAsync(reqString, testAsyncHandler);
                int waitCount = 0;
                while (!fresponseRef.isDone() && waitCount < 15) {
                    Thread.sleep(1000);
                    waitCount++;
                }
                if (fresponseRef.isDone() && waitCount < 15) {
                    answer = testAsyncHandler.getResponse();
                } else {
                    rsp.print("<html><head><title>CXF x509 Async Service Cleint</title></head><body>");
                    rsp.print("<p>Request: CXF x509 Async Service Client</p>");
                    rsp.print("<p>Response: TIMEOUT: Exited wait loop with no indication that request has completed </p>");
                    rsp.print("</body></html>");
                }
            }

            System.out.println("Answer received: " + answer);

            rsp.print("<html><head><title>CXF x509 Async Service Cleint</title></head><body>");
            rsp.print("<p>Request: CXF x509 Async Service Client</p>");
            rsp.print("<p>Response: " + answer + "</p>");
            rsp.print("</body></html>");

        } catch (Exception ex) {
            ex.printStackTrace();
            PrintWriter rsp = response.getWriter();
            rsp.print("<html><head><title>CXF x509 Async Service Cleint</title></head><body>");
            rsp.print("<p>Request: CXF x509 Async Service Client</p>");
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
