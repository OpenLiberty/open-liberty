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

package com.ibm.ws.wssecurity.fat.x509client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

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

import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;

import test.wssecfvt.basicplcy.FVTVersionBAXService;

/**
 * Servlet implementation class CxfX509SvcClient
 */
@WebServlet("/CxfX509SvcClient")
public class CxfX509SvcClient extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://basicplcy.wssecfvt.test";
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

    String endPoint = null;

    private StringReader reqMsg = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfX509SvcClient() {
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
            //System.out.println("gkuo: servername:" + serverName);
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
                       + "/x509token/"
                       + rawServiceName + "?wsdl";
        wsdlURL = new URL(wsdlLocation);
        System.out.println("CxfX509SvcClient: wsdl:" + wsdlLocation);
        System.out.println("thisMethod:" + thisMethod +
                           " testMode:" + testMode +
                           " serviceName:" + serviceName +
                           " servicePort:" + servicePort +
                           " servername:" + serverName);
        endPoint = httpProtocal + "//localhost:"
                   + thePort
                   // + "9085"
                   + "/x509token/"
                   + rawServiceName;
        try {
            if (thisMethod.equals("testCxfX509Service")) {
                testCxfX509Service(request, response);
            }

        } catch (Exception ex) {
        }

        return;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfX509Service(HttpServletRequest request,
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
            FVTVersionBAXService service = new FVTVersionBAXService(wsdlURL, serviceName);
            if (endPoint.contains("9085")) { // This piece of code only needed when we try to use tcpmon to debug. It's only in HTTP
                service.addPort(servicePort,
                                SOAPBinding.SOAP11HTTP_BINDING,
                                endPoint);
            }
            dispSOAPMsg = service.createDispatch(servicePort,
                                                 SOAPMessage.class,
                                                 Mode.MESSAGE);

            //
            Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
            //System.out.println("gkuo: servername:" + serverName);
            if (id != null) {
                requestContext.put("ws-security.username", id);
            }
            if (pw != null) {
                requestContext.put("ws-security.password", pw);
            }

            if (serverName.equals("com.ibm.ws.wssecurity_fat.x509_1")) {
                if (testMode.equals("positive")) {// test the cxf service client can override the server.xml
                    setClientX509KeyProps(requestContext);
                }
            } else if (serverName.equals("com.ibm.ws.wssecurity_fat.x509_2")) {
                //test Crypto object to be used for signature and encryption
                setCryptoObjectsOnRequestContext(requestContext);
            }

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

            String answer = soapResp.getSOAPBody().getTextContent();
            System.out.println(thisMethod + ":answer:" + answer);
            PrintWriter rsp = response.getWriter();

            String strExpect = "WSSECFVT X509 Version: 1.0-1.0";

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
                String strExpect = "No certificates were found for decryption";
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

    void setCryptoObjectsOnRequestContext(Map<String, Object> requestContext) throws Exception {

        // Load the keystore
        //Crypto crypto = new Merlin();
        requestContext.put("ws-security.callback-handler", "com.ibm.ws.wssecurity.example.cbh.CommonPasswordCallback");
        String strServerDir = System.getProperty("server.config.dir").replace('\\', '/');
        String strX509JksSignLocation = strServerDir + "x509ClientDefaultS.properties";

        //Crypto crypto = CryptoFactory.getInstance(strX509JksSignLocation);

        Crypto crypto = CryptoFactory.getInstance(getProperties(strX509JksSignLocation, strServerDir));
        requestContext.put("ws-security.signature.crypto", crypto);

        String strX509JksEncrLocation = strServerDir + "/x509ClientDefaultE.properties";

        Crypto crypto2 = CryptoFactory.getInstance(getProperties(strX509JksEncrLocation, strServerDir));
        requestContext.put("ws-security.encryption.crypto", crypto2);
        /*
         * KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
         * ClassLoader loader = Loader.getClassLoader(this.getClass());
         * 
         * 
         * String signKS = strServerDir.concat("/x509ClientDefault.jks");
         * InputStream input = Merlin.loadInputStream(loader, signKS);
         * keyStore.load(input, "LibertyX509Client".toCharArray());
         * ((Merlin)crypto).setKeyStore(keyStore);
         * 
         * // Load the truststore
         * Crypto processCrypto = new Merlin();
         * KeyStore keystore2 = KeyStore.getInstance(KeyStore.getDefaultType());
         * ClassLoader loader2 = Loader.getClassLoader(this.getClass());
         * String verifyKS = strServerDir.concat("/x509ClientDefault.jks");
         * InputStream input2 = Merlin.loadInputStream(loader, verifyKS);
         * keystore2.load(input2, "LibertyX509Client".toCharArray());
         * ((Merlin)crypto).setTrustStore(keystore2);
         */

    }

    void setClientX509KeyProps(Map<String, Object> requestContext) throws Exception {
        //System.out.println(" gkuo: set up client x509 properties");
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
        //System.out.println("gkuo: strX509JksLocation:" + strX509JksLocation + " : " + fX509Jks.exists());
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
        //System.out.println("gkuo: key" + strKey + " : " + strValueRight );

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
