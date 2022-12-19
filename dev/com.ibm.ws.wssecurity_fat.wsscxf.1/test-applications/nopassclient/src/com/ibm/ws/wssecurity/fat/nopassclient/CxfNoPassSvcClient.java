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

package com.ibm.ws.wssecurity.fat.nopassclient;

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

import test.wssecfat.NoPassService;

/**
 * Servlet implementation class CxfNoPassSvcClient
 */
@WebServlet("/CxfNoPassSvcClient")
public class CxfNoPassSvcClient extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://wssecfat.test";
    String serverName = null;
    String wsdlLocation = "";
    String httpPortNum = null;
    String httpSecurePortNum = null;
    String thePort = "";
    QName serviceName = null;
    QName servicePort = null;
    String rawServiceName = null;
    String id = null;
    String pw = null;
    String thisMethod = null;
    String testMode = null;
    URL wsdlURL = null;
    String httpProtocal = "http:";

    private StringReader reqMsg = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfNoPassSvcClient() {
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
            serverName = request.getParameter("serverName");
            System.out.println("gkuo: servername:" + serverName);
            thisMethod = request.getParameter("thisMethod");
            testMode = request.getParameter("testMode");
            httpPortNum = request.getParameter("httpDefaultPort");
            httpSecurePortNum = request.getParameter("httpSecureDefaultPort");
            id = request.getParameter("id");
            pw = request.getParameter("pw");
            rawServiceName = request.getParameter("serviceName");
            serviceName = new QName(SERVICE_NS, rawServiceName);
            servicePort = new QName(SERVICE_NS, request.getParameter("servicePort"));
            if (httpSecurePortNum == null || httpSecurePortNum.length() == 0) {
                thePort = httpPortNum;
            } else {
                thePort = httpSecurePortNum;
                httpProtocal = "https:";
            }
        } catch (Exception ex) {
            System.out.println("Failed to find all required parameters");
            ex.printStackTrace();
        }

        wsdlLocation = httpProtocal + "//localhost:"
                       + thePort
                       //+ "9085"
                       + "/nopassunt/"
                       + rawServiceName + "?wsdl";
        wsdlURL = new URL(wsdlLocation);
        System.out.println("CxfNoPassSvcClient: wsdl:" + wsdlLocation);
        System.out.println("thisMethod:" + thisMethod +
                           " testMode:" + testMode +
                           " serviceName:" + serviceName +
                           " servicePort:" + servicePort +
                           " servername:" + serverName);

        try {
            if (thisMethod.startsWith("testCxfNoPassService")) {
                // testCxfNoPassService & testCxfNoPassServiceBadUser
                testCxfNoPassService(request, response);
            }

        } catch (Exception ex) {
        }

        return;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfNoPassService(HttpServletRequest request,
                                        HttpServletResponse response) throws ServletException, IOException {

        try {

            reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header></soapenv:Header><soapenv:Body xmlns=\"http://basicplcy.wssecfvt.test/types\"><invoke>WSSECFVT CXF Service Client</invoke></soapenv:Body></soapenv:Envelope>");
            Source src = new StreamSource(reqMsg);
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapReq = factory.createMessage();
            soapReq.getSOAPPart().setContent(src);
            soapReq.saveChanges();
            Dispatch<SOAPMessage> dispSOAPMsg = null;

/*
 * if( httpProtocal.equals("https:")){
 * // Let set up some SSL attribute. This is a bug-to-be-fixed, customers do not need to do so.
 * String strServerDir = System.getProperty("server.config.dir").replace('\\', '/');
 * String strJksLocation = strServerDir + "/sslServerTrust.jks";
 * 
 * System.setProperty("javax.net.ssl.trustStore" , strJksLocation);
 * System.setProperty("javax.net.ssl.trustStorePassword", "LibertyServer");
 * 
 * System.out.println("set javax.net.ssl.trustStore to " + strJksLocation);
 * System.out.println("set javax.net.ssl.trustStorePassword to " + "LibertyClient");
 * }
 */
            wsdlURL = new URL(wsdlLocation);
            NoPassService service = new NoPassService(wsdlURL, serviceName);
            dispSOAPMsg = service.createDispatch(servicePort,
                                                 SOAPMessage.class,
                                                 Mode.MESSAGE);

            //
            Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();

            if (id != null) {
                requestContext.put("ws-security.username", id);
            }
            if (pw != null) {
                requestContext.put("ws-security.password", pw);
            }

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

            String answer = soapResp.getSOAPBody().getTextContent();
            System.out.println(thisMethod + ":answer:" + answer);
            PrintWriter rsp = response.getWriter();

            String strExpect = "WSSECFVT NoPass Version: 1.0-1.0";

            rsp.print("<html><head/><body>");
            rsp.print("<p>pass:" + (strExpect.contains(answer) && testMode.equals("positive")));
            rsp.print("::" + rawServiceName + "</p>");
            rsp.print("<p>answer:" + answer + "</p>");
            rsp.print("<p>expect:" + strExpect + "</p>");
            rsp.print("<p>m:" + thisMethod + "</p>");
            rsp.print("<p>" + testMode + "</p>");
            rsp.print("</body></html>");

        } catch (Exception ex) {

            PrintWriter rsp = response.getWriter();
            if (testMode == null || testMode.equals("postive")) {
                ex.printStackTrace();
                rsp.print("<html><head/><body>");
                rsp.print("<p>pass:" + "false");
                rsp.print("::" + rawServiceName + "</p>");
                rsp.print("<p>Exception:ClassName:" + ex.getClass().getName() + "</p>");
                rsp.print("<p>Message:" + ex.getMessage() + "</p>");
                rsp.print("<p>m:" + thisMethod + "</p>");
                rsp.print("<p>" + testMode + "</p>");
                rsp.print("</body></html>");
            } else {
                String strMessage = ex.getMessage();
                System.out.println(" getAnException Message:" + strMessage);
                if (strMessage == null)
                    strMessage = "";
                String strExpect = "The security token could not be authenticated or authorized";
                rsp.print("<html><head/><body>");
                rsp.print("<p>pass:" + strMessage.contains(strExpect));
                rsp.print("::" + rawServiceName + "</p>");
                rsp.print("<p>Exception:ClassName:" + ex.getClass().getName() + "</p>");
                rsp.print("<p>Message:" + ex.getMessage() + "</p>");
                rsp.print("<p>m:" + thisMethod + "</p>");
                rsp.print("<p>t:" + testMode + "</p>");
                rsp.print("</body></html>");
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
