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

import static org.junit.Assert.assertTrue;

import java.io.File;

//Added 11/2020
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
//Added 10/2020
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
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
public class CxfPasswordDigestTests extends CommonTests {

    private final static Class<?> thisClass = CxfPasswordDigestTests.class;

    //private static String serviceClientUrl = "";
    //private static String serviceClientSSLUrl = "";

    private static String httpPortNumber = "";
    private static String httpsPortNumber = "";

    static String strJksLocation = "./securitykeys/sslClientDefault.jks";

    private static final String hashNonceCreatedMsg = "Username Token Created policy not enforced, Username Token Nonce policy not enforced";
    private static final String hashNonceMsg = "Username Token Nonce policy not enforced";
    private static final String hashCreatedMsg = "Username Token Created policy not enforced";
    private static final String couldNotAuth = "The security token could not be authenticated or authorized";
    private static final String hashingPolicyNotEnforced = "Password hashing policy not enforced";

    //@av
    private static final String noCBHAndPasswd = "No callback handler and no password available";

    final static String defaultClientWsdlLoc = System.getProperty("user.dir") + File.separator + "cxfclient-policies" + File.separator;
    final static String defaultHttpPort = "8010";
    final static String defaultHttpsPort = "8020";

    static private UpdateWSDLPortNum newWsdl = null;
//    static private String newClientWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.pwdigest";

    //Added 10/2020
    @Server(serverName)
    public static LibertyServer server;

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the applications
     * in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        //Added 11/2020
        WebArchive pwdigestclient_war = ShrinkHelper.buildDefaultApp("pwdigestclient", "com.ibm.ws.wssecurity.fat.pwdigestclient", "fats.cxf.pwdigest.wssec",
                                                                     "fats.cxf.pwdigest.wssec.types");
        WebArchive pwdigest_war = ShrinkHelper.buildDefaultApp("pwdigest", "com.ibm.ws.wssecurity.fat.pwdigest");
        ShrinkHelper.exportToServer(server, "", pwdigestclient_war);
        ShrinkHelper.exportToServer(server, "", pwdigest_war);
        //Added 10/2020
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, true,
                    "/pwdigestclient/CxfUntPWDigestSvcClient");
        httpPortNumber = "" + server.getHttpDefaultPort();
        httpsPortNumber = "" + server.getHttpDefaultSecurePort();
        Log.info(thisClass, "setup", "httpPortNumber: " + httpPortNumber);
        Log.info(thisClass, "setup", "httpsPortNumber: " + httpsPortNumber);
        addIgnoredServerException("CWWKW0226E");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes a jax-ws cxf
     * unt web service.
     *
     */

    @Test
    public void testPWDigestCXFSvcClientSpecifyUser() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestWebSvc",
                    "This is WSSECFVT CXF Web Service (Password Digest)", "The " + testName.getMethodName() + " test failed - did not receive the correct response");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes a jax-ws cxf
     * unt web service.
     *
     */

    @Test
    public void testPWDigestCXFSvcClientSpecifyUserSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user1", "UsrTokenPWDigestWebSvcSSL",
                    "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes a jax-ws cxf
     * unt web service.
     *
     */

    @Test
    public void testPWDigestCXFSvcClientDefaultUser() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "", "UsrTokenPWDigestWebSvc",
                    "This is WSSECFVT CXF Web Service (Password Digest)", "The " + testName.getMethodName() + " test failed - did not receive the correct response");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes a jax-ws cxf
     * unt web service.
     *
     */

    @Test
    public void testPWDigestCXFSvcClientDefaultUserSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "", "UsrTokenPWDigestWebSvcSSL",
                    "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");

    }

    @Test
    public void testPWDigestCXFSvcClientNoIdValidPw() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "NoId", "UsrTokenPWDigestWebSvc",
                    "This is WSSECFVT CXF Web Service (Password Digest)", "The " + testName.getMethodName() + " test failed - did not receive the correct response");

    }

    @Test
    public void testPWDigestCXFSvcClientNoIdValidPwSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "NoId", "UsrTokenPWDigestWebSvcSSL",
                    "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");

    }

    @Test
    public void testPWDigestCXFSvcClientBadPWOnClient() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user5", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "Bad password specified by the client - Expected Exception \"");

    }

    @Test
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testPWDigestCXFSvcClientBadPWOnClientSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user5", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Bad password specified by the client - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientBadPWOnBothSides() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user3", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "Bad password specified for user3 from the client and callback - Expected Exception \"");

    }

    @Test
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testPWDigestCXFSvcClientBadPWOnBothSidesSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user3", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Bad password specified for user3 from the client and callback - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientBadPWOnCallback() throws Exception {

        /*
         * genericTest("testPWDigestCXFSvcClientBadPWOnCallback", clientHttpUrl, "", "user6", "UsrTokenPWDigestWebSvc",
         * couldNotAuth, "Bad password specified for user6 from the server callback - Expected Exception \"") ;
         */
        //@av - Client side exception - there is no password and cbh specified for service client.
        genericTest(testName.getMethodName(), clientHttpUrl, "", "user6", "UsrTokenPWDigestWebSvc",
                    noCBHAndPasswd, "Bad password specified for user6 from the server callback - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientBadPWOnCallbackSSL() throws Exception {

        /*
         * genericTest("testPWDigestCXFSvcClientBadPWOnCallbackSSL", clientHttpsUrl, httpsPortNumber, "user6", "UsrTokenPWDigestWebSvcSSL",
         * couldNotAuth, "Bad password specified for user6 from the server callback - Expected Exception \"") ;
         *///@av
        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user6", "UsrTokenPWDigestWebSvcSSL",
                    noCBHAndPasswd, "Bad password specified for user6 from the server callback - Expected Exception \"");

    }

    @Test
    @ExpectedFFDC("java.io.IOException")
    public void testPWDigestCXFSvcMissingIdInCallback() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user4", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "Callback could not return a pw for user4 - Expected Exception \"");

    }

    @Test
    @AllowedFFDC("java.io.IOException")
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testPWDigestCXFSvcMissingIdInCallbackSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user4", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Callback could not return a pw for user4 - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientBadId() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user77", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "Bad password specified for user77 in the server callback - Expected Exception \"");

    }

    @Test
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testPWDigestCXFSvcClientBadIdSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user77", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Bad password specified for user77 in the server callback - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientCreated() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestCreatedSvc",
                    hashCreatedMsg, "Created specified in addition to Password Digest - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientCreatedSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user1", "UsrTokenPWDigestCreatedSvcSSL",
                    hashCreatedMsg, "Created specified in addition to Password Digest - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientNonceCreated() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestNonceCreatedSvc",
                    hashNonceCreatedMsg, "Nonce and Created specified in addition to Password Digest - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientNonceCreatedSSL() throws Exception {

        // SSL path only logs one policy issue
        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user1", "UsrTokenPWDigestNonceCreatedSvcSSL",
                    hashCreatedMsg, "Nonce and Created specified in addition to Password Digest - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientNonce() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestNonceSvc",
                    hashNonceMsg, "Nonce specified in addition to Password Digest - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientNonceSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user1", "UsrTokenPWDigestNonceSvcSSL",
                    hashNonceMsg, "Nonce specified in addition to Password Digest - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientNoPassword() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestNoPasswordSvc",
                    hashingPolicyNotEnforced, "No password specified - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientNoPasswordSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user1", "UsrTokenPWDigestNoPasswordSvcSSL",
                    hashingPolicyNotEnforced, "No password specified - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientaltCallback() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback.xml");
        genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback1", "UsrTokenPWDigestWebSvc",
                    "This is WSSECFVT CXF Web Service (Password Digest)", "The " + testName.getMethodName() + " test failed - did not receive the correct response"); //@av

        return;

    }

    @Test
    public void testPWDigestCXFSvcClientaltCallbackSSL() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback.xml");
        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback1", "UsrTokenPWDigestWebSvcSSL",
                    "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    @Test
    @ExpectedFFDC("java.io.IOException")
    public void testPWDigestCXFSvcClientaltCallbackBadUser() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback2", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    @Test
    @AllowedFFDC("java.io.IOException")
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testPWDigestCXFSvcClientaltCallbackBadUserSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback2", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    @Test
    @AllowedFFDC("java.io.IOException")
    //@av
    public void testPWDigestCXFSvcClientBadClCallback() throws Exception {

        /*
         * genericTest("testPWDigestCXFSvcClientBadClCallback", clientHttpUrl, "", "badCallback", "UsrTokenPWDigestWebSvc",
         * couldNotAuth, "The testPWDigestCXFSvcClientBadClCallback test failed - did not receive the correct response") ;
         *///@av
           //@av - Client side exception - there is no password and bad cbh specified for service client.
        genericTest(testName.getMethodName(), clientHttpUrl, "", "badCallback", "UsrTokenPWDigestWebSvc",
                    noCBHAndPasswd, "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    @Test
    @AllowedFFDC("java.io.IOException")
    //@av
    public void testPWDigestCXFSvcClientBadClCallbackSSL() throws Exception {

        /*
         * genericTest("testPWDigestCXFSvcClientBadClCallbackSSL", clientHttpUrl, httpsPortNumber, "badCallback", "UsrTokenPWDigestWebSvcSSL",
         * couldNotAuth, "The testPWDigestCXFSvcClientBadClCallbackSSL test failed - did not receive the correct response") ;
         *///@av
        genericTest(testName.getMethodName(), clientHttpUrl, httpsPortNumber, "badCallback", "UsrTokenPWDigestWebSvcSSL",
                    noCBHAndPasswd, "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    @Test
    public void testPWDigestCXFSvcClientClCallbackInServerXml() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback.xml");
        genericTest(testName.getMethodName(), clientHttpUrl, "", "user88", "UsrTokenPWDigestWebSvc",
                    "This is WSSECFVT CXF Web Service (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    @Test
    public void testPWDigestCXFSvcClientClCallbackInServerXmlSSL() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback.xml");
        genericTest(testName.getMethodName(), clientHttpUrl, httpsPortNumber, "user88", "UsrTokenPWDigestWebSvcSSL",
                    "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    @Test
    public void testPWDigestCXFSvcClientClNoHash() throws Exception {

        String newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "UsrTokenPWDigestWebSvcNoHash.wsdl",
                                                defaultClientWsdlLoc + "UsrTokenPWDigestWebSvcNoHashUpdated.wsdl");
        Log.info(thisClass, "testPWDigestCXFSvcClientClNoHash", "Using " + newClientWsdl);
        genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestWebSvc", newClientWsdl,
                    "[Password hashing policy not enforced]", "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    @Override
    public void genericTest(String thisMethod, String useThisUrl, String securePort, String id, String theWsdl, String verifyMsg, String failMsg) throws Exception {
        genericTest(thisMethod, useThisUrl, securePort, id, theWsdl, null, verifyMsg, failMsg);

    }

    public void genericTest(String thisMethod, String useThisUrl, String securePort, String id, String theWsdl, String newClientWsdl, String verifyMsg,
                            String failMsg) throws Exception {

        String respReceived = null;

        Log.info(thisClass, thisMethod, "****************************** Starting " + thisMethod + " via PwDigest genericTest ******************************");
        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            Log.info(thisClass, thisMethod, "Invoking: " + useThisUrl);
            request = new GetMethodWebRequest(useThisUrl);

            request.setParameter("testName", thisMethod);
            request.setParameter("httpDefaultPort", httpPortNumber);
            request.setParameter("httpSecureDefaultPort", securePort);
            request.setParameter("setId", id);
            request.setParameter("theWsdl", theWsdl);
            if (newClientWsdl != null) {
                request.setParameter("clientWsdl", newClientWsdl);
                Log.info(thisClass, thisMethod, "Not NULL clientWsdl: " + newClientWsdl);
            } else {
                Log.info(thisClass, thisMethod, "NULL clientWsdl: " + newClientWsdl);
                request.setParameter("clientWsdl", "");
            }

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response from Password Digest Service client: "
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

        assertTrue(failMsg + verifyMsg + "; Instead, received: " + respReceived, respReceived.contains(verifyMsg));
        Log.info(thisClass, thisMethod, thisMethod + ": PASS");

        return;

    }

    public String updateClientWsdl(String origClientWsdl,
                                   String updatedClientWsdl) {

        try {
            if (httpPortNumber.equals(defaultHttpPort)) {
                Log.info(thisClass, "updateClientWsdl", "Test should use " + origClientWsdl + " as the client WSDL");
                return origClientWsdl;
            } else { // port number needs to be updated
                newWsdl = new UpdateWSDLPortNum(origClientWsdl, updatedClientWsdl);
                newWsdl.updatePortNum(defaultHttpPort, httpPortNumber);
                Log.info(thisClass, "updateClientWsdl", "Test should use " + updatedClientWsdl + " as the client WSDL");

                return updatedClientWsdl;
            }
        } catch (Exception ex) {
            Log.info(thisClass, "updateClientWsdl",
                     "Failed updating the client wsdl try using the original");
            newWsdl = null;
            return origClientWsdl;
        }
    }

}
