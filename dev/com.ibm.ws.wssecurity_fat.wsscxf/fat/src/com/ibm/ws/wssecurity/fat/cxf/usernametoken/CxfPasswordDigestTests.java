/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
import java.util.Set;

//Added 11/2020
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
//Added 10/2020
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
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
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
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

    //2/2021 to use EE7 or EE8 pwdCallBackHandler
    private static String pwdCBHVersion = "";

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

        //2/2021
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        //The feature flag here is for the AltClientPWDigestCallbackHandler used in the test methods with id 'altCallback1' and 'altCallback2'
        //The PWDigestCallbackHandler version doesn't use this flag but determined by the server.xml/server_wss4j.xml/server_withClCallback.xml/server_withClCallback_wss4j.xml
        if (features.contains("jaxws-2.2")) {
            pwdCBHVersion = "EE7";
        }
        if (features.contains("jaxws-2.3")) {
            pwdCBHVersion = "EE8";
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        } //End 2/2021

        //Added 11/2020
        WebArchive pwdigestclient_war = ShrinkHelper.buildDefaultApp("pwdigestclient", "com.ibm.ws.wssecurity.fat.pwdigestclient", "fats.cxf.pwdigest.wssec",
                                                                     "fats.cxf.pwdigest.wssec.types");
        WebArchive pwdigest_war = ShrinkHelper.buildDefaultApp("pwdigest", "com.ibm.ws.wssecurity.fat.pwdigest");
        ShrinkHelper.exportToServer(server, "", pwdigestclient_war);
        ShrinkHelper.exportToServer(server, "", pwdigest_war);

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

    //4/2021
    @AllowedFFDC(value = { "java.lang.ClassNotFoundException" })
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

    //4/2021
    //@AllowedFFDC(value = { "java.net.MalformedURLException" })
    //5/2021 added PrivilegedActionExc, NoSuchMethodExc as a result of java11 and ee8
    @AllowedFFDC(value = { "java.net.MalformedURLException", "java.security.PrivilegedActionException",
                           "java.lang.NoSuchMethodException" })
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

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
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

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    @Test
    public void testPWDigestCXFSvcClientNoIdValidPwSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "NoId", "UsrTokenPWDigestWebSvcSSL",
                    "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");
    }

    @Test
    //2/2021
    @AllowedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testPWDigestCXFSvcClientBadPWOnClient() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user5", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "Bad password specified by the client - Expected Exception \"");

    }

    //2/2021
    //The following 4 test methods:
    //testPWDigestCXFSvcClientBadPWOnClientSSL
    //testPWDigestCXFSvcClientBadPWOnBothSidesSSL
    //testPWDigestCXFSvcMissingIdInCallbackSSL
    //testPWDigestCXFSvcClientBadIdSSL
    //can't work with the combined exceptions:
    //@ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException" })
    //EE7 test will fail with [An FFDC reporting org.apache.wss4j.common.ext.WSSecurityException was expected but none was found.]
    //EE8 test will fail with [An FFDC reporting org.apache.ws.security.WSSecurityException was expected but none was found.]
    //So split them for EE7only an EE8only respectively

    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    //Orig:
    //public void testPWDigestCXFSvcClientBadPWOnClientSSL() throws Exception {
    public void testPWDigestCXFSvcClientBadPWOnClientSSLEE7Only() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user5", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Bad password specified by the client - Expected Exception \"");

    }

    //2/2021 run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testPWDigestCXFSvcClientBadPWOnClientSSLEE8Only() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user5", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Bad password specified by the client - Expected Exception \"");

    }

    @Test
    //2/2021
    @AllowedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testPWDigestCXFSvcClientBadPWOnBothSides() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user3", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "Bad password specified for user3 from the client and callback - Expected Exception \"");

    }

    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    //Orig:
    //public void testPWDigestCXFSvcClientBadPWOnBothSidesSSL() throws Exception {
    public void testPWDigestCXFSvcClientBadPWOnBothSidesSSLEE7Only() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user3", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Bad password specified for user3 from the client and callback - Expected Exception \"");

    }

    //2/2021 run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testPWDigestCXFSvcClientBadPWOnBothSidesSSLEE8Only() throws Exception {

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

    //2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @AllowedFFDC("java.io.IOException")
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    //Orig:
    //public void testPWDigestCXFSvcMissingIdInCallbackSSL() throws Exception {
    public void testPWDigestCXFSvcMissingIdInCallbackSSLEE7Only() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user4", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Callback could not return a pw for user4 - Expected Exception \"");

    }

    //2/2021 run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC("java.io.IOException")
    @ExpectedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testPWDigestCXFSvcMissingIdInCallbackSSLEE8Only() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user4", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Callback could not return a pw for user4 - Expected Exception \"");

    }

    @Test
    //2/2021
    @AllowedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testPWDigestCXFSvcClientBadId() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user77", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "Bad password specified for user77 in the server callback - Expected Exception \"");

    }

    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    //Orig:
    //public void testPWDigestCXFSvcClientBadIdSSL() throws Exception {
    public void testPWDigestCXFSvcClientBadIdSSLEE7Only() throws Exception {
        //Orig:
        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user77", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Bad password specified for user77 in the server callback - Expected Exception \"");

    }

    //2/2021 run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testPWDigestCXFSvcClientBadIdSSLEE8Only() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user77", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "Bad password specified for user77 in the server callback - Expected Exception \"");

    }

    @Test
    public void testPWDigestCXFSvcClientCreated() throws Exception {

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestCreatedSvc",
                    hashCreatedMsg, "Created specified in addition to Password Digest - Expected Exception \"");

    }

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
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

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException", "java.lang.ClassNotFoundException" })
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

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    @Test
    public void testPWDigestCXFSvcClientNonceSSL() throws Exception {

        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user1", "UsrTokenPWDigestNonceSvcSSL",
                    hashNonceMsg, "Nonce specified in addition to Password Digest - Expected Exception \"");

    }

    //2/2021
    //@AV999 TODO
    //In the old code, failure happens at the provider side complaining that the Password hashing policy not enforced
    //In the new code, failure happens at the time of service construction in the service client, complaining - W Failed to build the policy 'UserNameToken1':Invalid Policy
    //regarding {http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}NoPassword
    //This leads to internal error
    //either we use two different policies or update the service client to expect the internal error
    //I think the policy is invalid to have both nopassword and hashpassword

    //2/2021 Per above comment, not running this test
    //@Test
    public void testPWDigestCXFSvcClientNoPassword() throws Exception {
        //Mei:
        //String newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "UsrTokenPWDigestNoPasswordSvc.wsdl",
        //defaultClientWsdlLoc + "UsrTokenPWDigestNoPasswordSvcUpdated.wsdl"); //@AV999
        //end
        genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestNoPasswordSvc",
                    hashingPolicyNotEnforced, "No password specified - Expected Exception \"");

    }

    //2/2021 Per above comment, not running this test
    //@Test
    public void testPWDigestCXFSvcClientNoPasswordSSL() throws Exception {

        //Mei:
        //String newClientWsdl = updateClientWsdl(defaultClientWsdlLoc + "UsrTokenPWDigestNoPasswordSvc.wsdl",
        //defaultClientWsdlLoc + "UsrTokenPWDigestNoPasswordSvcUpdated.wsdl"); //@AV999 this is not a valid testcase
        //End
        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "user1", "UsrTokenPWDigestNoPasswordSvcSSL",
                    hashingPolicyNotEnforced, "No password specified - Expected Exception \"");

    }

    //2/2021 run with EE7 and the corresponding server_withClCallback.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    public void testPWDigestCXFSvcClientaltCallbackEE7Only() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback.xml");
        //2/2021 Orig:
        //genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback1", "UsrTokenPWDigestWebSvc",
        //            "This is WSSECFVT CXF Web Service (Password Digest)", "The " + testName.getMethodName() + " test failed - did not receive the correct response"); //@av

        //2/2021
        genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback1", "UsrTokenPWDigestWebSvc",
                    "This is WSSECFVT CXF Web Service (Password Digest)", "The " + testName.getMethodName() + " test failed - did not receive the correct response", pwdCBHVersion);

        return;

    }

    //2/2021 run with EE8 and the corresponding server_withClCallback_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testPWDigestCXFSvcClientaltCallbackEE8Only() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback_wss4j.xml");
        //2/2021 Orig:
        //genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback1", "UsrTokenPWDigestWebSvc",
        //            "This is WSSECFVT CXF Web Service (Password Digest)", "The " + testName.getMethodName() + " test failed - did not receive the correct response"); //@av

        //2/2021
        genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback1", "UsrTokenPWDigestWebSvc",
                    "This is WSSECFVT CXF Web Service (Password Digest)", "The " + testName.getMethodName() + " test failed - did not receive the correct response", pwdCBHVersion);
        return;

    }

    //2/2021 run with EE7 and the corresponding server_withClCallback.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    public void testPWDigestCXFSvcClientaltCallbackSSLEE7Only() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback.xml");
        //Orig:
        //genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback1", "UsrTokenPWDigestWebSvcSSL",
        //            "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
        //            "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        //2/2021
        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback1", "UsrTokenPWDigestWebSvcSSL",
                    "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response", pwdCBHVersion);

        return;

    }

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    //2/2021 run with EE8 and the corresponding server_withClCallback_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testPWDigestCXFSvcClientaltCallbackSSLEE8Only() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback_wss4j.xml");
        //Orig:
        //genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback1", "UsrTokenPWDigestWebSvcSSL",
        //            "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
        //            "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        //2/2021
        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback1", "UsrTokenPWDigestWebSvcSSL",
                    "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response", pwdCBHVersion);

        return;

    }

    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC("java.io.IOException")
    //Orig:
    //public void testPWDigestCXFSvcClientaltCallbackBadUser() throws Exception {
    public void testPWDigestCXFSvcClientaltCallbackBadUserEE7Only() throws Exception {

        //Orig:
        //genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback2", "UsrTokenPWDigestWebSvc",
        //            couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        //2/2021
        genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback2", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response", pwdCBHVersion);

        return;

    }

    //2/2021 run with EE8 and the corresponding server_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC("java.io.IOException")
    public void testPWDigestCXFSvcClientaltCallbackBadUserEE8Only() throws Exception {

        //Orig:
        //genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback2", "UsrTokenPWDigestWebSvc",
        //            couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        //2/2021
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        genericTest(testName.getMethodName(), clientHttpUrl, "", "altCallback2", "UsrTokenPWDigestWebSvc",
                    couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response", pwdCBHVersion);

        return;

    }

    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @AllowedFFDC("java.io.IOException")
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    //Orig:
    //public void testPWDigestCXFSvcClientaltCallbackBadUserSSL() throws Exception {
    public void testPWDigestCXFSvcClientaltCallbackBadUserSSLEE7Only() throws Exception {

        //Orig:
        //genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback2", "UsrTokenPWDigestWebSvcSSL",
        //            couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        //2/2021
        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback2", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response", pwdCBHVersion);

        return;

    }

    //2/2021 run with EE8 and the corresponding server_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.io.IOException", "org.apache.wss4j.common.ext.WSSecurityException" })
    public void testPWDigestCXFSvcClientaltCallbackBadUserSSLEE8Only() throws Exception {

        //Orig:
        //genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback2", "UsrTokenPWDigestWebSvcSSL",
        //            couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        //2/2021
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        genericTest(testName.getMethodName(), clientHttpsUrl, httpsPortNumber, "altCallback2", "UsrTokenPWDigestWebSvcSSL",
                    couldNotAuth, "The " + testName.getMethodName() + " test failed - did not receive the correct response", pwdCBHVersion);

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
         */
        //@av

        genericTest(testName.getMethodName(), clientHttpUrl, httpsPortNumber, "badCallback", "UsrTokenPWDigestWebSvcSSL",
                    noCBHAndPasswd, "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    //2/2021 run with EE7 and the corresponding server_withClCallback.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    public void testPWDigestCXFSvcClientClCallbackInServerXmlEE7Only() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback.xml");

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user88", "UsrTokenPWDigestWebSvc",
                    "This is WSSECFVT CXF Web Service (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    //2/2021 run with EE8 and the corresponding server_withClCallback_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testPWDigestCXFSvcClientClCallbackInServerXmlEE8Only() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback_wss4j.xml");

        genericTest(testName.getMethodName(), clientHttpUrl, "", "user88", "UsrTokenPWDigestWebSvc",
                    "This is WSSECFVT CXF Web Service (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    //2/2021 run with EE7 and the corresponding server_withClCallback.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    public void testPWDigestCXFSvcClientClCallbackInServerXmlSSLEE7Only() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback.xml");

        genericTest(testName.getMethodName(), clientHttpUrl, httpsPortNumber, "user88", "UsrTokenPWDigestWebSvcSSL",
                    "This is WSSECFVT CXF Web Service with SSL (Password Digest)",
                    "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        return;

    }

    //4/2021
    @AllowedFFDC(value = { "java.net.MalformedURLException" })
    //2/2021 run with EE8 and the corresponding server_withClCallback_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testPWDigestCXFSvcClientClCallbackInServerXmlSSLEE8Only() throws Exception {

        // reconfig server
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_withClCallback_wss4j.xml");

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
        //Orig:
        //genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestWebSvc", newClientWsdl,
        //            "[Password hashing policy not enforced]", "The " + testName.getMethodName() + " test failed - did not receive the correct response");

        //2/2021
        genericTest(testName.getMethodName(), clientHttpUrl, "", "user1", "UsrTokenPWDigestWebSvc", newClientWsdl,
                    "[Password hashing policy not enforced]", "The " + testName.getMethodName() + " test failed - did not receive the correct response", pwdCBHVersion);

        return;

    }

    //6/2021 The @Override caused " error: method does not override or implement a method from a supertype"
    //Commented out for now
    //@Override
    //2/2021 Orig:
    //public void genericTest(String thisMethod, String useThisUrl, String securePort, String id, String theWsdl, String verifyMsg, String failMsg) throws Exception {
    //    genericTest(thisMethod, useThisUrl, securePort, id, theWsdl, null, verifyMsg, failMsg);

    //}

    //2/2021
    public void genericTest(String thisMethod, String useThisUrl, String securePort, String id, String theWsdl, String verifyMsg, String failMsg) throws Exception {
        genericTest(thisMethod, useThisUrl, securePort, id, theWsdl, null, verifyMsg, failMsg, null);

    }

    //2/2021
    public void genericTest(String thisMethod, String useThisUrl, String securePort, String id, String theWsdl, String verifyMsg, String failMsg,
                            String cbhVersion) throws Exception {
        genericTest(thisMethod, useThisUrl, securePort, id, theWsdl, null, verifyMsg, failMsg, cbhVersion);

    }

    //2/2021 Orig:
    //public void genericTest(String thisMethod, String useThisUrl, String securePort, String id, String theWsdl, String , String verifyMsg,
    //                        String failMsg) throws Exception {

    //2/2021
    public void genericTest(String thisMethod, String useThisUrl, String securePort, String id, String theWsdl, String newClientWsdl, String verifyMsg,
                            String failMsg, String pwdcbhVersion) throws Exception {

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

            //2/2021
            request.setParameter("pwdCallBackhandlerVersion", pwdCBHVersion);

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

    //2/2021
    public static void copyServerXml(String copyFromFile) throws Exception {

        try {
            String serverFileLoc = (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();
            Log.info(thisClass, "copyServerXml", "Copying: " + copyFromFile
                                                 + " to " + serverFileLoc);
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
                                                   serverFileLoc, "server.xml", copyFromFile);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

}
