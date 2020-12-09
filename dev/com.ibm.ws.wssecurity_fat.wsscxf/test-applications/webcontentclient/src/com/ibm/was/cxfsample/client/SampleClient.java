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

package com.ibm.was.cxfsample.client;

import java.net.URL;
import java.util.concurrent.Future;
import javax.xml.ws.BindingProvider;

import com.ibm.was.cxfsample.sei.echo.EchoStringInput;
import com.ibm.was.cxfsample.sei.echo.EchoStringResponse;
import com.ibm.was.cxfsample.sei.echo.EchoServicePortType_EchoServicePort_Client;

import com.ibm.was.cxfsample.sei.echo.EchoService;

public class SampleClient {

    private String urlProtocol = "http";
    private String urlHost = "localhost";
    private String urlPort = "9080";
    private String strContext = "/webcontent";
    private static final String CONTEXT_PING = "/PingService";
    private static final String CONTEXT_ECHO = "/EchoService";
    private String urlSuffix = "";
    private String strScenario = "BASIC";
    private String strMessage = "Hello";
    private String strTest = "echo";    // ping, echo, async
    private String strOptions = "";     // soap11 or soap12
    private int count = 1;
    private int timeout = 15;           // Async timeout in seconds
    private static final int ONESECOND = 1000;  // Milliseconds in a second

    EchoService ss;

    
    public void setEchoService( EchoService es ){
        ss = es;
    }

    /**
     * main()
     * 
     * see printusage() for command-line arguments
     * 
     * @param args
     */
    public static void main(String[] args) {
        SampleClient client = new SampleClient();
        client.parseArgs(args);
        client.CallFromMain();
    }

    /**
     * parseArgs Read and interpret the command-line arguments
     * 
     * @param args
     */
    public void parseArgs(String[] args) {
        if (args.length >= 1) {
            for (int i = 0; i < args.length; i++) {
                try {
                    if ('-' == args[i].charAt(0)) {
                        switch (args[i].charAt(1)) {
                        case '?':
                            printUsage(null);
                            System.exit(0);
                            break;
                        case 'a':
                        case 'A':
                            timeout = new Integer(args[++i]).intValue();
                            break;
                        case 'h':
                        case 'H':
                            urlHost = args[++i];
                            break;
                        case 'p':
                        case 'P':
                            urlPort = args[++i];
                            break;
                        case 'f':
                        case 'F':
                            urlSuffix = args[++i];
                            break;
                        case 'c':
                        case 'C':
                            count = new Integer(args[++i]).intValue();
                            break;
                        case 'm':
                        case 'M':
                            strMessage = args[++i];
                            break;
                        case 's':
                        case 'S':
                            strScenario = args[++i];
                            break;
                        case 't':
                        case 'T':
                            strTest = args[++i];
                            break;
                        case 'o':
                        case 'O':
                            strOptions = args[++i];
                            break;
                        case '1':
                            strOptions += "soap11";
                            break;
                        case '2':
                            strOptions += "soap12";
                            break;
                        default:
                            printUsage(args[i]);
                            System.exit(1);
                            break;
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Invalid option format.");
                    printUsage(null);
                    System.exit(2);
                }
            }
            if (!strOptions.contains("soap")) {
                strOptions += "soap11";
            }
        }
    }

    /**
     * printUsage Print usage help to output
     * 
     * @param invalidOpt
     *            - if non-null, is the invalid parameter
     */
    private void printUsage(String invalidOpt) {
        if (null != invalidOpt) {
            System.out.println("Invalid Option: " + invalidOpt);
        }
        System.out.println("Usage:");
        System.out
                .println(this.getClass().getName()
                        + " -h [hostname] -p [port] -f [urlSuffix] -c [count] -m [message] -s [scenario] -t [test] -o [options] -a [asynctimeout]");
        System.out.println("Default values:");
        System.out.println("  hostname= localhost");
        System.out.println("  port= 9080");
        System.out.println("  urlSuffix=" + strContext+CONTEXT_ECHO);
        System.out.println("  count= 1");
        System.out.println("  message= "+strMessage);
        System.out.println("  test= echo");
        System.out.println("  asynctimeout= "+timeout);
    }

    /**
     * Call the service using class variables set from command line. No parms.
     * 
     * @return String with responses
     */
    private String CallFromMain() {
        return CallService(strContext, "",
                strScenario, strTest, strOptions, strMessage, count);
    }

    /**
     * Parms were already read into uriString. Now call the service proxy class
     * 
     * @param uriString
     *            - http://host:port/suffix
     * @param scenarioString
     *            - BASIC
     * @param testString
     *            - ping echo or async
     * @param optionsString
     *            - soap, etc
     * @param count
     *            - Int message count
     * @return String with results
     */
    public String CallService(String context, String uriString, String scenarioString,
            String testString, String optionsString, String messageString, int count) {
        System.out.println(">> CLIENT using context " + strContext + ":" + context);
        if (!context.isEmpty())
        {
            strContext = context;
        }
        TestResults xmlresults =  new TestResults();
        try {

            // SOAP 11
            if (optionsString.contains("soap11")) {
                String options = "soap11";
                for (int counter = 0; counter < count; counter++) {
                    if (0 == testString.compareTo("ping")) { 
                        uriString = ParseUri(uriString, strContext+CONTEXT_PING, ""); 
                        String retval = buildPing(uriString, messageString);
                        if (retval.isEmpty())
                        {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "pass", "Ping Success");
                        } else {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "fail", retval);
                        }
                    } else
                    if (0 == testString.compareTo("echo")) {
                        uriString = ParseUri(uriString, strContext+CONTEXT_ECHO, "");
                        String retval = buildEcho(uriString, messageString);
                        if (retval.endsWith(messageString))
                        {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "pass", retval);
                        } else {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "fail", retval);
                        }
                    } else
                    if (0 == testString.compareTo("async")) { 
                        uriString = ParseUri(uriString, strContext+CONTEXT_ECHO, "");
                        String retval = buildAsync(uriString, messageString);
                        if (retval.endsWith(messageString))
                        {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "pass", retval);
                        } else {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "fail", retval);
                        }                       
                    } else {
                        xmlresults.setTest(scenarioString, uriString, testString, options,
                            "notrun", "Test Not Supported: "+ testString);
                    }
                }
            }
            
            // SOAP12
            if (optionsString.contains("soap12")) {
                String options = "soap12";
                for (int counter = 0; counter < count; counter++) {
                    if (0 == testString.compareTo("ping")) { 
                        uriString = ParseUri(uriString, strContext+CONTEXT_PING, "12");
                        String retval = buildPing12(uriString, messageString);
                        if (retval.isEmpty())
                        {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "pass", "Ping Success");
                        } else {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "fail", retval);
                        }
                    } else
                    if (0 == testString.compareTo("echo")) {
                        uriString = ParseUri(uriString, strContext+CONTEXT_ECHO, "12");
                        String retval = buildEcho12(uriString, messageString);
                        if (retval.endsWith(messageString))
                        {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "pass", retval);
                        } else {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "fail", retval);
                        }
                    } else
                    if (0 == testString.compareTo("async")) { 
                        uriString = ParseUri(uriString, strContext+CONTEXT_ECHO, "12");
                        String retval = buildAsync12(uriString, messageString);
                        if (retval.endsWith(messageString))
                        {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "pass", retval);
                        } else {
                            xmlresults.setTest(scenarioString, uriString, testString, options,
                                "fail", retval);
                        }                       
                    } else {
                        xmlresults.setTest(scenarioString, uriString, testString, options,
                            "notrun", "Test Not Supported: "+ testString);
                    }
                }
            }
        } catch (Exception e) {
            xmlresults.setTest(scenarioString, uriString, testString, strOptions,
                    "fail", "ERROR - EXCEPTION "+ e.getMessage());
            e.printStackTrace(System.out);
        }
        
        return xmlresults.toString();
    }

    
    /**
     * buildPing
     * Call the ping service 
     * @param endpointURL The Service endpoint URL
     * @param input The message string
     * @return String - empty if the ping works, otherwise, the error string
     */
    public String buildPing(String endpointURL, String input) {
        String retval = "ERROR";
        try {
        } catch (Exception e) {
            retval = ">> CLIENT: SOAP11 Ping ERROR EXCEPTION "+e.getMessage();
            e.printStackTrace(System.out);
        }
        return retval;
    }

    /**
     * buildEcho
     * Call the Echo service (Sync)
     * @param endpointURL The Service endpoint URL
     * @param input The message string
     * @return String from the service 
     */
    public String buildEcho(String endpointURL, String input) {
        String retval = "ERROR";
        try {
            EchoServicePortType_EchoServicePort_Client client = new EchoServicePortType_EchoServicePort_Client();
            client.setEchoService( ss ); // pass the EchoService which generated in ClientServlet
            retval = client.callEchoOperation( input );
        } catch (Exception e) {
            retval = ">> CLIENT: SOAP11 Echo ERROR EXCEPTION " + e.getMessage();
            e.printStackTrace(System.out);
        }
        return retval;
    }
    
    /**
     * buildAsync
     * Call the Echo service (Async)
     * @param endpointURL The Service endpoint URL
     * @param input The message string
     * @return String from the service 
     */
    public String buildAsync(String endpointURL, String input) {
        String response = "ERROR!: ";
        try {
        } catch (Exception e) {
            response.concat("SOAP11 Async EXCEPTION " + e.getMessage());
            e.printStackTrace(System.out);
        }
        return response;
    }
    
    /**
     * buildPing12
     * Call the SOAP12 ping service 
     * @param endpointURL The Service endpoint URL
     * @param input The message string
     * @return String - empty if the ping works, otherwise, the error string
     */
    public String buildPing12(String endpointURL, String input) {
        String retval = "ERROR";
        try {
        } catch (Exception e) {
            retval = ">> CLIENT: SOAP12 Ping ERROR EXCEPTION "+e.getMessage();
            e.printStackTrace(System.out);
        }
        return retval;
    }

    /**
     * buildEcho12
     * Call the SOAP12 Echo service (Sync)
     * @param endpointURL The Service endpoint URL
     * @param input The message string
     * @return String from the service 
     */
    public String buildEcho12(String endpointURL, String input) {
        String retval = "ERROR: ";
        try {
        } catch (Exception e) {
            retval.concat("SOAP12 Echo ERROR EXCEPTION " + e.getMessage());
            e.printStackTrace(System.out);
        }
        return retval;
    }
    
    /**
     * buildAsync12
     * Call the SOAP12 Echo service (Async)
     * @param endpointURL The Service endpoint URL
     * @param input The message string
     * @return String from the service 
     */
    public String buildAsync12(String endpointURL, String input) {
        String response = "ERROR!: ";
        try {
        } catch (Exception e) {
            response.concat("SOAP12 Async EXCEPTION " + e.getMessage());
            e.printStackTrace(System.out);
        }
        return response;
    }
        
    
    /**
     * Turn separate URL elements into a single string
     * 
     * @param urlHost
     * @param urlPort
     * @param urlSuffix
     * @param context - default to be used if not provided
     * @param soap - extension to be used on suffix if not provided
     * @return String - adjusted URL
     */
    public String ProcessUri(String urlHost, String urlPort, String urlSuffix, String context, String soap) {
        String result = ""; // For the return value
        if (0 == urlSuffix.length()) {
            urlSuffix = context + soap;         
        }

        // Create a result string
        if (urlPort.length() > 0) {
            result = urlProtocol+"://" + urlHost + ":" + urlPort + urlSuffix;
        } else { // port is not required - just leave it out
            result = urlProtocol+"://" + urlHost + urlSuffix;
        }
        return result;
    }

    /**
     * Pull a URL apart and set separate private variables with the values
     * 
     * @param urlBase
     *            - http://host:port/Endpoint
     * 
     * @return String - the adjusted URL
     */
    public String ParseUri(String urlBase, String defaultContext, String defaultSoap) {
        URL url;
        if (null != urlBase) {
            // Split the URL
            try {
                if ((urlBase.indexOf("https:") > -1) || (urlBase.indexOf("HTTPS:") > -1))
                {
                    urlProtocol = "https";
                } else {
                    urlProtocol = "http";
                }
                url = new URL(urlBase);
                try {
                    urlHost = url.getHost();
                } catch (Exception e) {
                    System.out.println(">>> Cannot get host");
                }
                try {
                    urlPort = new Integer(url.getPort()).toString();
                    if (urlPort.equals("-1"))
                        urlPort = "";
                } catch (Exception e) {
                    System.out.println(">>> Cannot get port");
                }
                try {
                    urlSuffix = url.getPath();
                } catch (Exception e) {
                    System.out.println(">>> Cannot get suffix");
                }
            } catch (Exception e) {
                System.out.println(">>> Cannot parse URL(Normal in webcontentclient situation)");
            }
        }
        return ProcessUri(urlHost, urlPort, urlSuffix, defaultContext, defaultSoap);
    }
}
