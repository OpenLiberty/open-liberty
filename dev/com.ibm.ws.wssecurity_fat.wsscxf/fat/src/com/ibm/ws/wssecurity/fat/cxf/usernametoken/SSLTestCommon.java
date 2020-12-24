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

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
//Added 10/2020
import org.junit.runner.RunWith;

//Added 10/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateServerXml;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

//Added 10/2020
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 10/2020
@RunWith(FATRunner.class)
public class SSLTestCommon {

    static private final Class<?> thisClass = SSLTestCommon.class;
    //Added 10/2020
    static final private String serverName = "com.ibm.ws.wssecurity_fat.ssl";
    @Server(serverName)
    //orig from CL:
    public static LibertyServer server = null;

    protected static String portNumber = "";
    protected static String portNumberSecure = "";
    protected static String untClientUrl = "";
    protected static String untSSLClientUrl = "";

    protected static String hostName = "localhost";
    protected static TrustManager tm = null;

    static String strJksLocation = "./securitykeys/sslClientDefault.jks";
    static final String SERVICE_NS = "http://wssec.basic.cxf.fats";

    static String simpleSoapBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                   + "<soapenv:Header/>"
                                   + "<soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\">"
                                   + "<invoke>WSSECFVT Version: 2.0</invoke>"
                                   + "</soapenv:Body>"
                                   + "</soapenv:Envelope>";
    final static String badUsernameToken = "The security token could not be authenticated or authorized";
    final static String msgExpires = "The message has expired";
    final static String badHttpsToken = "HttpsToken could not be asserted";
    final static String badHttpsClientCert = "Could not send Message.";
    final static String replayAttack = "An error happened processing a Username Token \"A replay attack has been detected\"";
    final static String timestampReqButMissing = "An invalid security token was provided (WSSecurityEngine: Invalid timestamp";

    // "RequireClientCertificate is set, but no local certificates were negotiated.";

    public SSLTestCommon() {
        super();
        try {
            SharedTools.fixProviderOrder("SSLTestCommon");
        } catch (Exception e) {
            Log.info(thisClass, "SSLTestCommon", "Failed to either view or update the Java provider list - this MAY cause issues later");
        }

    }

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the applications
     * in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        //String thisMethod = "setup";
        //orig from CL:
        //server = LibertyServerFactory.getLibertyServer("com.ibm.ws.wssecurity_fat.ssl");

        //Added 10/2020
        ShrinkHelper.defaultDropinApp(server, "untsslclient", "com.ibm.ws.wssecurity.fat.untsslclient", "fats.cxf.basicssl.wssec", "fats.cxf.basicssl.wssec.types");
        ShrinkHelper.defaultDropinApp(server, "untoken", "com.ibm.ws.wssecurity.fat.untoken");

        initServer();
    }

    protected static void initServer() throws Exception {
        String thisMethod = "initServer";

        //commented out 10/16/2020, it's deprecated in CL and does not exist in OL
        //HttpUtils.enableSSLv3();

        Log.info(thisClass, "initServer", "before server.startServer() inside SSLTestCommon");
        server.startServer();// will check CWWKS0008I: The security service is ready.
        Log.info(thisClass, "setUp", "after initServer inside SSLTestCommon");

        portNumber = "" + server.getHttpDefaultPort();
        portNumberSecure = "" + server.getHttpDefaultSecurePort();
        server.waitForStringInLog("CWWKS0008I"); // Default wait for 2 minutes
        server.waitForStringInLog("port " + portNumber, 60 * 1000); // 1 minute
        server.waitForStringInLog("port " + portNumberSecure, 60 * 1000); // 1 minute

        assertNotNull("SSL Service is not ready.", server.waitForStringInLog("CWWKO0219I.*ssl"));

        //portNumber = "9085" ;
        untClientUrl = "http://localhost:" + portNumber
                       + "/untsslclient/CxfUntSSLSvcClient";
        untSSLClientUrl = "https://localhost:" + portNumberSecure
                          + "/untsslclient/CxfUntSSLSvcClient";

        Log.info(thisClass, thisMethod, "****portNumber is:" + portNumber);
        Log.info(thisClass, thisMethod, "****portNumberSecure is:"
                                        + portNumberSecure);

        try {
            tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] xcs,
                                               String string) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs,
                                               String string) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            //SSLContext ctx = SSLContext.getInstance("TLS");
            SSLContext ctx;
            try {
                Log.info(thisClass, thisMethod, "SSLContext.getInstance(TLS)");
                ctx = SSLContext.getInstance("TLS");
            } catch (Exception e) {
                Log.info(thisClass, thisMethod, "SSLContext.getInstance(SSL)");
                ctx = SSLContext.getInstance("SSL");
            }
            ctx.init(null, new TrustManager[] { tm }, null);
            SSLContext.setDefault(ctx);
            Log.info(thisClass, "static initializer",
                     "Set default TrustManager");
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HostnameVerifier hv = new HostnameVerifier() {
                @Override
                public boolean verify(String urlHostName, SSLSession session) {
                    System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
            Log.info(thisClass, "static initializer",
                     "Unable to set default TrustManager", e);
            throw new RuntimeException("Unable to set default TrustManager", e);
        }

        return;
    }

    public void genericTest(String thisMethod, String useThisUrl,
                            String securePort, String id, String pw, String serviceName,
                            String servicePort, String replayTest, String sendMsg, String verifyMsg, String failMsg) throws Exception {

        genericTest(thisMethod, useThisUrl, securePort, null, id, pw, serviceName, servicePort, replayTest, sendMsg, verifyMsg, failMsg);
    }

    public void genericTest(String thisMethod, String useThisUrl,
                            String securePort, String managedClient, String id, String pw, String serviceName,
                            String servicePort, String replayTest, String sendMsg, String verifyMsg, String failMsg) throws Exception {

        printMethodName(thisMethod);
        String respReceived = null;

        try {
            /*
             * com.sun.net.ssl.HttpsURLConnection
             * .setDefaultHostnameVerifier(new com.sun.net.ssl.HostnameVerifier() {
             * public boolean verify(String urlHostname,
             * String certHostname) {
             * return true;
             * }
             * });
             */

            if (!securePort.isEmpty()) {
                System.setProperty("javax.net.ssl.trustStore", strJksLocation);
                System.setProperty("javax.net.ssl.trustStorePassword",
                                   "LibertyClient");
            }

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            Log.info(thisClass, thisMethod, "Invoking: " + useThisUrl);
            request = new GetMethodWebRequest(useThisUrl);

            request.setParameter("testName", thisMethod);
            request.setParameter("httpDefaultPort", portNumber);
            request.setParameter("httpSecureDefaultPort", securePort);
            request.setParameter("id", id);
            request.setParameter("pw", pw);
            request.setParameter("serviceName", serviceName);
            request.setParameter("servicePort", servicePort);
            request.setParameter("msg", sendMsg);
            request.setParameter("replayTest", replayTest);
            request.setParameter("managedClient", managedClient);

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response from CXF UNT SSL Service client: "
                                            + respReceived);

            // Service client catches the exception from the service and returns
            // the exception in
            // the msg, so, if we get an exception, there must be something
            // really wrong!
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e,
                      "Exception occurred - Service Client would catch all expected exceptions: ");
            System.err.println("Exception: " + e);
            throw e;
        }

        assertTrue(failMsg, respReceived.contains(verifyMsg));
        Log.info(thisClass, thisMethod, thisMethod + ": PASS");

        return;

    }

    public static void reconfigServer(String copyFromFile) throws Exception {
        UpdateServerXml.reconfigServer(server, copyFromFile);

    }

    /*******************/
    @AfterClass
    public static void tearDown() throws Exception {
        try {
            printMethodName("tearDown");
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    protected static void printMethodName(String strMethod) {
        Log.info(thisClass, strMethod, "*****************************"
                                       + strMethod);
        System.err.println("*****************************" + strMethod);
    }
}
