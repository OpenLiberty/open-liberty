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

package com.ibm.ws.wssecurity.fat.utils.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.net.ConnectException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.soap.SOAPBinding;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

public class CommonTests {

    // public static TCPMonitor tcpMon = null;
    // public static MessageListener msgListener = new MessageListener();
    static private final Class<?> thisClass = CommonTests.class;
    public static LibertyServer server = null;

    protected static String portNumber = "";
    protected static String checkPort = "";
    protected static String portNumberSecure = "";
    protected static String clientHttpUrl = "";
    protected static String clientHttpsUrl = "";
    protected static String ENDPOINT_BASE = "";
    protected static String NAMESPACE_URI = "";
//    protected static String uniqOrigServerXml = null;
    protected static TrustManager tm = null;

    protected static String hostName = "localhost";

    //static final int TCPMON_LISTENER_PORT = 9088;
    static final int TCPMON_LISTENER_PORT = 0;

    static String strJksLocation = "./securitykeys/sslClientDefault.jks";

    protected final static String defaultClientWsdlLoc = System.getProperty("user.dir")
                                                         + File.separator + "cxfclient-policies" + File.separator;
    protected final static String defaultHttpPort = "8010";
    final static String defaultHttpsPort = "8020";

    protected static String[] ignoredServerExceptions = null;

    @Rule
    public final TestName testName = new TestName();

    static String lastServer = null;
    static String origServer = null;
    public static String _testName = null;

//    private void logTestCaseInServerSideLogs(String action) throws Exception {
//
//        for (LibertyServer server : serverRefList) {
//            logTestCaseInServerSideLog(action, server);
//        }
//    }

    @Before
    public void setTestName() throws Exception {
        _testName = testName.getMethodName();
        System.out.println("----- Start:  " + _testName + "   ----------------------------------------------------");
        printMethodName(_testName, "Starting TEST: " + _testName);

//        logTestCaseInServerSideLogs("STARTING");

    }

    public CommonTests() {
        super();
        try {
            SharedTools.fixProviderOrder("CommonTests");
        } catch (Exception e) {
            Log.info(thisClass, "CommonTests", "Failed to either view or update the Java provider list - this MAY cause issues later");
        }

    }

    /**
     * Sets up any configuration required for running the tests. Currently, it
     * just starts the server, which should start the applications in dropins.
     */

    // CXF interface - for backward compat
    public static void commonSetUp(String requestedServer, Boolean useSSL,
                                   String uniqueUrl) throws Exception {
        commonSetUp(requestedServer, null, useSSL, uniqueUrl, null);

    }

    // CXF interface - allowing a different starting server.xml
    public static void commonSetUp(String requestedServer, String serverXML,
                                   Boolean useSSL, String uniqueUrl) throws Exception {
        commonSetUp(requestedServer, serverXML, useSSL, uniqueUrl, null);

    }

    // TWAS interface
    public static void commonSetUp(String requestedServer, String ServerXML,
                                   String NS) throws Exception {
        commonSetUp(requestedServer, ServerXML, false, null, NS);
    }

    public static void commonSetUp(String requestedServer, String serverXML,
                                   Boolean useSSL, String uniqueUrl, String NS) throws Exception {
        commonSetUp(requestedServer, serverXML, useSSL, uniqueUrl, NS, "CWWKS0008I"); // CWWKS0008I: The security service is ready.
    }

    public static void commonSetUp(String requestedServer, String serverXML,
                                   Boolean useSSL, String uniqueUrl, String NS,
                                   String strCheckMessage // extra message to be checked after Server start, such as: CWWKS0008I: The security service is ready.
    ) throws Exception {

        String thisMethod = "commonSetUp";
        printMethodName("commonSetUp");

        setupSSLClient();

        //11/2020 to be removed, since we're passing in server object from test case caller
        //server = LibertyServerFactory.getLibertyServer(requestedServer);

        String fixedServerName = "server.xml";
        if (serverXML != null) {
            Log.info(thisClass, thisMethod, "fixedServerName = serverXML");
            fixedServerName = serverXML;
        }
        Log.info(thisClass, thisMethod, "setting lastServer and origServer");
        lastServer = System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + fixedServerName;
        origServer = System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + fixedServerName;

        if (serverXML != null) {
//            uniqOrigServerXml = serverXML;
            Log.info(thisClass, thisMethod, "checking when serverXml is not server.xml");
            if (!serverXML.equals("server.xml")) {
                Log.info(thisClass, thisMethod, "ServerRoot is: " + server.getServerRoot());
                String oldServerXml = server.getServerRoot() + File.separator
                                      + "server.xml";
                Log.info(thisClass, thisMethod, "oldServerXML is : " + oldServerXml);
                String newServerXml = System.getProperty("user.dir")
                                      + File.separator + server.getPathToAutoFVTNamedServer()
                                      + serverXML;
                Log.info(thisClass, thisMethod, "newServerXML is: " + newServerXml);
                Log.info(thisClass, thisMethod, "Copy newServerXml to oldServerXml ");
                Log.info(thisClass, thisMethod, "Copy: " + newServerXml
                                                + " to: " + oldServerXml);
                Log.info(thisClass, thisMethod, "Before updateServerXml.copyFile()");
                UpdateServerXml.copyFile(newServerXml, oldServerXml);
                Log.info(thisClass, thisMethod, "After UpdateServerXml.copyFile");
                // assumes that commonSetUp is called with the original
                // server.xml (or server_orig.xml)
            }
        }

        HttpUnitOptions.setLoggingHttpHeaders(true);

        // install the callback handler if it exists for this FAT project
        //updated 11/2020 to remove; the callBackhandler is already installed via build.gradle and ShrinkHelper
        //SharedTools.installCallbackHandler(server);

//        String cbh = "com.ibm.ws.wssecurity.example.cbh_1.0.0";
//        File f = new File(server.getServerRoot() + File.separator + "publish" + File.separator + "bundles" + File.separator + cbh + ".jar");
//        if (f.exists()) {
//            Log.info(thisClass, thisMethod, "Installing callback handler: " + cbh);
//            server.installUserBundle(cbh);
//            server.installUserFeature("wsseccbh-1.0");
//        }
        Log.info(thisClass, thisMethod, "Starting server: " + requestedServer);
        server.startServer();

        checkPort = "" + server.getHttpDefaultPort();
        server.waitForStringInLog("port " + checkPort);

        if (TCPMON_LISTENER_PORT != 0) {

            // chc//System.out.println("TCPMON starting...");

            // chc//tcpMon = TCPMonitor.instance(TCPMON_LISTENER_PORT,
            // "localhost",
            // chc//Integer.valueOf(server.getHttpDefaultPort()), msgListener);
            // tcpMonStarted = true;
            portNumber = "" + TCPMON_LISTENER_PORT;
        } else {
            portNumber = "" + server.getHttpDefaultPort();
        }

        // clientHttpUrl = "http://localhost:" + portNumber + uniqueUrl;
        clientHttpUrl = "http://localhost:" + "" + server.getHttpDefaultPort()
                        + uniqueUrl;
        ENDPOINT_BASE = "http://localhost:" + portNumber;

        Log.info(thisClass, thisMethod, "****clientHttpUrl is:" + clientHttpUrl);
        Log.info(thisClass, thisMethod, "****portNumber is:" + portNumber);

        if (useSSL) {
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
            } ;
            portNumberSecure = "" + server.getHttpDefaultSecurePort();
            server.waitForStringInLog("port " + portNumberSecure);

            assertNotNull("SSL Service is not ready.", server.waitForStringInLog("CWWKO0219I.*ssl"));
            clientHttpsUrl = "https://localhost:" + portNumberSecure + uniqueUrl;

            Log.info(thisClass, thisMethod, "****portNumberSecure is:"
                                            + portNumberSecure);
            Log.info(thisClass, thisMethod, "clientHttpUrl: " + clientHttpUrl);
            Log.info(thisClass, thisMethod, "clientHttpsUrl: " + clientHttpsUrl);

            //SRVE0274W: Error while adding servlet mapping for path ...
            addIgnoredServerException("SRVE0274W");
//            server.addIgnoredErrors(regexes);
//            server.
//            /* SRVE0274W */
        }

        NAMESPACE_URI = NS;
        if (strCheckMessage != null && strCheckMessage.length() > 0) {
            server.waitForStringInLog(strCheckMessage, 10 * 1000); // wait for strCheckMessage unless 10 seconds timeout
        }
        return;

    }

    // CXF Client tests
    public void genericTest(String thisMethod, String useThisUrl,
                            String securePort, String id, String pw, String serviceName,
                            String clientWsdl, String servicePort, String sendMsg,
                            String verifyMsg, String failMsg) throws Exception {

        genericTest(thisMethod, useThisUrl,
                    securePort, id, pw, serviceName,
                    clientWsdl, servicePort, sendMsg,
                    verifyMsg, null, failMsg);
    }

    public void genericTest(String thisMethod, String useThisUrl,
                            String securePort, String id, String pw, String serviceName,
                            String clientWsdl, String servicePort, String sendMsg,
                            String verifyMsg, String verifyMsg2, String failMsg) throws Exception {
        try {
            genericTestSub(thisMethod, useThisUrl,
                           securePort, id, pw, serviceName,
                           clientWsdl, servicePort, sendMsg,
                           verifyMsg, verifyMsg2, failMsg);
        } catch (ConnectException e) {
            // Something must be very wrong. This is calling from HttpClient to ServiceClient
            // It should not get any ConnectException

            String strMsg = e.getMessage();
            if (strMsg != null && strMsg.indexOf("Connection refused") >= 0) {
                // This Exception should not happen
                // Let wait 1 second before try again.
                String strTmp = "Let's sleep 1 second to wait for the resource to cool down a little bit....";
                Log.error(thisClass, thisMethod, e, strTmp);
                System.err.println(strTmp);
                try {
                    Thread.currentThread().sleep(1000);
                } catch (Exception se) {
                    // do nothing
                }
                // call again
                genericTestSub(thisMethod, useThisUrl,
                               securePort, id, pw, serviceName,
                               clientWsdl, servicePort, sendMsg,
                               verifyMsg, verifyMsg2, failMsg);
            }
        }
    }

    public void genericTestSub(String thisMethod, String useThisUrl,
                               String securePort, String id, String pw, String serviceName,
                               String clientWsdl, String servicePort, String sendMsg,
                               String verifyMsg, String verifyMsg2, String failMsg) throws Exception {

        printMethodName(thisMethod);
        String respReceived = null;
        Log.info(thisClass, thisMethod, "In CommontTests.genericTestSub: generic CXF client code");

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

//            if (!securePort.isEmpty()) {
//                Log.info(thisClass, thisMethod, "Setting trustStore to " + strJksLocation);
//                System.setProperty("javax.net.ssl.trustStore", strJksLocation);
//                System.setProperty("javax.net.ssl.trustStorePassword",
//                                   "LibertyClient");

//                setupSSLClient(strJksLocation, "LibertyClient");
//            }

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            //Log.info(thisClass, thisMethod, "useThisUrl is: " + useThisUrl);
            Log.info(thisClass, thisMethod, "Invoke service client: " + useThisUrl);
            request = new GetMethodWebRequest(useThisUrl);

            request.setParameter("testName", thisMethod);
            request.setParameter("httpDefaultPort", portNumber);
            request.setParameter("httpSecureDefaultPort", securePort);
            request.setParameter("id", id);
            request.setParameter("pw", pw);
            request.setParameter("serviceName", serviceName);
            request.setParameter("clientWsdl", clientWsdl);
            request.setParameter("servicePort", servicePort);
            request.setParameter("msg", sendMsg);

            // debug
//            Log.info(thisClass, thisMethod, "testName " + thisMethod);
//            Log.info(thisClass, thisMethod, "httpDefaultPort" + portNumber);
//            Log.info(thisClass, thisMethod, "httpSecureDefaultPort" +
//                                            securePort);
//            Log.info(thisClass, thisMethod, "id" + id);
//            Log.info(thisClass, thisMethod, "pw" + pw);
//            Log.info(thisClass, thisMethod, "serviceName" + serviceName);
//            Log.info(thisClass, thisMethod, "clientWsdl" + clientWsdl);
//            Log.info(thisClass, thisMethod, "servicePort" + servicePort);
//            Log.info(thisClass, thisMethod, "msg" + sendMsg);
//
//            Log.info(thisClass, thisMethod, "wc: " + wc);
            Log.info(thisClass, thisMethod, "request is: " + request);

            // Invoke the client

            Log.info(thisClass, thisMethod, "Invoke the client - before wc.getResponse");
            response = wc.getResponse(request);
            Log.info(thisClass, thisMethod, "After wc.getResponse");

            if (response == null) {
                Log.info(thisClass, thisMethod, "Response from CXF x509 Sig Service client was null");
            }
            // Read the response page from client jsp
            Log.info(thisClass, thisMethod, "Read the response - before response.getText() ");
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from CXF x509 Sig Service client: " + respReceived);

            // Service client catches the exception from the service and returns
            // the exception in
            // the msg, so, if we get an exception, there must be something
            // really wrong!
        } catch (ConnectException e) {// defect 109058 java.net.ConnectException: Connection refused
            // The request is from httpClient to the serviceClient. It should not get Connection refused
            Log.error(thisClass, thisMethod, e,
                      "javax.net.ConnectException occurred - we should not get ConnectionException ");
            System.err.println("ConnectException: " + e);
            throw e;

        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e,
                      "Exception occurred - Service Client would catch all expected exceptions: ");
            System.err.println("Exception: " + e);
            throw e;
        }

        // assertTrue(failMsg + " Message received is: " + respReceived,
        // respReceived.contains(verifyMsg));
        assertTrue("\n" + "Expected: " + "\n" + verifyMsg + "\n"
                   + " But received: " + "\n" + respReceived + "\n", respReceived.contains(verifyMsg));
        if (verifyMsg2 != null) {
            assertTrue("\n" + "Expected: " + "\n" + verifyMsg2 + "\n"
                       + " But received: " + "\n" + respReceived + "\n", respReceived.contains(verifyMsg2));
        }

        Log.info(thisClass, thisMethod, thisMethod + ": PASS");
        return;

    }

    // TWAS Client tests
    public void genericTest(String thisMethod, String ENDPOINT_EXT,
                            String serviceName, String servicePort, String sendMsg,
                            String verifyMsg) throws Exception {
        genericTest(thisMethod, ENDPOINT_EXT, serviceName, servicePort,
                    sendMsg, verifyMsg, "");
    }

    public void genericTest(String thisMethod, String ENDPOINT_EXT,
                            String serviceName, String servicePort, String sendMsg,
                            String verifyMsg, String verifyMsg2) throws Exception {

        printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, "In generic TWAS client code");

        String respMsg = null;
        StringReader sendThisMsg = new StringReader(sendMsg);

        QName QUALIFIED_SERVICE_NAME = new QName(NAMESPACE_URI, serviceName);
        QName QUALIFIED_PORT_NAME = new QName(NAMESPACE_URI, servicePort);

        Service svc = Service.create(QUALIFIED_SERVICE_NAME);

        svc.addPort(QUALIFIED_PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING,
                    ENDPOINT_BASE + ENDPOINT_EXT);

        try {

            Dispatch<SOAPMessage> dispatch = svc.createDispatch(
                                                                QUALIFIED_PORT_NAME, SOAPMessage.class, Mode.MESSAGE);
            Source src = new StreamSource(sendThisMsg);
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message = factory.createMessage();
            message.getSOAPPart().setContent(src);
            message.saveChanges();
            Log.info(thisClass, thisMethod,
                     "Invoking Web Service:");
            SOAPMessage returnMessage = dispatch.invoke(message);
            Log.info(thisClass, thisMethod,
                     "Response received:");
            respMsg = returnMessage.getSOAPBody().getTextContent();

            Log.info(thisClass, thisMethod,
                     "Response SOAP Body content: " + respMsg);

        } catch (Exception ex) {
            respMsg = ex.getMessage();
            Log.info(thisClass, thisMethod,
                     "exception received: " + respMsg);
            ex.printStackTrace();
        }

        assertTrue("The " + thisMethod + " test failed.  Expected: "
                   + verifyMsg + " Received: " + respMsg, respMsg.contains(verifyMsg));
        if (verifyMsg2 != null) {
            assertTrue("The " + thisMethod + " test failed.  Expected: "
                       + verifyMsg2 + " Received: " + respMsg, respMsg.contains(verifyMsg2));
        }

        Log.info(thisClass, thisMethod, thisMethod + ": PASS");
        return;

    }

    public static void reconfigServer(String copyFromFile) throws Exception {

        reconfigServer(copyFromFile, _testName);

    }

    public static void reconfigServer(String copyFromFile, String testName) throws Exception {

        String thisMethod = "reconfigServer";

        Log.info(thisClass, thisMethod, "************** Starting server.xml update for: " + testName);

        // mark the end of the log - all work will occur after this point
        server.setMarkToEndOfLog();

        if (copyFromFile == null) {
            Log.info(thisClass, thisMethod, "No new server xml specified for - skipping reconfig");
            return;
        }

        if (copyFromFile.equals(lastServer)) {
            Log.info(thisClass, thisMethod, "The SAME SERVER CONFIG file is being used - skipping reconfig");
            return;
        }
        lastServer = copyFromFile;

        // update the server config by replacing the server.xml...
        try {
            String serverFileLoc = (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();
            Log.info(thisClass, "reconfigServer", "Copying: " + copyFromFile
                                                  + " to " + serverFileLoc);
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
                                                   serverFileLoc, "server.xml", copyFromFile);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }

        // make sure that the server detected the update
        // not checking the result - go on in either case - msgs will be logged in either case
        if (server.waitForStringInLogUsingMark("CWWKG0016I") != null) {
            Log.info(thisClass, "reconfigServer", "Server detected the update");
        } else {
            Log.info(thisClass, "reconfigServer", "Server did NOT detect the update - will continue waiting for additional messages");
        }

        String noUpdateMsg = server.waitForStringInLogUsingMark("CWWKG0018I", 10000);
        // if we didn't get the msg that we don't need an update, go on to check for
        // the update completed msg
        if (noUpdateMsg == null) {
            //waiting for msg that server was actually updated...
            Log.info(thisClass, "reconfigServer", "Server update msg: " + server.waitForStringInLogUsingMark("CWWKG0017I"));
        } else {
            Log.info(thisClass, "reconfigServer", "noUpdateMsg: " + noUpdateMsg);
            Log.info(thisClass, "reconfigServer",
                     "Server doesn't need to be updated");
        }

        //try {
        //	Log.info(thisClass, "reconfigServer", "Sleeping for 30 seconds") ;
        //	Thread.sleep(30000);
        //} catch (InterruptedException ie) {
        //	Log.error(thisClass, "reconfigServer", ie);
        //}
        // comment out these 2 lines for dynamic update
        //server.restartServer();
        //server.waitForStringInLog("port " + checkPort);

        Log.info(thisClass, "reconfigServer", "************** Completing server.xml update: " + testName);

        System.err.flush();
    }

    public static void restoreServer() throws Exception {
        Log.info(thisClass, "restoreServer", "lastServer: " + lastServer);
        Log.info(thisClass, "restoreServer", "origServer: " + origServer);
        reconfigServer(origServer, "restoreServer");
    }

    @After
    public void endTest() throws Exception {

        try {
            restoreServer();
            String _testName = testName.getMethodName();
            printMethodName(_testName, "Ending TEST ");
            System.out.println("----- End:  " + testName + "   ----------------------------------------------------");

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            printMethodName("tearDown");

            try {
                Log.info(thisClass, "tearDown", "Time to stop the server");
                if (server != null && server.isStarted()) {
                    if (getIgnoredServerExceptions() == null) {
                        Log.info(thisClass, "tearDown", "calling stop with no expected exceptions");
                        server.stopServer();
                    } else {
                        Log.info(thisClass, "tearDown", "calling stop WITH expected exceptions");
                        server.stopServer(getIgnoredServerExceptions());
                    }
                }
            } catch (Exception e) {
                Log.info(thisClass, "tearDown", "Server stop threw an exception - start");
                e.printStackTrace(System.out);
                Log.info(thisClass, "tearDown", "Server stop threw an exception - end");
            }

            Log.info(thisClass, "tearDown", "Trying to unintall the callback handler");
            SharedTools.unInstallCallbackHandler(server);

            /*
             * if (TCPMON_LISTENER_PORT != 0) {
             * System.out.println("Terminating TCPMON...");
             * TCPMonitor.terminate(tcpMon); }
             */
//            if (uniqOrigServerXml != null) {
//                // Restore the original server.xml
//                String oldServerXml = server.getServerRoot() + File.separator
//                                      + "server.xml";
//                String newServerXml = System.getProperty("user.dir")
//                                      + File.separator + server.getPathToAutoFVTNamedServer()
//                                      + uniqOrigServerXml;
//                System.out.println("DEBUG: Copy File " + newServerXml + " to "
//                                   + oldServerXml);
//                UpdateServerXml.copyFile(newServerXml, oldServerXml);
//            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    protected static void printMethodName(String strMethod) {
        System.out.flush();
        Log.info(thisClass, strMethod, "*****************************"
                                       + strMethod);
        System.err.println("*****************************" + strMethod);
    }

    protected static void printMethodName(String strMethod, String task) {
        System.err.flush();
        Log.info(thisClass, strMethod, "*****************************" + task);
        System.err.println("*****************************" + task);
    }

    /**
     * Perform setup for testing with SSL connections: TrustManager, hostname
     * verifier, ...
     */
    private static void setupSSLClient() {

        String thisMethod = "setupSSLCLient";

        printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, "Inside setupSSLClient but not calling enableSSLv3()");
        Log.info(thisClass, thisMethod, "Setting up global trust");

        try {
            KeyManager keyManagers[] = null;

            // if the System.Properties already set up the keystore, initialize
            // it
            String ksPath = System.getProperty("javax.net.ssl.keyStore");
            if (ksPath != null && ksPath.length() > 0) {
                String ksPassword = System.getProperty("javax.net.ssl.keyStorePassword");
                String ksType = System.getProperty("javax.net.ssl.keyStoreType");
                Log.info(thisClass, "setup Keymanager", "ksPath=" + ksPath + " ksPassword=" + ksPassword + " ksType=" + ksType);
                if (ksPassword != null && ksType != null) {
                    KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

                    File ksFile = new File(ksPath);
                    KeyStore keyStore = KeyStore.getInstance(ksType);
                    FileInputStream ksStream = new FileInputStream(ksFile);
                    keyStore.load(ksStream, ksPassword.toCharArray());

                    kmFactory.init(keyStore, ksPassword.toCharArray());
                    keyManagers = kmFactory.getKeyManagers();
                }
            }

            // Create a trust manager that does not validate certificate chains
            /* */
            trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            } };

            // Install the all-trusting trust manager
            Log.info(thisClass, thisMethod, "before SSLContext TLS");
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            @SuppressWarnings("unused")
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            /* setup jdk ssl */
            Log.info(thisClass, thisMethod, "Setting trustStore to " + strJksLocation);
            System.setProperty("javax.net.ssl.trustStore", strJksLocation);
            System.setProperty("javax.net.ssl.trustStorePassword", "LibertyClient");
            // "changeit");
            System.setProperty("javax.net.debug", "ssl");
            Log.info(thisClass, thisMethod, "javax.net.debug is set to: " + System.getProperty("javax.net.debug"));

            Log.info(thisClass, thisMethod, "end of setupSSLClient()");

        } catch (Exception e) {
            Log.info(thisClass, "static initializer", "Unable to set default TrustManager", e);
            throw new RuntimeException("Unable to set default TrustManager", e);
        } finally {
            System.setProperty("javax.net.ssl.keyStore", ""); // reset the
                                                              // System
                                                              // property to
                                                              // empty string
                                                              // on keyStore
                                                              // settings for
                                                              // next test
                                                              // suite
        }

    }

    protected static TrustManager[] trustAllCerts = null;

    public static void addIgnoredServerException(String exception) {
        String method = "addIgnoredServerException";
        Log.info(thisClass, method, "Adding message [" + exception + "] to the list of ignored server exceptions");

        String[] currentExceptions = getIgnoredServerExceptions();
        if (currentExceptions == null) {
            currentExceptions = new String[0];
        }
        Log.info(thisClass, method, "Current exception list: " + Arrays.toString(currentExceptions));

        List<String> currentExceptionsList = new ArrayList<String>(Arrays.asList(currentExceptions));

        // Log.info(thisClass, method, "Adding exception: " + exception);
        currentExceptionsList.add(exception);

        String[] updatedExceptions = currentExceptionsList.toArray(new String[currentExceptionsList.size()]);
        Log.info(thisClass, method, "New exception list: " + Arrays.toString(updatedExceptions));
        setIgnoredServerExceptions(updatedExceptions);
    }

    public static void setIgnoredServerExceptions(String[] ignoredExceptions) {
        ignoredServerExceptions = ignoredExceptions.clone();
        List<String> mylist = Arrays.asList(ignoredServerExceptions);
//        server.addIgnoredErrors(mylist);
        Log.info(thisClass, "setIgnoredServerExceptions", Arrays.toString(ignoredServerExceptions));
    }

    public static String[] getIgnoredServerExceptions() {
        Log.info(thisClass, "getIgnoredServerExceptions", Arrays.toString(ignoredServerExceptions));
        return ignoredServerExceptions;
    }
}
