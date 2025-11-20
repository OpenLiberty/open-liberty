/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.ecdh;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

/*
 * This test suite tests the custom policy assertions we added to support using ECDH-ES with WS-SecurityPolicy.
 * These are custom AlgorithmSuites and are defined like this in the callertoken.wsdl file:
 *
 *      <sp:AlgorithmSuite>
 *         <wsp:Policy>
 *            <sp-cxf:Basic192GCMSha256ECDHES xmlns:sp-cxf="http://cxf.apache.org/custom/security-policy"/>
 *         </wsp:Policy>
 *      </sp:AlgorithmSuite>
 *
 * Each test defines which AlgorithmSuite it tests, as well as whether or not its using a supporting UNT or a UNT as the protection token.
 * Only the Supporting UNT tests actually use ECDH-ES encryption, since UNT security policy is outside of that scope.
 */
@RunWith(FATRunner.class)
public class CxfECDHCallerTests {

    static final private String serverName = "com.ibm.ws.wssecurity_fat.caller";
    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfECDHCallerTests.class;

    private static String errMsgVersion = "";

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

        //issue 23060
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");

        //issue 23599
        ShrinkHelper.defaultDropinApp(server, "callerclient", "com.ibm.ws.wssecurity.fat.callerclient",
                                      "test.libertyfat.caller.contract", "test.libertyfat.caller.types");
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
                             "/callerclient/ECDHCxfCallerSvcClient";

        // portNumber = "9085";                // for debugging
        Log.info(thisClass, thisMethod, "****portNumber is(2):" + portNumber);
        Log.info(thisClass, thisMethod, "****portNumberSecure is(2):" + portNumberSecure);

        return;

    }

    /**
     *
     * Test a Caller UsernameToken as ProtectionToken with the Basic128GCMSha256ECDHES AlgorithmSuite.
     *
     * This is a positive test
     *
     */
    @Test
    public void testCxfBasic128GCMSha256ECDHESWithUserNameToken() throws Exception {

        String thisMethod = "testCxfBasic128GCMSha256ECDHESWithUserNameToken";
        methodFull = "testCxfBasic128GCMSha256ECDHESWithUserNameToken";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "Basic128GCMSha256ECDHESWithUserNameToken", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC01Service", //String strServiceName,
                        "UrnCallerToken01", //String strServicePort
                        "secp256r1", // Expecting User ID
                        "security" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     *
     * Test a Caller UsernameToken Supporting Token with an x509 protection token with the Basic128GCMSha256ECDHES AlgorithmSuite.
     *
     * This is a positive test
     *
     */
    @Test
    public void testCxfBasic128GCMSha256ECDHESWithSupportingUNT() throws Exception {

        String thisMethod = "testCxfBasic128GCMSha256ECDHESWithSupportingUNT";
        methodFull = "testCxfBasic128GCMSha256ECDHESWithSupportingUNT";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "Basic128GCMSha256ECDHESWithSupportingUNT", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC02Service", //String strServiceName,
                        "UrnCallerToken02", //String strServicePort
                        "secp256r1", // Expecting User ID
                        "security" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     *
     * Test a Caller UsernameToken Supporting Token with an x509 protection token with the Basic192GCMSha256ECDHES AlgorithmSuite.
     *
     * This is a negative test, as using AES192_GCM for encryption violates BSP rule R55620
     *
     */
    @Test
    @ExpectedFFDC("org.apache.wss4j.common.ext.WSSecurityException")
    public void testCxfBasic192GCMSha256ECDHESWithSupportingUNT() throws Exception {

        String thisMethod = "testCxfBasic192GCMSha256ECDHESWithSupportingUNT";
        methodFull = "testCxfBasic192GCMSha256ECDHESWithSupportingUNT";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "Basic192GCMSha256ECDHESWithSupportingUNT", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC04Service", //String strServiceName,
                        "UrnCallerToken04", //String strServicePort
                        "secp256r1", // Expecting User ID
                        "security" // Password
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     *
     * Test a Caller UsernameToken Supporting Token with an x509 protection token with the Basic256GCMSha256ECDHES AlgorithmSuite.
     *
     * This is a positive test
     *
     */
    @Test
    public void testCxfBasic256GCMSha256ECDHESWithSupportingUNT() throws Exception {

        String thisMethod = "testCxfBasic256GCMSha256ECDHESWithSupportingUNT";
        methodFull = "testCxfBasic256GCMSha256ECDHESWithSupportingUNT";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "Basic256GCMSha256ECDHESWithSupportingUNT", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAC06Service", //String strServiceName,
                        "UrnCallerToken06", //String strServicePort
                        "secp256r1", // Expecting User ID
                        "security" // Password
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
                       untPassword,
                       null);

        return;
    }

    protected void testRoutine(
                               String thisMethod,
                               String callerPolicy,
                               String testMode, // Positive, positive-1, negative or negative-1... etc
                               String portNumber,
                               String portNumberSecure,
                               String strServiceName,
                               String strServicePort,
                               String untID,
                               String untPassword,
                               String errMsgVersion) throws Exception {
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
                       untPassword,
                       errMsgVersion);

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
                       untPassword,
                       null);

        return;
    }

    protected void testBadRoutine(
                                  String thisMethod,
                                  String callerPolicy,
                                  String testMode, // Positive, positive-1, negative or negative-1... etc
                                  String portNumber,
                                  String portNumberSecure,
                                  String strServiceName,
                                  String strServicePort,
                                  String untID,
                                  String untPassword,
                                  String errMsgVersion) throws Exception {
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
                       untPassword,
                       errMsgVersion);

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
                                  String untPassword,
                                  String errMsgVersion) throws Exception {
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

            request.setParameter("errorMsgVersion", errMsgVersion);

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
            Log.info(thisClass, thisMethod, "Exception occurred: " + e);
            //System.err.println("Exception: " + e);
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

        Log.info(thisClass, "tearDown", "deleting usr/extension/lib/com.ibm.ws.wssecurity.example.cbh.jar");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/com.ibm.ws.wssecurity.example.cbh.jar");
        Log.info(thisClass, "tearDown", "deleting usr/extension/lib/features/wsseccbh-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/wsseccbh-1.0.mf");
        Log.info(thisClass, "tearDown", "deleting usr/extension/lib/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
        Log.info(thisClass, "tearDown", "deleting usr/extension/lib/features/wsseccbh-2.0.mf");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/wsseccbh-2.0.mf");

    }

    private static void printMethodName(String strMethod) {
        Log.info(thisClass, strMethod, "*****************************"
                                       + strMethod);
        System.err.println("*****************************" + strMethod);
    }

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
