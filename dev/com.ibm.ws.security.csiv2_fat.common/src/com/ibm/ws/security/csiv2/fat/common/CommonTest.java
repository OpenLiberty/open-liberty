/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2.fat.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.apps.AppConstants;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPUtils;

/**
 *
 */
public class CommonTest {

    private final static Class<?> thisClass = CommonTest.class;

    protected static TestServer downstreamServer = null;
    protected static TestServer upstreamServer = null;
    protected static TestServer intermediateServer = null;
    protected static BasicAuthClient myClient;

    protected static String user = "user1";
    protected static String pwd = "user1pwd";
    protected static String ldapPwd = "security";
    protected static String userDN = "cn=user1,o=ibm,c=us";
    protected static String assertedDN = "CN=user1, O=ibm, C=us";
    protected static String programmaticUser = "user2";
    protected static String programmaticPassword = "user2pwd";
    protected static String badUser = "user3";
    protected static String badPwd = "user3pwd";
    protected static String clOnlyUser = "user42";
    protected static String clOnlyPwd = "user42pwd";
    protected static String transportMappedUser = "localhost";
    protected static String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    protected static HashSet<String> upstreamApps = new HashSet<String>() {
        {
            add(Constants.CSIv2_CLIENT_APP);
            add(Constants.TESTMARKER_APP);
        }
    };
    protected static HashSet<String> downstreamApps = new HashSet<String>() {
        {
            add(Constants.CSIv2_DOWNSTREAM_APP);
            add(Constants.TESTMARKER_APP);
        }
    };
    protected static HashSet<String> intermediateApps = new HashSet<String>() {
        {
            add(Constants.CSIv2_INTERMEDIATE_APP);
        }
    };
    protected static List<String> extraMsgs = new ArrayList<String>() {
        {
            add("CWWKF0008I.*");
        }
    };

    @Rule
    public TestName name = new TestName();
    public String testName = "";

    public static TestServer setupUpstreamServer(String serverXML) throws Exception {
        return commonSetUp(Constants.CSIv2_UPSTREAM_SERVER, serverXML, Constants.CSIv2_UPSTREAM, upstreamApps,
                           Constants.NO_EXTRA_MSGS, Constants.CSIv2_CLIENT_APP);
    }

    public static TestServer setupUpstreamServer(String server, String serverXML) throws Exception {
        return commonSetUp(server, serverXML, Constants.CSIv2_UPSTREAM, upstreamApps,
                           Constants.NO_EXTRA_MSGS, Constants.CSIv2_CLIENT_APP);
    }

    public static TestServer setupDownstreamServer(String serverXML) throws Exception {
        return commonSetUp(Constants.CSIv2_DOWNSTREAM_SERVER, serverXML, Constants.CSIv2_DOWNSTREAM, downstreamApps,
                           extraMsgs, Constants.CSIv2_DOWNSTREAM_APP);
    }

    public static TestServer setupDownstreamServer(String server, String serverXML) throws Exception {
        return commonSetUp(server, serverXML, Constants.CSIv2_DOWNSTREAM, downstreamApps,
                           extraMsgs, Constants.CSIv2_DOWNSTREAM_APP);
    }

    public static TestServer setupIntermediateServer(String serverXML) throws Exception {
        return commonSetUp(Constants.CSIv2_INTERMEDIATE_SERVER, serverXML, Constants.CSIv2_INTERMEDIATE, intermediateApps,
                           extraMsgs, Constants.CSIv2_INTERMEDIATE_APP);
    }

    public static TestServer commonSetUp(String requestedServer,
                                         String serverXML, String testType,
                                         HashSet<String> addtlApps, List<String> addtlMessages,
                                         String targetUrl) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType,
                           addtlApps, addtlMessages, targetUrl, true, true);
    }

    public static TestServer commonSetUp(String requestedServer,
                                         String serverXML, String testType,
                                         HashSet<String> addtlApps, List<String> addtlMessages,
                                         String targetUrl, Boolean secStartMsg, Boolean sslCheck) throws Exception {

        Integer defaultPort = null;
        String httpString = null;
        String httpsString = null;
        String thisMethod = "commonSetUp";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, "requested server: " + requestedServer);
        TestServer aTestServer = null;
        try {
            aTestServer = new TestServer(requestedServer, serverXML, testType);
            aTestServer.removeServerConfigFiles();
            List<String> messages = aTestServer.getDefaultStartMessages(testType);
            if (addtlMessages != null && !addtlMessages.isEmpty()) {
                messages.addAll(addtlMessages);
            }

            HashSet<String> checkApps = aTestServer.getDefaultTestApps(testType);
            for (String c : checkApps) {
                Log.info(thisClass, thisMethod, "Loop at after Default Apps: " + c);
            }
            if (addtlApps != null && !addtlApps.isEmpty()) {
                checkApps.addAll(addtlApps);
            }

            for (String c : checkApps) {
                Log.info(thisClass, thisMethod, "Loop at end: " + c);
            }

            if (!secStartMsg) {
                aTestServer.setSkipSecurityReadyMsg();
            }

            Log.info(thisClass, thisMethod, "calling LDAPUtil.addLDAPVariables", null);
            LDAPUtils.addLDAPVariables(aTestServer.getServer());
            Log.info(thisClass, thisMethod, "called LDAPUtil.addLDAPVariables", null);

            if (sslCheck) {
                aTestServer.startServer(serverXML, checkApps, messages, null, null);
            } else {
                aTestServer.startServer(serverXML, checkApps, messages, "skip", null);
            }

//            setupSSLClient();

//            /*
//             * reconfigExpectations = new FATDataHelpers().addExpectation(null, null,
//             * Constants.MESSAGES_LOG, null, null,
//             * Constants.MSG_OPENID_CONFIG_MODIFIED);
//             */
//            initHttpUnitSettings();

            defaultPort = aTestServer.getHttpDefaultPort();
            httpString = aTestServer.getHttpString();
            httpsString = aTestServer.getHttpsString();

//            helpers = new CommonTestHelpers();
//
//            // define the testSettings that are appropriate for the test type
//            // either setup OAuth, or OpenID Connect
//            if (testType.equals(Constants.OAUTH_OP)) {
//                testSettings.setDefaultOAuthOPTestSettings(httpString, httpsString);
//                eSettings.setOAUTHOPTestType();
//                helpers.setOPServer(aTestServer);
//            } else {
//                if (testType.equals(Constants.OIDC_OP)) {
//                    testSettings.setDefaultOIDCOPTestSettings(httpString, httpsString);
//                    eSettings.setOIDCOPTestType();
//                    helpers.setOPServer(aTestServer);
//                } else {
//                    if (testType.equals(Constants.OIDC_RP)) {
//                        testSettings.setDefaultRPTestSettings(httpString, httpsString, targetUrl, providerType);
//                        //eSettings.setOAUTHTestType() ;
//                        helpers.setRPServer(aTestServer);
//                    } else {
//                        if (testType.equals(Constants.GENERIC_SERVER)) {
//                            helpers.setGenericTestServer(aTestServer);
//                        } else {
//                            throw new RuntimeException("Unknown testType:" + testType + " Can not determine defaults to set");
//                        }
//                    }
//                }
//            }

        } catch (Exception e) {
            // if we fail setting up the server, we don't know how far it may have gotten
            // the server may be running - since we're not returning the TestServer object
            // to the caller the AfterClass method won't have the reference to use to stop the server
            // if we eat the failure, the tests will go on and fail and waste time instead of
            // erroring out...
            tearDownServer(aTestServer);
            throw e;
        }
        return aTestServer;

    }

    public static void temporarilyCopyNeededFeaturesAndBundles(LibertyServer server) throws Exception {
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/csiv2-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "internalfeatures/com.ibm.ws.security.csiv2_1.0.jar");
    }

    public static void setupBasicAuthClient() {
        LibertyServer upstreamLibertyServer = upstreamServer.getServer();
        myClient = new BasicAuthClient(upstreamLibertyServer, BasicAuthClient.DEFAULT_REALM, "ClientServlet", "/csiv2Client");
    }

    public void assertPolicyCompliance() {
        LibertyServer libertyServer = upstreamServer.getServer();
        assertNotNull("The policy mismatch check must be invoked", libertyServer.waitForStringInLog("CWWKS9520E"));
    }

    public String genericDownstream(String reqUser, String reqPwd) {
        String queryString = "/ClientServlet?method=invokeEJBAfterProgrammaticLogin&user=" + reqUser + "&password=" + reqPwd
                             + "&realmName=targetServerRealm&lookup=cosnaming";
        return myClient.accessProtectedServletWithAuthorizedCredentials(queryString, reqUser, reqPwd);
    }

    public void genericDownstream() {
        String queryString = "/ClientServlet?method=invokeEJBAfterProgrammaticLogin&user=" + programmaticUser + "&password=" + programmaticPassword
                             + "&realmName=targetServerRealm&lookup=cosnaming";
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, user, pwd);

        LibertyServer libertyServer = downstreamServer.getServer();
        assertNotNull("The TSSGSSUPMechConfig's check method must be invoked.",
                      libertyServer.waitForStringInTrace("TSSGSSUPMechConfig > check Entry  "));
        assertTrue("Failed to find expected callerSubject: " + programmaticUser, response.contains("callerSubject: Subject:"));
        assertTrue("Failed to find expected principal: " + programmaticUser, response.contains("Principal: WSPrincipal:" + programmaticUser));
    }

    public Boolean driveDownstreamFlowInvalidUser(String reqUser, String reqPwd) {
        String port = downstreamServer.getIIOPDefaultPort();
        Log.info(thisClass, "driveDownstreamFlow", "IIOP port: " + port);
        String queryString = "/ClientServlet?lookup=jndi&port=" + port;
        return myClient.accessProtectedServletWithInvalidCredentials(queryString, reqUser, reqPwd);
    }

    public String driveDownstreamFlow() {
        String port = downstreamServer.getIIOPDefaultPort();
        Log.info(thisClass, "driveDownstreamFlow", "IIOP port: " + port);
        String queryString = "/ClientServlet?lookup=jndi&port=" + port;
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, user, pwd);
        return response;
    }

    public String driveDownstreamFlow(String reqUser, String reqPwd) {
        String port = downstreamServer.getIIOPDefaultPort();
        Log.info(thisClass, "driveDownstreamFlow", "IIOP port: " + port);
        String queryString = "/ClientServlet?lookup=jndi&port=" + port;
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, reqUser, reqPwd);
        return response;
    }

    public String driveDownstreamFlowGSSUP(String username, String password) {
        String port = downstreamServer.getIIOPDefaultPort();
        Log.info(thisClass, "driveDownstreamFlowGSSUP", "IIOP port: " + port);
        String queryString = "/ClientServlet?method=invokeEJBAfterProgrammaticLogin&user=" + username + "&password=" + password
                             + "&realmName=targetServerRealm&lookup=jndi&port=" + port;
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, user, pwd);
        return response;
    }

    public String driveDownstreamFlowUnprotected() {
        String port = downstreamServer.getIIOPDefaultPort();
        Log.info(thisClass, "driveDownstreamFlowUnprotected", "IIOP port: " + port);
        String queryString = "/UnprotectedClientServlet?lookup=jndi&port=" + port;
        String response = myClient.accessUnprotectedServlet(queryString);
        return response;
    }

    public String driveDownstreamFlowViaIntermediate(String reqUser, String reqPwd) {
        String intermediatePort = intermediateServer.getIIOPDefaultPort();
        String downstreamPort = downstreamServer.getIIOPDefaultPort();
        Log.info(thisClass, "driveDownstreamFlowViaIntermediate", "IIOP port: " + intermediatePort);
        String queryString = "/ClientServlet?method=invokeViaIntermediateBean&port=" + intermediatePort + "&downstreamPort=" + downstreamPort;
        return myClient.accessProtectedServletWithAuthorizedCredentials(queryString, reqUser, reqPwd);
    }

    public void assertSubjectInResponse(String response, String username) {
        Log.info(thisClass, testName, "Response to check: " + response);
        assertTrue("Failed to find expected callerSubject: " + username, response.contains("callerSubject: Subject:"));
        assertTrue("Failed to find expected principal: " + username, response.contains("Principal: WSPrincipal:" + username));
    }

    /*
     * In most of the tests in this class, we are re-configuring the existing liberty server with a new configuration file.
     * So, before the BasicAuthClient actually fire any request to the servers, we need to re-affirm that the client web application
     * is available in the upstream server. If we do not perform this check, we
     * may end up getting a 404 error at the client side. To do this, we check for the following in message in trace file.
     *
     * CWWKT0016I: Web application available<followed a string>/csiv2Client/.
     *
     * Accordingly we create a regular expression for this pattern and create a list.
     */
    public ArrayList<String> createMessagesToWaitInUpstreamServer() {
        String webAppAvailableMessageRegExp = "CWWKT0016I: Web application available.*/csiv2Client/";
        ArrayList<String> messagesListToWaitFor = new ArrayList<String>();
        messagesListToWaitFor.add(webAppAvailableMessageRegExp);
        return messagesListToWaitFor;
    }

    public ArrayList<String> createMessagesToWaitInDownstreamServer() {
        return createNameServerMessageToWaitFor();
    }

    public ArrayList<String> createMessagesToWaitInIntermediateServer() {
        ArrayList<String> messagesListToWaitFor = createNameServerMessageToWaitFor();
        messagesListToWaitFor.add("CWWKZ0003I:.*IntermediateApp");
        return messagesListToWaitFor;
    }

    private ArrayList<String> createNameServerMessageToWaitFor() {
        String nameServerAvailableRegExp = "CWWKI0001I: The CORBA name server is now available at";
        ArrayList<String> messagesListToWaitFor = new ArrayList<String>();
        messagesListToWaitFor.add(nameServerAvailableRegExp);
        return messagesListToWaitFor;
    }

    /**
     * setup before running a test
     *
     * @throws Exception
     */
    @Before
    public void setTestName() throws Exception {
        testName = name.getMethodName();
        System.out.println("----- Start:  " + testName + "   ----------------------------------------------------");
        msgUtils.printMethodName(testName, "Starting TEST ");
        logTestCaseInServerSideLog("STARTING", upstreamServer);
        logTestCaseInServerSideLog("STARTING", downstreamServer);
    }

    /**
     * Clean up after running a test
     *
     * @throws Exception
     */
    @After
    public void endTest() throws Exception {
        try {
            endServer(downstreamServer);
            logTestCaseInServerSideLog("ENDING", downstreamServer);
            endServer(upstreamServer);
            logTestCaseInServerSideLog("ENDING", upstreamServer);
            endServer(intermediateServer);

            msgUtils.printMethodName(testName, "Ending TEST ");
            System.out.println("----- End:  " + testName + "   ----------------------------------------------------");

//                    // if test case over write the autoredirect, reset it!
//                    if (OverrideRedirect) {
//                            HttpUnitOptions.setAutoRedirect(true);
//                            OverrideRedirect = false ;
//                    }

            myClient.resetClientState();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
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
     * Clean up at the end of the test class
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        try {
            msgUtils.printMethodName("tearDown");
            tearDownServer(downstreamServer);
            tearDownServer(upstreamServer);
            tearDownServer(intermediateServer);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        timeoutChecker();
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

    private void logTestCaseInServerSideLog(String action, TestServer server) throws Exception {

        try {
            if (server != null) {

                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet getMethod = new HttpGet(server.getHttpString() + "/" + AppConstants.TESTMARKER_CONTEXT_ROOT + "/" + AppConstants.TESTMARKER_PATH + "?action=" + action
                                                + "&testCaseName=" + testName);
                HttpResponse response = client.execute(getMethod);
                Log.info(thisClass, "logTestCaseInServersideLog", "log status: " + response.getStatusLine());

            }
        } catch (Exception e) {
            // just log the failure - we shouldn't allow a failure here to cause
            // a test case to fail.
            e.printStackTrace();
        }

    }

    /***
     * Test to make sure that no test case in the class has timed out waiting for msgs in the log
     * This is more of a test case test, than a product test
     *
     * @throws Exception
     */
    public static void timeoutChecker() throws Exception {
        boolean timeoutFound = false;
        String current = new java.io.File(".").getCanonicalPath();
        Log.info(thisClass, "timeoutChecker", "Current working dir: " + current);
        String currentDir = System.getProperty("user.dir");
        Log.info(thisClass, "timeoutChecker", "Current working dir: " + currentDir);
        String outputFile = "./results/output.txt";
        File f = new File(outputFile);

        if (f.exists() && !f.isDirectory()) {
            Log.info(thisClass, "timeoutChecker", "Found the output.txt file: " + f.getCanonicalPath());
            if (f.canRead()) {
                Scanner in = null;
                try {
                    in = new Scanner(new FileReader(f));
                    String theLine;
                    // look for time out msg
                    while (in.hasNextLine() && !timeoutFound) {
                        theLine = in.nextLine();
                        //if (in.nextLine().indexOf("Timed out") >= 0) {
                        if (theLine.indexOf("Timed out") >= 0) {
                            // timed out message stating that an update is NOT necessary should be ignored
                            if (in.nextLine().indexOf("CWWKG0018I") < 0) {
                                Log.info(thisClass, "timeoutChecker", "Found a line in output.txt that contains 'Timed out' : " + theLine);
                                timeoutFound = true;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (Exception e) { /* ignore */
                    }
                }
            } else {
                throw new RuntimeException("The output.txt file in the test machine cannot be read by the test framework."
                                           + System.getProperty("line.separator")
                                           + "The output.txt file must be readable always. This is a test infrastruture exception!!!");
            }
        }

        if (timeoutFound) {
            throw new RuntimeException("Timed out msg found in output.txt - most likly a test case issue - search output.txt for 'Timed out'."
                                       + System.getProperty("line.separator")
                                       + "This exception is issued from an end of test class check and appears as an extra test case.  Fix the timed out msg issue and the test count will be correct!!!");
        }
        Log.info(thisClass, "timeoutChecker", "Time out messages were not found in the output.txt file");
    }

    /**
     * @param serverProp
     * @param upstreamServer
     * @param string
     * @throws IOException
     */
    protected static void addServerProp(LibertyServer libertyServer, String serverDir, String serverProp) throws IOException {
        String thisMethod = "addServerProp";
        final String newPropString = serverProp + "=" + InetAddress.getLocalHost().getCanonicalHostName();
        final String bootProps = serverDir + "/bootstrap.properties";
        // add property to bootstrap.properties
        Log.info(thisClass, thisMethod, serverProp + " File: " + bootProps);
        // append to bootstrap.properties
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    FileWriter writer = new FileWriter(bootProps, true);
                    writer.append(System.getProperty("line.separator"));
                    writer.append(newPropString);
                    writer.append(System.getProperty("line.separator"));
                    writer.close();
                } catch (IOException e) {

                }
                return null;
            }
        });
    }
}
