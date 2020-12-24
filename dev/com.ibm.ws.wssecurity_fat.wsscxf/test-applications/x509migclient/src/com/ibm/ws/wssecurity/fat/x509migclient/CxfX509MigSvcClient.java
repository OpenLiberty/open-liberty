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

package com.ibm.ws.wssecurity.fat.x509migclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.crypto.Cipher;
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
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.soap.SOAPBinding;

import test.libertyfat.x509mig.contract.FatBAX01Service;
import test.libertyfat.x509mig.contract.FatBAX02Service;
import test.libertyfat.x509mig.contract.FatBAX03Service;
import test.libertyfat.x509mig.contract.FatBAX04Service;
import test.libertyfat.x509mig.contract.FatBAX05Service;
import test.libertyfat.x509mig.contract.FatBAX06Service;
import test.libertyfat.x509mig.contract.FatBAX07Service;
import test.libertyfat.x509mig.contract.FatBAX08Service;
import test.libertyfat.x509mig.contract.FatBAX09Service;
import test.libertyfat.x509mig.contract.FatBAX10Service;
import test.libertyfat.x509mig.contract.FatBAX11Service;
import test.libertyfat.x509mig.contract.FatBAX12Service;
import test.libertyfat.x509mig.contract.FatBAX13Service;
import test.libertyfat.x509mig.contract.FatBAX14Service;
import test.libertyfat.x509mig.contract.FatBAX15Service;
import test.libertyfat.x509mig.contract.FatBAX16Service;
import test.libertyfat.x509mig.contract.FatBAX17Service;
import test.libertyfat.x509mig.contract.FatBAX18Service;
import test.libertyfat.x509mig.contract.FatBAX19Service;
import test.libertyfat.x509mig.contract.FatBAX20Service;
import test.libertyfat.x509mig.contract.FatBAX21Service;
import test.libertyfat.x509mig.contract.FatBAX22Service;
import test.libertyfat.x509mig.contract.FatBAX23Service;
import test.libertyfat.x509mig.contract.FatBAX24Service;
import test.libertyfat.x509mig.contract.FatBAX25Service;
import test.libertyfat.x509mig.contract.FatBAX26Service;
import test.libertyfat.x509mig.contract.FatBAX27Service;
import test.libertyfat.x509mig.contract.FatBAX28Service;
import test.libertyfat.x509mig.contract.FatBAX29Service;
import test.libertyfat.x509mig.contract.FatBAX30Service;
import test.libertyfat.x509mig.contract.FatBAX31Service;
import test.libertyfat.x509mig.contract.FatBAX32Service;
import test.libertyfat.x509mig.contract.FatBAX33Service;
import test.libertyfat.x509mig.contract.FatBAX34Service;
import test.libertyfat.x509mig.contract.FatBAX35Service;
import test.libertyfat.x509mig.contract.FatBAX36Service;

/**
 * Servlet implementation class CxfX509MigSvcClient
 */
@WebServlet("/CxfX509MigSvcClient")
public class CxfX509MigSvcClient extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://x509mig.libertyfat.test/contract";

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
    String x509Policy = null;
    URL wsdlURL = null;
    String httpProtocal = "http:";
    String endPoint = null;
    String methodFull = null;
    SOAPMessage soapReq = null;

    private StringReader reqMsg = null;
    static boolean unlimitCryptoKeyLength = false;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfX509MigSvcClient() {
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
                   + "/x509migtoken/"
                   + rawServiceName;
        wsdlLocation = endPoint + "?wsdl";
        wsdlURL = new URL(wsdlLocation);
        System.out.println("CxfX509MigSvcClient: wsdl:" + wsdlLocation);
        System.out.println("thisMethod:" + thisMethod +
                           " x509Policy:" + x509Policy +
                           " testMode:" + testMode +
                           " methodFull:" + methodFull +
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
 * // // Debugging
 * // System.setProperty( "javax.net.debug", "ssl");
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
            if (thisMethod.equals("testCxfX509KeyIdMigSymService")) {
                service = new FatBAX01Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax01";
                strSubErrMsg = "The signature method does not match the requirement";
            } else if (thisMethod.equals("testCxfX509AsymIssuerSerialMigService")) {
                service = new FatBAX02Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax02";
            } else if (thisMethod.equals("testCxfX509IssuerSerialMigSymService")) {
                service = new FatBAX03Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax03";
                strSubErrMsg = "The signature method does not match the requirement";
            } else if (thisMethod.equals("testCxfX509ThumbprintMigSymService")) {
                service = new FatBAX04Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax04";
            } else if (thisMethod.equals("testCxfX509KeyIdDerivedMigSymService")) {
                service = new FatBAX05Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax05";
            } else if (thisMethod.equals("testCxfX509ThumbprintDerivedMigSymService")) {
                service = new FatBAX06Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax06";
            } else if (thisMethod.equals("testCxfX509AsymThumbprintMigService")) {
                service = new FatBAX07Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax07";
            } else if (thisMethod.equals("testCxfX509AsymProtectTokensMigService")) {
                service = new FatBAX08Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax08";
            } else if (thisMethod.equals("testCxfX509ProtectTokensMigSymService")) {
                service = new FatBAX09Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax09";
            } else if (thisMethod.equals("testCxfX509TransportEndorsingMigService")) {
                service = new FatBAX10Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax10";
                strSubErrMsg = "Assertion of type {http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}HttpsToken could not be asserted";
            } else if (thisMethod.equals("testCxfX509TransportEndorsingSP11MigService")) {
                service = new FatBAX11Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax11";
                strSubErrMsg = "Assertion of type {http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}HttpsToken could not be asserted";
            } else if (thisMethod.equals("testCxfX509TransportSignedEndorsingMigService")) {
                service = new FatBAX12Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax12";
                strSubErrMsg = "Assertion of type {http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}HttpsToken could not be asserted";
            } else if (thisMethod.equals("testCxfX509TransportEndorsingEncryptedMigService")) {
                service = new FatBAX13Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax13";
                strSubErrMsg = "Assertion of type {http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}HttpsToken could not be asserted";
            } else if (thisMethod.equals("testCxfX509TransportSignedEndorsingEncryptedMigService")) {
                service = new FatBAX14Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax14";
                strSubErrMsg = "Assertion of type {http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}HttpsToken could not be asserted";
            } else if (thisMethod.equals("testCxfX509TransportSupportingSignedMigService")) {
                service = new FatBAX15Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax15";
                strSubErrMsg = "Assertion of type {http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}HttpsToken could not be asserted";
            } else if (thisMethod.equals("testCxfX509TransportKVTMigService")) {
                service = new FatBAX16Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax16";
                strSubErrMsg = "Assertion of type {http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}HttpsToken could not be asserted";
            } else if (thisMethod.equals("testCxfX509AsymmetricSignatureMigService")) {
                service = new FatBAX17Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax17";
            } else if (thisMethod.equals("testCxfX509AsymmetricSignatureReplayMigService")) {
                service = new FatBAX17Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax17";
                strSubErrMsg = "An invalid security token was provided (WSSecurityEngine: Invalid timestamp";
            } else if (thisMethod.equals("testCxfX509AsymmetricSignatureSP11MigService")) {
                service = new FatBAX18Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax18";
            } else if (thisMethod.equals("testCxfX509AsymmetricEncryptionMigService")) {
                service = new FatBAX19Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax19";
            } else if (thisMethod.equals("testWsAddressingService")) {
                service = new FatBAX20Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax20";
            } else if (thisMethod.equals("testWsComplexService")) {
                service = new FatBAX21Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax21";
            } else if (thisMethod.equals("testAsymmetricBasic256Service")) {
                service = new FatBAX22Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax22";
                strSubErrMsg = "Cannot encrypt data";
            } else if (thisMethod.equals("testWsComplexSP11Service")) {
                service = new FatBAX23Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax23";
            } else if (thisMethod.equals("testX509KeyIdentifierUNTService")) {
                service = new FatBAX24Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax24";
            } else if (thisMethod.equals("testX509SignatureConfirmService")) {
                service = new FatBAX25Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax25";
            } else if (thisMethod.equals("testAsymSignatureConfirmService")) {
                service = new FatBAX26Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax26";
            } else if (thisMethod.equals("testSymEncSignService")) {
                service = new FatBAX27Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax27";
            } else if (thisMethod.equals("testAsymEncSignService")) {
                service = new FatBAX28Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax28";
            } else if (thisMethod.equals("testSymEncSignStrictService")) {
                service = new FatBAX29Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax29";
            } else if (thisMethod.equals("testAsymEncSignStrictService")) {
                service = new FatBAX30Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax30";
            } else if (thisMethod.equals("testBasic192Service")) {
                service = new FatBAX31Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax31";
                strSubErrMsg = "Cannot encrypt data";
                if (unlimitCryptoKeyLength) {
                    strSubErrMsg = "xenc:EncryptionMethod/@Algorithm is not supported: http://www.w3.org/2001/04/xmlenc#aes192-cbc";
                }
            } else if (thisMethod.equals("testTripleDesService")) {
                service = new FatBAX32Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax32";
            } else if (thisMethod.equals("testInclusiveC14NService")) {
                service = new FatBAX33Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax33";
                strSubErrMsg = "SymmetricBinding: The digest method does not match the requirement";
            } else if (thisMethod.equals("testBasic128Service")) {
                service = new FatBAX34Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax34";
            } else if (thisMethod.equals("testSymmetricEndorsingUNTPolicy")) {
                service = new FatBAX35Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax35";
            } else if (thisMethod.equals("testSymSignatureTokenPolicy")) {
                service = new FatBAX36Service(wsdlURL, serviceName);
                strExpect = "LIBERTYFAT X509 bax36";
            }
            if (service == null) {
                throw new Exception("thisMethod '" + thisMethod + "' did not get a Service. Test cases error.");
            }
            testCxfX509MigService(request, response, service, strExpect, strSubErrMsg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfX509MigService(HttpServletRequest request,
                                         HttpServletResponse response,
                                         javax.xml.ws.Service service,
                                         String strExpect) throws ServletException, IOException {
        testCxfX509MigService(request, response, service, strExpect, (String) null);
        return;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfX509MigService(HttpServletRequest request,
                                         HttpServletResponse response,
                                         javax.xml.ws.Service service,
                                         String strExpect, String strSubErrMsg) throws ServletException, IOException {
        if (testMode.startsWith("positive")) {
            testCxfX509PositiveMigService(request, response, service, strExpect, strSubErrMsg);
        } else {
            testCxfX509NegativeMigService(request, response, service, strSubErrMsg);
        }

        return;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfX509PositiveMigService(HttpServletRequest request,
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
            // Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();

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
    protected void testCxfX509NegativeMigService(HttpServletRequest request,
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
            // Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

            String answer = soapResp.getSOAPBody().getTextContent();
            System.out.println(thisMethod + ":answer:" + answer);

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
            x509Policy = request.getParameter("x509Policy");
            testMode = request.getParameter("testMode");
            httpPortNum = request.getParameter("httpDefaultPort");
            httpSecurePortNum = request.getParameter("httpSecureDefaultPort");
            id = request.getParameter("id");
            pw = request.getParameter("pw");
            rawServiceName = request.getParameter("serviceName");
            methodFull = request.getParameter("methodFull");
            serviceName = new QName(SERVICE_NS, rawServiceName);
            servicePort = new QName(SERVICE_NS, request.getParameter("servicePort"));
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
                                "<soapenv:Body xmlns=\"http://x509mig.liberty.test/types\"><invoke>" +
                                x509Policy + ":" + testMode + ":" + methodFull + // add the x509Policy, testMode and methodFull for easier debugging 
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
        rsp.print("<p>policy:" + x509Policy + "</p>");
        rsp.print("<p>" + testMode + "</p>");
        rsp.print("</body></html>");
    }

    void setClientX509MigKeyProps(Map<String, Object> requestContext) throws Exception {
        System.out.println(" gkuo: set up client x509Mig negative properties");
        String strServerDir = System.getProperty("server.config.dir");
        strServerDir = strServerDir.replace('\\', '/');
        String strX509JksSignLocation = strServerDir + "/x509ClientDefaultS.properties";
        String strX509JksEncrLocation = strServerDir + "/x509ClientSecondE.properties"; // do the server.xml on purpose
        requestContext.put("ws-security.callback-handler", "com.ibm.ws.wssecurity.example.cbh.CommonPasswordCallback"); // The callback
        requestContext.put("ws-security.signature.properties", getProperties(strX509JksSignLocation, strServerDir));
        requestContext.put("ws-security.encryption.properties", getProperties(strX509JksEncrLocation, strServerDir));
    }

    Properties getProperties(String strX509JksLocation, String strServerDir) throws Exception {
        File fX509Jks = new File(strX509JksLocation);
        System.out.println("gkuo: strX509JksLocation:" + strX509JksLocation + " : " + fX509Jks.exists());
        Properties x509Props = new Properties();
        x509Props.load(new FileInputStream(fX509Jks));

        // update the hardcoded file location into right one
        String strKey = "org.apache.ws.security.crypto.merlin.keystore.file";
        String strValue = (String) x509Props.get(strKey);
        // if( strValue == null ) strValue = "";
        int index = strValue.lastIndexOf("/");
        if (index < 0) {
            index = strValue.lastIndexOf("\\");
        }
        if (index < 0) {
            index = 0;
        }
        String strValueRight = strServerDir + strValue.substring(index);
        x509Props.put(strKey, strValueRight);
        System.out.println("gkuo: key" + strKey + " : " + strValueRight);

        return x509Props;
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
