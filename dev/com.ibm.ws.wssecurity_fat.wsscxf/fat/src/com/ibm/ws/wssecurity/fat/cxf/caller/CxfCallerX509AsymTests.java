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

package com.ibm.ws.wssecurity.fat.cxf.caller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
//Added 10/2020
import org.junit.runner.RunWith;

//Added 10/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
//2/2021
import com.ibm.ws.wssecurity.fat.utils.common.UpdateServerXml;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

//Added 10/2020
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
//Added 11/2020
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 10/2020
@RunWith(FATRunner.class)
public class CxfCallerX509AsymTests {
    //orig from CL
    //private static String serverName = "com.ibm.ws.wssecurity_fat.x509caller";
    //private static LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

    //Added 10/2020
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509caller";
    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfCallerX509AsymTests.class;

    static boolean debugOnHttp = true;

    private static String portNumber = "";
    private static String portNumberSecure = "";
    private static String callerUNTClientUrl = "";
    private static String callerBadUNTClientUrl = "";
    private String methodFull = null;

    static String hostName = "localhost";

    final static String badUsernameToken = "The security token could not be authenticated or authorized";
    final static String msgExpires = "The message has expired";
    final static String badHttpsToken = "HttpsToken could not be asserted";
    final static String badHttpsClientCert = "Could not send Message.";

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the applications
     * in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        String thisMethod = "setup";

        //orig from CL
        //SharedTools.installCallbackHandler(server);

        //2/2021
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        }
        if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_wss4j.xml");
        }

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "callerclient", "com.ibm.ws.wssecurity.fat.callerclient", "test.libertyfat.caller.contract", "test.libertyfat.caller.types");
        ShrinkHelper.defaultDropinApp(server, "callertoken", "test.libertyfat.caller");

        server.addInstalledAppForValidation("callerclient");
        server.addInstalledAppForValidation("callertoken");

        server.startServer(); // check CWWKS0008I: The security service is ready.
        SharedTools.waitForMessageInLog(server, "CWWKS0008I");
        portNumber = "" + server.getHttpDefaultPort();
        portNumberSecure = "" + server.getHttpDefaultSecurePort();

        server.waitForStringInLog("port " + portNumber);
        server.waitForStringInLog("port " + portNumberSecure);
        // check  message.log
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now lis....Port 8010
        assertNotNull("defaultHttpendpoint may not started at :" + portNumber,
                      server.waitForStringInLog("CWWKO0219I.*" + portNumber));
        // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now lis....Port 8020
        assertNotNull("defaultHttpEndpoint SSL port may not be started at:" + portNumberSecure,
                      server.waitForStringInLog("CWWKO0219I.*" + portNumberSecure));

        // using the original port to send the parameters
        callerUNTClientUrl = "http://localhost:" + portNumber +
                             "/callerclient/CxfCallerSvcClient";
        // using the original port to send the parameters
        callerBadUNTClientUrl = "http://localhost:" + portNumber +
                                "/callerclient/CxfCallerBadUNTClient";
        // portNumber = "9085";                // for debugging
        Log.info(thisClass, thisMethod, "****portNumber is(2):" + portNumber);
        Log.info(thisClass, thisMethod, "****portNumberSecure is(2):" + portNumberSecure);

        return;

    }

    /**
     *
     * Test a Caller X509 Token
     *
     */
    //2/2021 to test with EE7, then the corresponding server_x509_asym.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCxfCallerX509TokenPolicy() throws Exception {
    public void testCxfCallerX509TokenPolicyEE7Only() throws Exception {

        //Orig:
        //UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");
        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        String thisMethod = "testCxfCallerX509TokenPolicy";
        methodFull = "testCxfCallerX509TokenPolicy";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TokenPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC05Service", //String strServiceName,
                        "UrnCallerToken05", //String strServicePort
                        "test3", // Expecting User ID
                        "x509TokenInUse" // Password (Bad... Testing X509 userID only)
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //2/2021 to test with EE8, then the corresponding server_x509_asym_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCxfCallerX509TokenPolicyEE8Only() throws Exception {

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym_wss4j.xml");

        String thisMethod = "testCxfCallerX509TokenPolicy";
        methodFull = "testCxfCallerX509TokenPolicy";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TokenPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC05Service", //String strServiceName,
                        "UrnCallerToken05", //String strServicePort
                        "test3", // Expecting User ID
                        "x509TokenInUse" // Password (Bad... Testing X509 userID only)
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     *
     * Test a Caller X509 Token
     *
     */
    //2/2021 to test with EE7, then the corresponding server_x509_asym.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCxfCallerX509TransportEndorsingPolicy() throws Exception {
    public void testCxfCallerX509TransportEndorsingPolicyEE7Only() throws Exception {

        // In case, the sequence on test cases are random... then need to unmark next line
        //UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        String thisMethod = "testCxfCallerX509TransportEndorsingPolicyEE7Only";
        methodFull = "testCxfCallerX509TransportEndorsingPolicy";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportEndorsingPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAC06Service", //String strServiceName,
                        "UrnCallerToken06", //String strServicePort
                        "test3", // Expecting User ID
                        "BadPassword" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //2/2021 to test with EE8, then the corresponding server_x509_asym_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCxfCallerX509TransportEndorsingPolicyEE8Only() throws Exception {

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym_wss4j.xml");

        String thisMethod = "testCxfCallerX509TransportEndorsingPolicyEE8Only";
        methodFull = "testCxfCallerX509TransportEndorsingPolicy";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportEndorsingPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAC06Service", //String strServiceName,
                        "UrnCallerToken06", //String strServicePort
                        "test3", // Expecting User ID
                        "BadPassword" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     *
     * Test a Caller UsernameToken
     *
     */
    //2/2021 to test with EE7, then the corresponding server_x509_asym.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCxfCallerHttpPolicyInX509() throws Exception {
    public void testCxfCallerHttpPolicyInX509EE7Only() throws Exception {

        // In case, the sequence on test cases are random... then need to unmark next line
        //UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        //Orig:
        //String thisMethod = "testCxfCallerHttpPolicy";
        String thisMethod = "testCxfCallerHttpPolicyInX509EE7Only";
        methodFull = "testCxfCallerHttpPolicyInx509";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "CallerHttpPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC01Service", //String strServiceName,
                        "UrnCallerToken01", //String strServicePort
                        "test3", // Expecting User ID
                        "test3" // Password for UserNameToken
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //2/2021 to test with EE8, then the corresponding server_x509_asym_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCxfCallerHttpPolicyInX509EE8Only() throws Exception {

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym_wss4j.xml");

        String thisMethod = "testCxfCallerHttpPolicyInX509EE8Only";
        methodFull = "testCxfCallerHttpPolicyInx509";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "CallerHttpPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC01Service", //String strServiceName,
                        "UrnCallerToken01", //String strServicePort
                        "test3", // Expecting User ID
                        "test3" // Password for UserNameToken
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     *
     * Test a Caller X509Token with https
     *
     */
    //2/2021 to test with EE7, then the corresponding server_x509_asym.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCxfCallerHttpsPolicyInx509() throws Exception {
    public void testCxfCallerHttpsPolicyInx509EE7Only() throws Exception {
        // In case, the sequence on test cases are random... then need to unmark next line
        //UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        //Orig:
        //String thisMethod = "testCxfCallerHttpsPolicy";
        String thisMethod = "testCxfCallerHttpsPolicyInx509EE8Only";
        methodFull = "testCxfCallerHttpsPolicyInX509";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "CallerHttpsPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAC02Service", //String strServiceName,
                        "UrnCallerToken02", //String strServicePort
                        "test2", // Expecting User ID
                        "test2" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //2/2021 to test with EE8, then the corresponding server_x509_asym_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCxfCallerHttpsPolicyInx509EE8Only() throws Exception {

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym_wss4j.xml");

        String thisMethod = "testCxfCallerHttpsPolicyInx509EE8Only";
        methodFull = "testCxfCallerHttpsPolicyInX509";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "CallerHttpsPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAC02Service", //String strServiceName,
                        "UrnCallerToken02", //String strServicePort
                        "test2", // Expecting User ID
                        "test2" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     *
     * Test a Caller X509Token with https
     *
     */

    //2/2021 to test with EE7, then the corresponding server_x509_asym.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCxfCallerNoPolicyInX509() throws Exception {
    public void testCxfCallerNoPolicyInX509EE7Only() throws Exception {

        // In case, the sequence on test cases are random... then need to unmark next line
        //UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        //Orig:
        //String thisMethod = "testCxfCallerNoPolicy";
        String thisMethod = "testCxfCallerNoPolicyInX509EE7Only";
        methodFull = "testCxfCallerNoPolicyInX509";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "CallerNoPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC03Service", //String strServiceName,
                        "UrnCallerToken03", //String strServicePort
                        "UNAUTHENTICATED", // Expecting User ID
                        "UserIDForVerifyOnly" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //2/2021 to test with EE8, then the corresponding server_x509_asym_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCxfCallerNoPolicyInX509EE8Only() throws Exception {

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym_wss4j.xml");

        String thisMethod = "testCxfCallerNoPolicyInX509EE8Only";
        methodFull = "testCxfCallerNoPolicyInX509";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "CallerNoPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC03Service", //String strServiceName,
                        "UrnCallerToken03", //String strServicePort
                        "UNAUTHENTICATED", // Expecting User ID
                        "UserIDForVerifyOnly" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     *
     * Test a Caller X509Token with https
     *
     */

    //2/2021 to test with EE7, then the corresponding server_x509_asym.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    //public void testCxfCallerHttpsNoUntPolicyInX509() throws Exception {
    public void testCxfCallerHttpsNoUntPolicyInX509EE7Only() throws Exception {
        // In case, the sequence on test cases are random... then need to unmark next line
        //UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym.xml");

        //Orig:
        //String thisMethod = "testCxfCallerHttpsNoUntPolicy";
        String thisMethod = "testCxfCallerHttpsNoUntPolicyInX509EE7Only";
        methodFull = "testCxfCallerHttpsNoUntPolicyInX509";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "CallerHttpsNoUntPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAC04Service", //String strServiceName,
                        "UrnCallerToken04", //String strServicePort
                        "test4", // Expecting User ID
                        "test4" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //2/2021 to test with EE8, then the corresponding server_x509_asym_wss4j.xml can be used
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testCxfCallerHttpsNoUntPolicyInX509EE8Only() throws Exception {

        //2/2021
        UpdateServerXml.reconfigServer(server, System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509_asym_wss4j.xml");

        String thisMethod = "testCxfCallerHttpsNoUntPolicyInX509EE8Only";
        methodFull = "testCxfCallerHttpsNoUntPolicyInX509";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "CallerHttpsNoUntPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAC04Service", //String strServiceName,
                        "UrnCallerToken04", //String strServicePort
                        "test4", // Expecting User ID
                        "test4" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf web service.
     * It needs to have caller key set to sign and encrypt the SOAPBody
     * The request is request in https.
     * Though this test is not enforced it yet.
     *
     */
    protected void testRoutine(
                               String thisMethod,
                               String callerPolicy,
                               String testMode, // Positive, positive-1, negative or negative-1... etc
                               String portNumber,
                               String portNumberSecure,
                               String strServiceName,
                               String strServicePort,
                               String untID,
                               String untPassword) throws Exception {
        testSubRoutine(
                       thisMethod,
                       callerPolicy,
                       testMode, // Positive, positive-1, negative or negative-1... etc
                       portNumber,
                       portNumberSecure,
                       strServiceName,
                       strServicePort,
                       callerUNTClientUrl,
                       "",
                       untID,
                       untPassword);

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf web service.
     * It needs to have caller key set to sign and encrypt the SOAPBody
     * The request is request in https.
     * Though this test is not enforced it yet.
     *
     */
    protected void testBadRoutine(
                                  String thisMethod,
                                  String callerPolicy,
                                  String testMode, // Positive, positive-1, negative or negative-1... etc
                                  String portNumber,
                                  String portNumberSecure,
                                  String strServiceName,
                                  String strServicePort,
                                  String untID,
                                  String untPassword) throws Exception {
        testSubRoutine(
                       thisMethod,
                       callerPolicy,
                       testMode, // Positive, positive-1, negative or negative-1... etc
                       portNumber,
                       portNumberSecure,
                       strServiceName,
                       strServicePort,
                       callerBadUNTClientUrl,
                       "Bad",
                       untID,
                       untPassword);

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf web service.
     * It needs to have caller key set to sign and encrypt the SOAPBody
     * The request is request in https.
     * Though this test is not enforced it yet.
     *
     */
    protected void testSubRoutine(
                                  String thisMethod,
                                  String callerPolicy,
                                  String testMode, // Positive, positive-1, negative or negative-1... etc
                                  String portNumber,
                                  String portNumberSecure,
                                  String strServiceName,
                                  String strServicePort,
                                  String strClientUrl,
                                  String strBadOrGood,
                                  String untID,
                                  String untPassword) throws Exception {
        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            Log.info(thisClass, methodFull, "Invoking: " + callerPolicy + ":" + testMode);
            request = new GetMethodWebRequest(strClientUrl);

            request.setParameter("serverName", serverName);
            request.setParameter("thisMethod", thisMethod);
            request.setParameter("callerPolicy", callerPolicy);
            request.setParameter("testMode", testMode);
            request.setParameter("httpDefaultPort", portNumber);
            request.setParameter("httpSecureDefaultPort", portNumberSecure);
            request.setParameter("serviceName", strServiceName);
            request.setParameter("servicePort", strServicePort);
            request.setParameter("methodFull", methodFull);
            request.setParameter("untID", untID);
            request.setParameter("untPassword", untPassword);

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            String respReceived = response.getText();
            String methodFull = thisMethod;
            if (strBadOrGood.length() > 0) {
                methodFull = thisMethod.substring(0, 4) + // "test"
                             strBadOrGood +
                             thisMethod.substring(4);
            }
            if (respReceived != null && respReceived.isEmpty()) {
                respReceived = "pass:false:'received nothing'";
            }
            Log.info(thisClass, methodFull, "'" + respReceived + "'");
            assertTrue("Failed to get back the expected text. But :" + respReceived, respReceived.contains("<p>pass:true:"));
            assertTrue("Hmm... Strange! wrong testMethod back. But :" + respReceived, respReceived.contains(">m:" + thisMethod + "<"));
        } catch (Exception e) {
            Log.info(thisClass, thisMethod, "Exception occurred:");
            System.err.println("Exception: " + e);
            throw e;
        }

        return;
    }

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
        //orig from CL
        //SharedTools.unInstallCallbackHandler(server);

        //2/2021
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/wsseccbh-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/wsseccbh-2.0.mf");

    }

    //2/2021 Mei:
    private static void printMethodName(String strMethod) {
        Log.info(thisClass, strMethod, "*****************************"
                                       + strMethod);
        System.err.println("*****************************" + strMethod);
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
