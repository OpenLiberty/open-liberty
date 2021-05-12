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

package com.ibm.ws.wssecurity.fat.callerclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;

import javax.crypto.Cipher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.soap.SOAPBinding;

import test.libertyfat.caller.contract.FatBAC01Service;
import test.libertyfat.caller.contract.FatBAC02Service;
import test.libertyfat.caller.contract.FatBAC03Service;
import test.libertyfat.caller.contract.FatBAC04Service;
import test.libertyfat.caller.contract.FatBAC05Service;
import test.libertyfat.caller.contract.FatBAC06Service;
import test.libertyfat.caller.contract.FatBAC07Service;
import test.libertyfat.caller.contract.FatBAC08Service;

/**
 * Servlet implementation class CxfCallerSvcClient
 */
//@WebServlet("/CxfCallerSvcClient")
public class CxfCallerSvcClient extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://caller.libertyfat.test/contract";

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
    String callerPolicy = null;
    URL wsdlURL = null;
    String httpProtocal = "http:";
    String endPoint = null;
    String methodFull = null;
    SOAPMessage soapReq = null;

    String untID = "";
    String untPassword = "";

    private StringReader reqMsg = null;
    static boolean unlimitCryptoKeyLength = false;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfCallerSvcClient() {
        super();
        // Check the JDK supports crypto key length > 128
        try {
            unlimitCryptoKeyLength = Cipher.getMaxAllowedKeyLength("RC5") > 128;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doWorker(HttpServletRequest request,
                            HttpServletResponse response) throws ServletException, IOException {

        getTestParameters(request); // Get all the testing parameters from the test-client request

        endPoint = httpProtocal + "//localhost:"
                   + thePort
                   //+ "9085"
                   + "/callertoken/"
                   + rawServiceName;
        wsdlLocation = endPoint + "?wsdl";
        wsdlURL = new URL(wsdlLocation);
        System.out.println("CxfCallerSvcClient: wsdl:" + wsdlLocation);
        System.out.println("thisMethod:" + thisMethod +
                           " callerPolicy:" + callerPolicy +
                           " testMode:" + testMode +
                           " methodFull:" + methodFull +
                           " untID:" + untID +
                           " untPassword:" + untPassword +
                           " serviceName:" + serviceName +
                           " servicePort:" + servicePort +
                           " servername:" + serverName);

        // This piece of code is a work-around for a bug defect 89493
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
 * System.out.println("set javax.net.ssl.trustStorePassword to " + "LibertyServer");
 * }
 */

        prepareServiceClientReqMsg();

        // call the test case to verify the output
        try {
            wsdlURL = new URL(wsdlLocation);
            Service service = null;
            String strExpect = null;
            String strSubErrMsg = "Not known what error message yet";
            if (thisMethod.equals("testCxfCallerHttpPolicy")) {
                service = new FatBAC01Service(wsdlURL, serviceName);
                strExpect = "Liberty Fat Caller bac01(" + untID + ")";
            } else if (thisMethod.equals("testCxfCallerHttpsPolicy")) {
                service = new FatBAC02Service(wsdlURL, serviceName);
                strExpect = "Liberty Fat Caller bac02(" + untID + ")";
                strSubErrMsg = "The security token could not be authenticated or authorized (Unexpected number of certificates: 0)";
            } else if (thisMethod.equals("testCxfCallerNoPolicy")) {
                service = new FatBAC03Service(wsdlURL, serviceName);
                strExpect = "Liberty Fat Caller bac03(" + untID + ")";
            } else if (thisMethod.equals("testCxfCallerHttpsNoUntPolicy")) {
                service = new FatBAC04Service(wsdlURL, serviceName);
                strExpect = "Liberty Fat Caller bac04(" + untID + ")";
                //strSubErrMsg = "There is no Username token in the message to process caller.";
                strSubErrMsg = "The security token could not be authenticated or authorized (UsernameToken is missing)";

                // msg is There is no username token in the message to process the caller
                //    in nlsprops
                if (!thisMethod.equals(methodFull)) { // Test this test in x509 Caller
                    //strSubErrMsg = "There is no Asymmetric signature token exists in the message";
                    strSubErrMsg = "The security token could not be authenticated or authorized (Unexpected number of certificates: 0)";
                }
            } else if (thisMethod.equals("testCxfCallerX509TokenPolicy")) {
                service = new FatBAC05Service(wsdlURL, serviceName);
                strExpect = "Liberty Fat Caller bac05(" + untID + ")";
            } else if (thisMethod.equals("testCxfCallerX509TransportEndorsingPolicy")) {
                service = new FatBAC06Service(wsdlURL, serviceName);
                strExpect = "Liberty Fat Caller bac06(" + untID + ")";
            } else if (thisMethod.equals("testCxfCallerSymmetricEndorsingPolicy")) {
                service = new FatBAC07Service(wsdlURL, serviceName);
                strExpect = "Liberty Fat Caller bac07(" + untID + ")";
            } else if (thisMethod.equals("testCxfCallerSymmetricEndorsingTLSPolicy")) {
                service = new FatBAC08Service(wsdlURL, serviceName);
                strExpect = "Liberty Fat Caller bac08(" + untID + ")";
            }
            if (service == null) {
                throw new Exception("thisMethod '" + thisMethod + "' did not get a Service. Test cases error.");
            }
            testCxfService(request, response, service, strExpect, strSubErrMsg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfService(HttpServletRequest request,
                                  HttpServletResponse response,
                                  javax.xml.ws.Service service,
                                  String strExpect) throws ServletException, IOException {
        testCxfService(request, response, service, strExpect, (String) null);
        return;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfService(HttpServletRequest request,
                                  HttpServletResponse response,
                                  javax.xml.ws.Service service,
                                  String strExpect, String strSubErrMsg) throws ServletException, IOException {
        if (testMode.startsWith("positive")) {
            testCxfPositiveMigService(request, response, service, strExpect, strSubErrMsg);
        } else {
            testCxfNegativeMigService(request, response, service, strSubErrMsg);
        }

        return;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfPositiveMigService(HttpServletRequest request,
                                             HttpServletResponse response,
                                             javax.xml.ws.Service service,
                                             String strExpect,
                                             String strSubErrMsg) throws ServletException, IOException {

        try {

            if (endPoint.contains("9085")) { // This piece of code only needed when we try to use tcpmon to debug. It's only in HTTP
                service.addPort(servicePort,
                                SOAPBinding.SOAP11HTTP_BINDING,
                                endPoint);
            }

            Dispatch<SOAPMessage> dispSOAPMsg = service.createDispatch(servicePort,
                                                                       SOAPMessage.class,
                                                                       Mode.MESSAGE);

            // set properties into requestContext when necessary
            if (untID != null && untID.length() > 0) {
                Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
                System.out.println("Setting untID: " + untID + " with password: " + untPassword);
                requestContext.put("ws-security.username", untID);
                requestContext.put("ws-security.password", untPassword);
            }

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

            String answer = soapResp.getSOAPBody().getTextContent();
            System.out.println(thisMethod + ":answer:" + answer);

            if (thisMethod.contains("Replay")) {
                try {
                    testMode = "negative"; // replay is an attack
                    soapResp = dispSOAPMsg.invoke(soapReq);

                    answer = soapResp.getSOAPBody().getTextContent();
                    System.out.println(thisMethod + ":Replay:answer:" + answer);
                    printUnexpectingResult(response, answer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    printExpectingException(response, ex, strSubErrMsg);
                }
            } else {
                printExpectingResult(response, answer, strExpect);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            printUnexpectingException(response, ex);
        }

        return;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfNegativeMigService(HttpServletRequest request,
                                             HttpServletResponse response,
                                             javax.xml.ws.Service service,
                                             String strSubErrMsg) throws ServletException, IOException {

        try {

            if (endPoint.contains("9085")) { // This piece of code only needed when we try to use tcpmon to debug. It's only in HTTP
                service.addPort(servicePort,
                                SOAPBinding.SOAP11HTTP_BINDING,
                                endPoint);
            }

            Dispatch<SOAPMessage> dispSOAPMsg = service.createDispatch(servicePort,
                                                                       SOAPMessage.class,
                                                                       Mode.MESSAGE);

            // set properties into requestContext when necessary
            if (untID != null && untID.length() > 0) {
                Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
                System.out.println("Setting untID: " + untID + " with password: " + untPassword);
                requestContext.put("ws-security.username", untID);
                requestContext.put("ws-security.password", untPassword);
            }

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

            String answer = soapResp.getSOAPBody().getTextContent();
            System.out.println(thisMethod + "@AV999 :answer:" + answer);

            printUnexpectingResult(response, answer);

        } catch (Exception ex) {
            ex.printStackTrace();
            printExpectingException(response, ex, strSubErrMsg);
        }

        return;
    }

    // get the parameters from test-client request
    void getTestParameters(HttpServletRequest request) {
        // Extract valuse sent by the client
        try {
            serverName = request.getParameter("serverName");
            System.out.println("gkuo: servername:" + serverName);
            thisMethod = request.getParameter("thisMethod");
            callerPolicy = request.getParameter("callerPolicy");
            testMode = request.getParameter("testMode");
            httpPortNum = request.getParameter("httpDefaultPort");
            httpSecurePortNum = request.getParameter("httpSecureDefaultPort");
            id = request.getParameter("id");
            pw = request.getParameter("pw");
            rawServiceName = request.getParameter("serviceName");
            methodFull = request.getParameter("methodFull");
            serviceName = new QName(SERVICE_NS, rawServiceName);
            servicePort = new QName(SERVICE_NS, request.getParameter("servicePort"));

            untID = request.getParameter("untID");
            if (untID == null)
                untID = "";
            untPassword = request.getParameter("untPassword");

            if (httpSecurePortNum == null || httpSecurePortNum.length() == 0) {
                httpProtocal = "http:";
                thePort = httpPortNum;
            } else {
                thePort = httpSecurePortNum;
                httpProtocal = "https:";
            }
        } catch (Exception ex) {
            System.out.println("Failed to find all required parameters");
            ex.printStackTrace();
        }
    }

    void prepareServiceClientReqMsg() {
        // prepare the request message
        try {
            String strSoapEnv = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                "<soapenv:Header></soapenv:Header>" +
                                "<soapenv:Body xmlns=\"http://caller.liberty.test/types\"><invoke>" +
                                callerPolicy + ":" + testMode + ":" + methodFull + // add the callerPolicy, testMode and methodFull for easier debugging 
                                "</invoke></soapenv:Body></soapenv:Envelope>";
            reqMsg = new StringReader(strSoapEnv);
            Source src = new StreamSource(reqMsg);
            MessageFactory factory = MessageFactory.newInstance();
            soapReq = factory.createMessage();
            soapReq.getSOAPPart().setContent(src);
            soapReq.saveChanges();

        } catch (Exception ex) {
            System.out.println("Failed to prepare the request message");
            ex.printStackTrace();
        }
    }

    // print results
    // when we expect service-provider respond without Exception.
    // But double check the response has the exspecting string
    void printExpectingResult(HttpServletResponse response, String answer, String strExpect) throws IOException {

        PrintWriter rsp = getPrintWriter(response);
        rsp.print("<p>pass:" + (strExpect.contains(answer)));
        rsp.print("::" + rawServiceName + "</p>");
        rsp.print("<p>answer:" + answer + "</p>");
        rsp.print("<p>expect:" + strExpect + "</p>");
        endPrint(rsp);
    }

    // print results
    // when we do not expect service-provider  to respond
    void printUnexpectingResult(HttpServletResponse response, String answer) throws IOException {

        PrintWriter rsp = getPrintWriter(response);
        rsp.print("<p>pass:false");
        rsp.print("::" + rawServiceName + "</p>");
        rsp.print("<p>answer:" + answer + "</p>");
        rsp.print("<p>expect to get an Exception but none received</p>");
        endPrint(rsp);
    }

    // print results
    // when we expect service-provider throws an Exception.
    // But double check the exception to have some specified sub-message
    void printExpectingException(HttpServletResponse response, Exception ex, String strSubErrMsg) throws IOException {
        PrintWriter rsp = getPrintWriter(response);
        String strMsg = ex.getMessage();
        if (strMsg == null)
            strMsg = "";
        rsp.print("<p>pass:" + strMsg.contains(strSubErrMsg));
        rsp.print("::" + rawServiceName + "</p>");
        rsp.print("<p>Exception:ClassName:" + ex.getClass().getName() + "</p>");
        rsp.print("<p>Message:" + ex.getMessage() + "</p>");
        endPrint(rsp);

    }

    // print results
    // when we do not expect service-provider throws an Exception but it does
    void printUnexpectingException(HttpServletResponse response, Exception ex) throws IOException {
        PrintWriter rsp = getPrintWriter(response);
        rsp.print("<p>pass:" + "false");
        rsp.print("::" + rawServiceName + "</p>");
        rsp.print("<p>Should not throw exception but get:" + ex.getClass().getName() + "</p>");
        rsp.print("<p>Message:" + ex.getMessage() + "</p>");
        endPrint(rsp);
    }

    PrintWriter getPrintWriter(HttpServletResponse response) throws IOException {
        PrintWriter rsp = response.getWriter();
        rsp.print("<html><head/><body>");
        return rsp;
    }

    void endPrint(PrintWriter rsp) throws IOException {
        rsp.print("<p>m:" + thisMethod + "</p>");
        rsp.print("<p>policy:" + callerPolicy + "</p>");
        rsp.print("<p>" + testMode + "</p>");
        rsp.print("</body></html>");
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
