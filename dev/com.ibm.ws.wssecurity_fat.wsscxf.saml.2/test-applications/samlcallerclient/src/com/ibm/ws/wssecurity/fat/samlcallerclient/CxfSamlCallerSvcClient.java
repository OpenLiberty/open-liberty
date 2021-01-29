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

package com.ibm.ws.wssecurity.fat.samlcallerclient;

import java.io.IOException;


import java.io.File;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.net.URL;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

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
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.soap.SOAPBinding;
import javax.crypto.Cipher;

import test.libertyfat.caller.contract.FatSamlC01Service;
import test.libertyfat.caller.contract.FatSamlC02Service;
import test.libertyfat.caller.contract.FatSamlC02aService;
import test.libertyfat.caller.contract.FatSamlC03Service;
import test.libertyfat.caller.contract.FatSamlC04Service;
import test.libertyfat.caller.contract.FatSamlC05Service;
import test.libertyfat.caller.contract.FatSamlC06Service;
import test.libertyfat.caller.contract.FatSamlC07Service;
import test.libertyfat.caller.contract.FatSamlC08Service;


/**
 * Servlet implementation class CxfSamlCallerSvcClient
 */
//@WebServlet("/CxfSamlCallerSvcClient")
public class CxfSamlCallerSvcClient extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVICE_NS = "http://caller.libertyfat.test/contract";
    
    String serverName        = null;
    String wsdlLocation      = "";
    String httpPortNum       = null;
    String httpSecurePortNum = null;
    String thePort           = "";
    QName serviceName        = null;
    QName servicePort        = null;
    String rawServiceName    = null;
    String id                = null;
    String pw                = null;
    String thisMethod        = null;
    String testMode          = null;
    String callerPolicy      = null;
    URL wsdlURL              = null;
    String httpProtocal      = "http:";
    String endPoint          = null;
    String methodFull        = null;
    SOAPMessage soapReq      = null;

    String untID             = "";
    String untPassword       = "";

    private StringReader reqMsg = null;
    static boolean unlimitCryptoKeyLength = false; 


    /**
     * @see HttpServlet#HttpServlet()
     */
    public CxfSamlCallerSvcClient() {
        super();
        // Check the JDK supports crypto key length > 128
        try{
            unlimitCryptoKeyLength = Cipher.getMaxAllowedKeyLength("RC5") > 128;
        } catch( Exception e ){
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doWorker(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        getTestParameters( request); // Get all the testing parameters from the test-client request
       
        SOAPMessage soapResp = null;
        String answer = "";
        
       
        
        endPoint     = httpProtocal + "//localhost:" 
                       + thePort
                       //+ "9085"
                       + "/samlcallertoken/"
                       + rawServiceName;
        wsdlLocation = endPoint + "?wsdl";
        wsdlURL = new URL(wsdlLocation);
        
        
        System.out.println( "thisMethod:"    + thisMethod  + 
                            " callerPolicy:" + callerPolicy  + 
                            " testMode:"     + testMode    + 
                            " methodFull:"   + methodFull  +
                            " untID:"        + untID       + 
                            " untPassword:"  + untPassword + 
                            " serviceName:"  + serviceName +
                            " servicePort:"  + servicePort +
                            " servername:"   + serverName ); 
        //TODO Aruna: hardcoding few things here. Properly need to set these on the request
        String wsdlLocation2 = "http://localhost:" + thePort + "/samlcallertoken/FatSamlC01Service?wsdl";
        //rawServiceName = "FatSamlC01Service";
        //serviceName = new QName(SERVICE_NS, rawServiceName);
        //testMode = "positive";
        //servicePort = new  QName(SERVICE_NS, "SamlCallerToken01");
        
        System.out.println("@AV999 CxfSamlCallerSvcClient: wsdl:" + wsdlLocation + ", hard coded = " + 
                        wsdlLocation2);



        prepareServiceClientReqMsg();
                       
                       try {
                           Service service  = null;
                           String strExpect = null;
                           String strSubErrMsg = "Not known what error message yet";
                           
                           if(thisMethod.equals("testCxfSamlCallerHttpPolicy" ) ){
                               System.out.println("DEBUG: testCxfSamlCallerHttpPolicy");                               
                               service = (Service)new FatSamlC01Service(wsdlURL, serviceName);
                               strExpect = "Liberty Fat SamlCaller bac01(" + untID + ")";
                           } else if(thisMethod.equals("testCxfCaller_WithRealmName")){
                               System.out.println("DEBUG: testCxfCaller_WithRealmName");                               
                               service = (Service)new FatSamlC02Service(wsdlURL, serviceName);
                               strExpect = "Liberty Fat SAMLCaller bac02(" + "realm name: saml.test)";
                           } else if(thisMethod.contains("testCxfCaller_") && thisMethod.toLowerCase().contains("identifier")){
                               System.out.println("DEBUG: testCxfCaller_ + identifier");                               
                               service = (Service)new FatSamlC02aService(wsdlURL, serviceName);
                               strExpect = "Liberty Fat SAMLCaller bac02a(";
                           } else if(thisMethod.equals("testCxfCallerHttpsPolicy")){
                               System.out.println("DEBUG: testCxfCallerHttpsPolicy");                               
                               service = (Service)new FatSamlC03Service(wsdlURL, serviceName);
                               strExpect = "Liberty Fat SAMLCaller bac03(" + untID + ")";
                           } else if(thisMethod.equals("testCxfCallerHttpsPolicy_IncludeTokenInSubjectIsFalse")){
                               System.out.println("DEBUG: testCxfCallerHttpsPolicy_IncludeTokenInSubjectIsFalse");                               
                               service = (Service)new FatSamlC04Service(wsdlURL, serviceName);
                               strExpect = "samlID:Assertion";
                           } else {
                               System.out.println("DEBUG: Somehow " + thisMethod + " did NOT match any of the supported cases");                                                              
                           }

                           if( service == null ){
                               throw new Exception("thisMethod '" + thisMethod + "' did not get a Service. Test cases error." );
                           }
                           testCxfService( request, response, service, strExpect, strSubErrMsg );
                           
                           //if( thisMethod.equals( "testCxfSamlCallerHttpPolicy" ) ){
                           
                           //wsdlURL          = new URL(wsdlLocation2); //@AV999
                           //    service = (Service)new FatSamlC01Service( wsdlURL, serviceName); //@AV999
                           //    strExpect = "This is WSSECFVT CXF Web Service(" + untID + ")"; //@AV999
                           //}
                           
                              
                           //testCxfService( request, response, service, strExpect, strSubErrMsg );//@AV999
//                           System.out.println("DEBUG: Response received:");
                           //soapResp.writeTo(System.out);
                                                                               
                           //answer = soapResp.getSOAPBody().getTextContent();
                                   
                        } catch (Exception ex) {
                            System.out.println("Exception invoking my Web Service");
                            ex.printStackTrace();
                            //answer = ex.getMessage();
                        }
                       
                       /* PrintWriter rsp = response.getWriter();
                                rsp.print("<html><head><title>CXF SAML Service Cleint</title></head><body>");
                                rsp.print("<p>Request: SAML CXF Service Client</p>");
                                rsp.print("<p>Response: "+answer+"</p>");
                            rsp.print("</body></html>"); */
                                    
                        return;

        
        
        // call the test case to verify the output 
        /*try {
            wsdlURL          = new URL(wsdlLocation);
            Service service  = null;
            String strExpect = null;
            String strSubErrMsg = "Not known what error message yet";
            if( thisMethod.equals( "testCxfSamlCallerHttpPolicy" ) ){
                service = (Service)new FatSamlC01Service( wsdlURL, serviceName);
                strExpect = "Liberty Fat SamlCaller bac01(" + untID + ")";
            } else  if( thisMethod.equals( "testCxfSamlCallerHttpsPolicy" ) ){
                service = (Service)new FatSamlC02Service( wsdlURL, serviceName);
                strExpect = "Liberty Fat SamlCaller bac02(" + untID + ")";
                strSubErrMsg = "There is no Asymmetric signature token exists in the message";
            } else  if( thisMethod.equals( "testCxfSamlCallerNoPolicy" ) ){
                service = (Service)new FatSamlC03Service( wsdlURL, serviceName);
                strExpect = "Liberty Fat SamlCaller bac03(" + untID + ")";
            } else  if( thisMethod.equals( "testCxfSamlCallerHttpsNoUntPolicy" ) ){
                service = (Service)new FatSamlC04Service( wsdlURL, serviceName);
                strExpect = "Liberty Fat SamlCaller bac04(" + untID + ")";
                strSubErrMsg = "There is no Username token in the message to process caller.";
                // msg is There is no username token in the message to process the caller
                //    in nlsprops
                if( !thisMethod.equals( methodFull ) ){ // Test this test in x509 SamlCaller
                    strSubErrMsg = "There is no Asymmetric signature token exists in the message";
                }
            } else  if( thisMethod.equals( "testCxfSamlCallerX509TokenPolicy" ) ){
                service = (Service)new FatSamlC05Service( wsdlURL, serviceName);
                strExpect = "Liberty Fat SamlCaller bac05(" + untID + ")";
            } else  if( thisMethod.equals( "testCxfSamlCallerX509TransportEndorsingPolicy" ) ){
                service = (Service)new FatSamlC06Service( wsdlURL, serviceName);
                strExpect = "Liberty Fat SamlCaller bac06(" + untID + ")";
            } else  if( thisMethod.equals( "testCxfSamlCallerSymmetricEndorsingPolicy" ) ){
                service = (Service)new FatSamlC07Service( wsdlURL, serviceName);
                strExpect = "Liberty Fat SamlCaller bac07(" + untID + ")";
            } else  if( thisMethod.equals( "testCxfSamlCallerSymmetricEndorsingTLSPolicy" ) ){
                service = (Service)new FatSamlC08Service( wsdlURL, serviceName);
                strExpect = "Liberty Fat SamlCaller bac08(" + untID + ")";
            }
            if( service == null ){
                throw new Exception("thisMethod '" + thisMethod + "' did not get a Service. Test cases error." );
            }
            testCxfService( request, response, service, strExpect, strSubErrMsg );
        } catch (Exception ex) {
            ex.printStackTrace();
        }        

        return; */
    }

    
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void testCxfService(HttpServletRequest request,
            HttpServletResponse response,
            javax.xml.ws.Service service,
            String strExpect) throws ServletException, IOException {
        testCxfService( request, response, service, strExpect, (String) null);
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
        if( testMode.startsWith( "positive" ) ){
            testCxfPositiveMigService( request, response, service, strExpect, strSubErrMsg);
        } else{
            testCxfNegativeMigService( request, response, service, strSubErrMsg);
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
            String strSubErrMsg ) throws ServletException, IOException {

        try {
        
            if( endPoint.contains("9085") ){ // This piece of code only needed when we try to use tcpmon to debug. It's only in HTTP
                service.addPort(servicePort,
                                SOAPBinding.SOAP11HTTP_BINDING,
                                endPoint);                  
            }     
            
            Dispatch<SOAPMessage> dispSOAPMsg =  service.createDispatch(servicePort,
                                                 SOAPMessage.class, 
                                                 Mode.MESSAGE);
            
            // set properties into requestContext when necessary
            if (untID != null && untID.length() > 0 
                            && untPassword != null && untPassword.length() > 0) {
                Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
                System.out.println("Setting untID: " + untID + " with password: " + untPassword );
                requestContext.put("ws-security.username", untID       );
                requestContext.put("ws-security.password", untPassword );
            }

            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

            String answer = soapResp.getSOAPBody().getTextContent();
            System.out.println(thisMethod + ":answer:" + answer);


            if(thisMethod != null && thisMethod.contains("Replay")){
                try{
                    testMode = "negative"; // replay is an attack
                    soapResp = dispSOAPMsg.invoke(soapReq);

                    answer = soapResp.getSOAPBody().getTextContent();
                    System.out.println(thisMethod + ":Replay:answer:" + answer);
                    printUnExpectedResult( response, answer);
                } catch( Exception ex ){
                    ex.printStackTrace();
                    printExpectedException( response, ex, strSubErrMsg);   
                }
            } else{
                printExpectedResult( response, answer, strExpect);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            printUnExpectedException( response, ex);            
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
        
            if( endPoint.contains("9085")){ // This piece of code only needed when we try to use tcpmon to debug. It's only in HTTP
                service.addPort(servicePort,
                                SOAPBinding.SOAP11HTTP_BINDING,
                                endPoint);                  
            }     
            
            Dispatch<SOAPMessage> dispSOAPMsg =  service.createDispatch(servicePort,
                                                 SOAPMessage.class, 
                                                 Mode.MESSAGE);
            
            // set properties into requestContext when necessary
            if (untID != null && untID.length() > 0 ) {
                Map<String, Object> requestContext = dispSOAPMsg.getRequestContext();
                System.out.println("Setting untID: " + untID + " with password: " + untPassword );
                requestContext.put("ws-security.username", untID       );
                requestContext.put("ws-security.password", untPassword );
            }


            SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq);

            String answer = soapResp.getSOAPBody().getTextContent();
            System.out.println(thisMethod + ":answer:" + answer);

            printUnExpectedResult( response, answer);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            printExpectedException( response, ex, strSubErrMsg);            
        }

        return;
    }
    
    
    // get the parameters from test-client request
    void getTestParameters(HttpServletRequest request){
        // Extract valuse sent by the client
        try {
            serverName  = request.getParameter("serverName");
            System.out.println("gkuo: servername:" + serverName);
            thisMethod  = request.getParameter("testName");
            callerPolicy  = request.getParameter("callerPolicy");
            testMode    = request.getParameter("testMode");
            httpPortNum = request.getParameter("httpDefaultPort");
            httpSecurePortNum = request.getParameter("httpDefaultSecurePort");
            id = request.getParameter("id");
            pw = request.getParameter("pw");
            rawServiceName = request.getParameter("serviceName");
            methodFull = request.getParameter("methodFull");
           
            String rawServicePort = request.getParameter("servicePort");
                       
            serviceName = new QName(SERVICE_NS, rawServiceName);
            servicePort = new QName(SERVICE_NS, rawServicePort);
            untID = id;
            untPassword = pw;
            //untID       = request.getParameter("untID"); //@AV999
            //if( untID == null ) untID = "user1"; //@AV999
            //untPassword = request.getParameter("untPassword"); //@AV999

            if (httpSecurePortNum == null || httpSecurePortNum.length()==0) {
                httpProtocal = "http:";
                thePort = httpPortNum;
            } else {
                thePort = httpSecurePortNum;
                httpProtocal = "https:";
            }
            System.out.println("Protocol = " + httpProtocal + ", the port = " + thePort);
        } catch (Exception ex) {
            System.out.println("Failed to find all required parameters");
            ex.printStackTrace();
        }       
    }
    
    void prepareServiceClientReqMsg(){
        // prepare the request message
        try {
            String strSoapEnv =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" + 
                "<soapenv:Header></soapenv:Header>" + 
                "<soapenv:Body xmlns=\"http://caller.liberty.test/types\"><invoke>" +
                callerPolicy + ":" + testMode + ":" + methodFull + // add the callerPolicy, testMode and methodFull for easier debugging 
                "</invoke></soapenv:Body></soapenv:Envelope>"; 
            reqMsg = new StringReader( strSoapEnv );
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
    void printExpectedResult( HttpServletResponse response, String answer, String strExpect)
         throws IOException{
        
        PrintWriter rsp = response.getWriter();//getPrintWriter(response);
        rsp.print("<html><head><title>CXF SAML Caller Service Client</title></head><body>");
        rsp.print("<p>pass:" + (answer.contains(strExpect)));
        rsp.print( "::" + rawServiceName + "</p>" );
        rsp.print("<p>answer:"+answer+"</p>" );
        rsp.print("<p>expect:" + strExpect + "</p>");
        endPrint( rsp );
    }
 
    // print results
    // when we do not expect service-provider  to respond 
    void printUnExpectedResult( HttpServletResponse response, String answer)
         throws IOException{
        
        PrintWriter rsp = response.getWriter();//getPrintWriter(response);
        rsp.print("<html><head><title>CXF SAML Caller Service Client</title></head><body>");
        rsp.print("<p>pass:false" );
        rsp.print( "::" + rawServiceName + "</p>" );
        rsp.print("<p>answer:"+answer+"</p>" );
        rsp.print("<p>expect to get an Exception but none received</p>");
        endPrint( rsp );
    }
   
    // print results
    // when we expect service-provider throws an Exception.
    // But double check the exception to have some specified sub-message
    void printExpectedException(HttpServletResponse response, Exception ex, String strSubErrMsg)
         throws IOException{
        PrintWriter rsp = response.getWriter();//getPrintWriter(response);
        String strMsg = ex.getMessage();
        if( strMsg == null) strMsg = "";
        rsp.print("<html><head><title>CXF SAML Caller Service Client (printExpectedException)</title></head><body>");
        rsp.print("<p>pass:" +strMsg.contains(strSubErrMsg) );
        rsp.print("::" + rawServiceName + "</p>" );
        rsp.print("<p>Exception:ClassName:" + ex.getClass().getName() + "</p>");
        rsp.print("<p>Message:" + ex.getMessage() + "</p>");
        endPrint( rsp );

    }
    
    // print results
    // when we do not expect service-provider throws an Exception but it does
    void printUnExpectedException(HttpServletResponse response, Exception ex)
         throws IOException{
        PrintWriter rsp = response.getWriter();//getPrintWriter(response);
        rsp.print("<html><head><title>CXF SAML Caller Service Client (printUnExpectedException)</title></head><body>");
        rsp.print("<p>pass:" + "false" );
        rsp.print("::" + rawServiceName + "</p>" );
        rsp.print("<p>Should not throw exception but get:" + ex.getClass().getName() + "</p>");
        rsp.print("<p>Message:" + ex.getMessage() + "</p>");
        endPrint( rsp );
    }    


    PrintWriter getPrintWriter(HttpServletResponse response) throws IOException{
        PrintWriter rsp = response.getWriter();            
        rsp.print("<html><head/><body>");
        return rsp;
    }

    void endPrint( PrintWriter rsp ) throws IOException {
        rsp.print("<p>m:" + thisMethod + "</p>");
        rsp.print("<p>policy:" + callerPolicy + "</p>");
        rsp.print("<p>" + testMode   + "</p>");
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
