/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.cookies.CookieProperties;

public class TestHelpers {

    @ClassRule
    //    public static TestServer dummyServer = new TestServer();
    private final static Class<?> thisClass = TestHelpers.class;
    protected static TrustManager[] trustAllCerts = null;
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    protected static String hostName = "localhost";
    protected static TestSettings testSettings = null;
    protected static boolean overrideRedirect = false;

    @Rule
    public final TestName testName = new TestName();
    public String _testName = "";

    /**
     * Perform setup for testing with SSL connections: TrustManager, hostname
     * verifier, ...
     */
    public static void setupSSLClient() {

        String thisMethod = "setupSSLCLient";

        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, "Setting up global trust");
        try {
            KeyManager keyManagers[] = null;

            // if the System.Properties already set up the keystore, initialize it
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
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            // @SuppressWarnings("unused")
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    Log.info(thisClass, "verify", "In test specific hostnameVerifier");
                    return true;
                }

                public void verify(String string, SSLSocket ssls) throws IOException {
                }

                public void verify(String string, X509Certificate xc) throws SSLException {
                }

                public void verify(String string, String[] strings, String[] strings1) throws SSLException {
                }

            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            // setup jdk ssl
            Log.info(thisClass, thisMethod, "Setting trustStore to " + Constants.JKS_LOCATION);
            System.setProperty("javax.net.ssl.trustStore", Constants.JKS_LOCATION);
            System.setProperty("javax.net.ssl.trustStorePassword",
                    // "changeit");
                    "LibertyClient");
            System.setProperty("javax.net.debug", "ssl");
            Log.info(thisClass, thisMethod, "javax.net.debug is set to: " + System.getProperty("javax.net.debug"));

        } catch (Exception e) {
            Log.info(thisClass, "static initializer", "Unable to set default TrustManager", e);
            throw new RuntimeException("Unable to set default TrustManager", e);
        } finally {
            System.setProperty("javax.net.ssl.keyStore", ""); // reset the System property to empty string on keyStore settings for next test suite
        }

    }

    /**
     * Sets httpunit options that all the tests need
     *
     * @param providerType
     *            - The type of provider that this test instance will use
     */
    public static void initHttpUnitSettings() {

        String thisMethod = "initHttpUnitSettings";
        msgUtils.printMethodName(thisMethod);

        /*
         * We need to make sure that scripting is enabled, but that we don't let
         * httpunit try to parse the java script embedded in the forms - that
         * doesn't seem to work all that well
         */
        HttpUnitOptions.setScriptingEnabled(true);
        HttpUnitOptions.setExceptionsThrownOnScriptError(false);
        HttpUnitOptions.setExceptionsThrownOnErrorStatus(false);
        HttpUnitOptions.setLoggingHttpHeaders(true);
        CookieProperties.setDomainMatchingStrict(false);
        CookieProperties.setPathMatchingStrict(false);

    }

    /**
     * 1) This method is to allow junit testing client to provide
     * clientCertificateAuthentication during SSL handshake
     * Since the certAuthentication attribute in configuration is implemented with
     * default value set to false,
     * the clientCertificate is ignored. And the test results are the same as
     * before in theory.
     * 2) Once the ssl in the client initialized in a single test class,
     * we got into troubles to update its settings again
     */
    protected static void setupSSLClientKeyStore(String keyFile, String password, String type) {
        String thisMethod = "setupSSLCLientKeyStore";
        /* setup jdk ssl */
        Log.info(thisClass, thisMethod, "Setting keyStore to " + keyFile);
        System.setProperty("javax.net.ssl.keyStore", keyFile);
        System.setProperty("javax.net.ssl.keyStorePassword", password);
        System.setProperty("javax.net.ssl.keyStoreType", type);
    }

    /**
     * Generic routine to end a server (just make sure the server exists before trying to end it)
     *
     * @param theServer
     * @throws Exception
     */
    private static void endServer(TestServer theServer) throws Exception {

        if (theServer != null) {
            theServer.endTestServer();
        }
    }

    /**
     * generic tear down of a server (makes sure the server exists first)
     *
     * @param theServer
     * @throws Exception
     */
    private static void tearDownServer(TestServer theServer) throws Exception {

        if (theServer != null) {
            theServer.tearDownServer();
        }
    }

    /**
     * Takes an array of server:port strings and randomly chooses one - checks if it is listening on the port
     * specified. If it is, it returns that information. If not, it creates a new list containing all OTHER
     * server and calls itself again. Hopefully randomly finding a server that is active. If no servers are
     * active then it throws an exception - which should cause the test class to fail.
     *
     * @param serverList
     * @return
     * @throws Exception
     */
    public String[] getServerFromList(String[] serverList) throws Exception {

        String thisMethod = "getServerFromList";
        if (serverList == null) {
            throw new Exception("No server from the list appears to be active - Test class is terminating");
        }
        int numServers = serverList.length;
        if (numServers == 0) {
            throw new Exception("None of the servers in the list appear to be active - Test class is terminating");
        }

        Log.info(thisClass, thisMethod, "Choosing server from a list of " + numServers + " servers.");
        Log.info(thisClass, thisMethod, "Selection will be made from: " + Arrays.toString(serverList));

        Random rand = new Random();
        Integer num = rand.nextInt(1000);

        // create array for next round of checking if needed
        String[] nextServerList = new String[numServers - 1];
        int div = num % numServers;
        Log.info(thisClass, thisMethod, "Checking server from array list location: " + div);

        int j = 0;
        for (int i = 0; i < numServers; i++) {
            if (div != i) {
                // This server wasn't selected to be checked - add to array for next round of checking if needed
                // debug msg: System.out.println("Server doesn't match - div is: " + div + " i is: " + i) ;
                nextServerList[j] = serverList[i];
                j++;
            } else {
                // debug msg: System.out.println("Server matches - div is: " + div + " i is: " + i) ;
                if (serverList[i] == null || serverList[i].length() == 0) {
                    Log.info(thisClass, thisMethod, "The server selected is not set - skipping.  (position " + i + " in the server array)");
                } else {
                    String serverName = serverList[i].split(":")[0];
                    String serverPort = serverList[i].split(":")[1];
                    String serverSecurePort = serverList[i].split(":")[2];
                    if (isRemoteServerPortInUse(serverName, serverPort)) {
                        // non-secure port available, now check ssl
                        if (isRemoteServerPortInUse(serverName, serverSecurePort)) {
                            return new String[] { serverName, serverPort, serverSecurePort };
                        }
                    }
                }
            }

        }
        return getServerFromList(nextServerList);
    }

    public boolean isRemoteServerPortInUse(String serverName, String serverPort) throws Exception {

        String thisMethod = "isRemoteServerPortInUse";
        if (serverName != null) {
            try {
                InetAddress ia = InetAddress.getByName(serverName);
                Socket s = new Socket(ia, Integer.parseInt(serverPort));
                Log.info(thisClass, thisMethod, "Server is listening on port " + serverPort + " of " + serverName);
                s.close();
                return true;
            } catch (IOException ex) {
                // The remote host is not listening on this port
                Log.info(thisClass, thisMethod, "Server is not listening on port " + serverPort + " of " + serverName);
                return false;
            }
        } else {
            Log.info(thisClass, thisMethod, "Server name specified was null");
            return false;
        }

    }

    public void testSleep(int time) throws Exception {
        Log.info(thisClass, "testSleep", "Sleeping for " + time + " seconds");
        Thread.sleep(time * 1000);
    }

    public static void addToMyList(List<String> theList, String theString) {
        if (theList == null) {
            theList = new ArrayList<String>();
        }
        theList.add(theString);
    }

    //    public void setRequestParameterIfSet(com.meterware.httpunit.WebRequest requestSettings, String name, String value) throws Exception {
    //        Log.info(thisClass, "setRequestParameterIfSet", "name: " + name + " value: " + value);
    //        if (value != null) {
    //            // only log not being set - other code will dump the list of parms set later
    //            //Log.info(thisClass, "setRequestParameterIfSet", "Setting parm: " + name + " with value: " + value);
    //            if (!value.isEmpty()) {
    //                requestSettings.setParameter(name, value);
    //                return;
    //            }
    //        }
    //        Log.info(thisClass, "setRequestParameterIfSet", "Skipping set of parm: " + name);
    //    }

    public void setRequestParameterIfSet(com.gargoylesoftware.htmlunit.WebRequest requestSettings, String name, String value) throws Exception {

        Log.info(thisClass, "setRequestParameterIfSet", "name: " + name + " value: " + value);
        if (value != null) {
            // only log not being set - other code will dump the list of parms set later
            //Log.info(thisClass, "setRequestParameterIfSet", "Setting parm: " + name + " with value: " + value);
            if (!value.isEmpty()) {
                requestSettings.getRequestParameters().add(new NameValuePair(name, value));
                return;
            }
        }

        Log.info(thisClass, "setRequestParameterIfSet", "Skipping set of parm: " + name);
    }

    public ArrayList<String[]> extractAllCookiesExcept(WebConversation wc, String exceptCookieName) {
        msgUtils.printAllCookies(wc);
        String[] cookieNames = wc.getCookieNames();

        ArrayList<String[]> cookieList = new ArrayList<String[]>();

        for (int i = 0; i < cookieNames.length; i++) {
            Log.info(thisClass, "extractAllCookiesExcept", "Processing: " + cookieNames[i]);
            if (!cookieNames[i].startsWith(exceptCookieName)) {
                String[] spCookie = new String[2];
                Log.info(thisClass, "extractAllCookiesExcept", "Adding: " + cookieNames[i]);
                spCookie[1] = wc.getCookieValue(cookieNames[i]);
                spCookie[0] = cookieNames[i];
                cookieList.add(spCookie);
            }
        }

        for (String[] cookie : cookieList) {
            Log.info(thisClass, "extractAllCookiesExcept", "Logging cookie: " + cookie[0] + " with value: " + cookie[1]);
        }
        return cookieList;
    }

    public WebConversation addAllCookiesExcept(WebConversation wc, ArrayList<String[]> cookieList, String exceptCookieName) {
        for (String[] cookie : cookieList) {
            Log.info(thisClass, "addAllCookiesExcept", "Checking cookie: " + cookie[0]);
            if (!cookie[0].equals(exceptCookieName)) {
                Log.info(thisClass, "addAllCookiesExcept", "Adding cookie: " + cookie[0] + " with value: " + cookie[1]);
                wc.addCookie(cookie[0], cookie[1]);
            }
        }
        msgUtils.printAllCookies(wc);
        return wc;
    }

    public void logDebugInfo() throws Exception {
        logNetstat();
        dumpPids();
    }

    public void logNetstat() throws Exception {
        final String cmd = "netstat -ano";
        execCmd(cmd, "logNetstat");

    }

    public void dumpPids() throws Exception {
        String cmd;
        final String cmd1 = "ps -ef";
        final String cmd2 = System.getenv("windir") + "\\system32\\" + "tasklist.exe /fo csv /nh";

        if (Machine.getLocalMachine().getOperatingSystem() == OperatingSystem.WINDOWS) {
            cmd = cmd2;
        } else {
            cmd = cmd1;
        }
        execCmd(cmd, "dumpPids");
    }

    public void execCmd(String cmd, String caller) throws Exception {
        BufferedReader br = null;
        try {

            Log.info(thisClass, "execCmd", "Invoking: " + cmd);
            Process process = Runtime.getRuntime().exec(cmd);

            InputStream in = process.getInputStream();

            String line;
            br = new BufferedReader(new InputStreamReader(in));
            while ((line = br.readLine()) != null) {

                Log.info(thisClass, caller, line);
            }
        } catch (Exception e) {
            Log.info(thisClass,
                    caller,
                    "failure occurred running \""
                            + cmd
                            + "\"  This command is ONLY run after we determine that the server has a port out of range - this is NOT the cause of the failure, just a failure as we try to gather more debug information.");
            Log.error(thisClass, caller, e);
        } finally {
            try {
                br.close();
            } catch (Exception e2) {
                Log.info(thisClass, caller, "BufferedReader close had issues - no big deal");
            }
        }
    }

    public boolean hasValue(String stringInQuestion) {
        if (stringInQuestion == null) {
            return false;
        } else {
            if (stringInQuestion.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static WebClient getWebClient(WebClient webClient) throws Exception {
        if (webClient != null) {
            Log.info(thisClass, "getWebClient", "Using existing WebClient");
            return webClient;
        } else {
            Log.info(thisClass, "getWebClient", "Creating new WebClient");
            return getWebClient();
        }
    }

    public static WebClient getWebClient() throws Exception {

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);

        return getWebClient(true);
    }

    /**
     * Creates a new WebClient - adding settings to ignore script and status errors if requested
     *
     * @param overrideException
     *            - flag indicating if scripting errors and failing status codes should be ignored
     * @return - returns a new WebClient instance
     * @throws Exception
     */
    public static WebClient getWebClient(boolean overrideException) throws Exception {

        HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostname, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(verifier);

        WebClient webClient = new WebClient();
        //        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_24);
        if (overrideException) {
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        }
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setRedirectEnabled(true);
        if (System.getProperty("java.specification.version").matches("1\\.[789]")) {
            if (CryptoUtils.isFips140_3Enabled()) {
                webClient.getOptions().setSSLClientProtocols((new String[] { "TLSv1.2" })); // rtc 259307
            } else {
                webClient.getOptions().setSSLClientProtocols((new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" })); // rtc 259307
            }
        } else {
            if (CryptoUtils.isFips140_3Enabled()) {
                webClient.getOptions().setSSLClientProtocols((new String[] { "TLSv1.3", "TLSv1.2" })); // rtc 259307
            } else {
                webClient.getOptions().setSSLClientProtocols((new String[] { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" })); // rtc 259307
            }
        }
        Log.info(thisClass, "getWebClient", "isUseInsecureSSL is set to: " + webClient.getOptions().isUseInsecureSSL());
        Log.info(thisClass, "getWebClient", "isThrowExceptionOnScriptError is set to: " + webClient.getOptions().isThrowExceptionOnScriptError());
        Log.info(thisClass, "getWebClient", "isThrowExceptionOnFailingStatusCode is set to: " + webClient.getOptions().isThrowExceptionOnFailingStatusCode());

        return webClient;
    }

    public static void destroyWebClient(WebClient webClient) throws Exception {
        try {
            if (webClient != null) {
                webClient.close();
            }
        } catch (Exception e) {
            Log.info(thisClass, "destroyWebClient", "Exception occurred trying to clean up the web client - tests will continue:  Exception was: " + e.toString());
        }
    }

    public static void waitBeforeContinuing(WebClient webClient) throws Exception {
        Log.info(thisClass, "waitBeforeContinuing", "Waiting for HtmlUnit to finish its business");
        webClient.waitForBackgroundJavaScriptStartingBefore(5000);
        webClient.waitForBackgroundJavaScript(5000);
    }

    public static File[] getFileList_endsWith(String serverLoc, final String endsWith) throws Exception {

        String thisMethod = "getFileList_endsWith";
        Log.info(thisClass, thisMethod, "Locating files in: " + serverLoc + " that end with: " + endsWith);
        File dir = new File(serverLoc);
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(endsWith);
            }
        });

        if (files != null) {
            for (File foundFile : files) {
                System.out.println(foundFile);
            }
        }

        return files;
    }

    public boolean pingExternalServer(String testcase, String serverUrl, String expectedTitle, int waitTime) throws Exception {

        String thisMethod = "pingExternalServer";
        msgUtils.printMethodName(thisMethod);
        WebClient webClient = null;
        WebRequest request = null;
        boolean status = false;

        try {
            webClient = getWebClient();

            // Build the request
            URL url = AutomationTools.getNewUrl(serverUrl);
            request = new WebRequest(url, HttpMethod.GET);
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
        }

        boolean keepChecking = true;
        int timeSlept = 0;
        while (keepChecking) {

            try {
                msgUtils.printAllCookies(webClient);
                msgUtils.printRequestParts(webClient, request, testcase, "Outgoing request");
                Object thePage = webClient.getPage(request);
                // make sure the page is processed before continuing
                waitBeforeContinuing(webClient);
                String title = AutomationTools.getResponseTitle(thePage);
                Log.info(thisClass, thisMethod, "**********************************************************");
                Log.info(thisClass, thisMethod, "Was able to get a reponse from the requested url - " + serverUrl);
                if (expectedTitle != null) {
                    if (title != null && title.contains(expectedTitle)) {
                        keepChecking = false;
                        status = true;
                        Log.info(thisClass, thisMethod, "The title in the reqponse was what we expected: " + expectedTitle);
                    } else {
                        Log.info(thisClass, thisMethod,
                                "Connected with url with title (" + title + ") - this is NOT wahat we expected - we were expecting (" + expectedTitle + ").  We may try again.");
                    }
                } else {
                    keepChecking = false;
                    status = true;
                    Log.info(thisClass, thisMethod, "The title in the reqponse was NOT validated - the actual title was: " + title);
                }
                Log.info(thisClass, thisMethod, "**********************************************************");
            } catch (Exception e) {
                Log.info(thisClass, thisMethod, "Exception occurred in " + thisMethod + System.getProperty("line.separator") + e.getMessage());
            }
            if (keepChecking) {
                if (timeSlept >= waitTime) {
                    keepChecking = false;
                    Log.info(thisClass, thisMethod, "Tried to ping " + serverUrl + " for more than the requested " + waitTime + " seconds - giving up.");
                } else {
                    Log.info(thisClass, thisMethod, "Sleeping 5 seconds");
                    testSleep(5);
                    timeSlept = timeSlept + 5;
                }
            }
        }
        destroyWebClient(webClient);
        return status;
    }
}
