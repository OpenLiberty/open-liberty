/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.x509migtoken;

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import javax.crypto.Cipher;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//issue 24772 - new lite test to detect runtime change if the expected fault messages are not available
@SkipForRepeat({ RepeatWithEE7cbh20.ID, EE9_FEATURES, EE10_FEATURES })
@RunWith(FATRunner.class)
public class CxfX509MigSymEE7LiteTests extends CommonTests {

    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509migsym.ee7lite";
    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfX509MigSymEE7LiteTests.class;

    static boolean debugOnHttp = true;

    private static String portNumber = "";

    private static String x509MigSymClientUrl = "";
    private String methodFull = null;

    static String hostName = "localhost";

    final static String badUsernameToken = "The security token could not be authenticated or authorized";
    final static String msgExpires = "The message has expired";
    final static String badHttpsToken = "HttpsToken could not be asserted";
    final static String badHttpsClientCert = "Could not send Message.";

    @BeforeClass
    public static void setUp() throws Exception {

        String thisMethod = "setup";

        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha512.xml");

        ShrinkHelper.defaultDropinApp(server, "x509migclient", "com.ibm.ws.wssecurity.fat.x509migclient", "test.libertyfat.x509mig.contract", "test.libertyfat.x509mig.types");
        ShrinkHelper.defaultDropinApp(server, "x509migtoken", "basicplcy.wssecfvt.test");

        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, true,
                    "/x509migbadclient/CxfX509MigBadSvcClient");

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
        x509MigSymClientUrl = "http://localhost:" + portNumber +
                              "/x509migclient/CxfX509MigSvcClient";

        // portNumber = "9085";                // for debugging
        Log.info(thisClass, thisMethod, "****portNumber is(2):" + portNumber);
        Log.info(thisClass, thisMethod, "****portNumberSecure is(2):" + portNumberSecure);

        return;

    }

    /**
     * This test invokes a complex policy
     * It encrypt the username token, sign the bod and encrypt it.
     * using Basic192
     * It's a negative test unless we put in the UnrestrictedPolicySecurityPolicy
     * Once it's implemented, the test become positive and test in here is set to negative on regular tests
     *
     * issue 24772 to verify the client and server return the expected fault message
     * when properties ws-security.return.security.error="true" is set
     **/

    @Test
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" })
    public void testBasic192ServiceEnableReturnError() throws Exception {
        String thisMethod = "testBasic192Service";
        methodFull = "testBasic192Service";

        if ((Cipher.getMaxAllowedKeyLength("AES") >= 192) == true) {
            try {
                testRoutine(
                            thisMethod, //String thisMethod,
                            "SymEncSignBasic192Policy", // Testing policy name
                            "negative", // Positive, positive-1, negative or negative-1... etc
                            portNumber, //String portNumber,
                            "", //String portNumberSecure
                            "FatBAX31Service", //String strServiceName,
                            "UrnX509Token31" //String strServicePort
                );
            } catch (Exception e) {
                throw e;
            }

            Log.info(thisClass, thisMethod, "In test method testBasic192ServiceEnableReturnError, ");
            String messageToSearch = "org.apache.cxf.binding.soap.SoapFault: BSP:R5620: Any ED_ENCRYPTION_METHOD Algorithm attribute MUST have a value of \\\"http://www.w3.org/2001/04/xmlenc#tripledes-cbc\\\", \\\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\\\" or \\\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\\\"";
            Log.info(thisClass, thisMethod,
                     "Searching the input string in messages.log: " + messageToSearch);
            long startSearch = System.currentTimeMillis(); // Get the time before the search to give proper timeout if necessary
            String result_client = server.waitForStringInLog(messageToSearch, 200000);
            Log.info(thisClass, thisMethod, "The search result in messages.log is: " + result_client + ", took :" + (System.currentTimeMillis() - startSearch) + " ms.");

            String traceToSearch = "Fault occured, printing Exception cause to trace.";
            Log.info(thisClass, thisMethod, "Searching the input string in trace.log: " + traceToSearch);
            startSearch = System.currentTimeMillis(); // Get the time before the search to give proper timeout if necessary
            String result_server = server.waitForStringInTrace(traceToSearch, 40000);
            Log.info(thisClass, thisMethod, "The search result in trace.log is: " + result_server + ", took :" + (System.currentTimeMillis() - startSearch) + " ms.");

            if (result_client == null) {
                fail("The message " + "'" + messageToSearch + "'" + " is not found in messages.log ");

            } else if (result_server == null) {
                fail("The message " + "'" + traceToSearch + "'" + " is not found in trace.log ");

            } else if ((result_client != null) && (result_server != null)) {
                Log.info(thisClass, thisMethod, "The test passed with the expected message in messages.log: " + result_client);
                Log.info(thisClass, thisMethod, "The test passed with the expected message in trace.log: " + result_server);
            }

        }

        return;
    }

    /**
     * This test invokes a complex policy
     * It encrypt the username token, sign the bod and encrypt it.
     * using Basic192
     * It's a negative test unless we put in the UnrestrictedPolicySecurityPolicy
     * Once it's implemented, the test become positive and test in here is set to negative on regular tests
     *
     * issue 24772 to verify the client and server return the expected fault message
     * when default properties ws-security.return.security.error="false" is set
     **/

    @Test
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" })
    public void testBasic192ServiceDisableReturnError() throws Exception {
        String thisMethod = "testBasic192Service";
        methodFull = "testBasic192Service";

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha512_disable_return_error.xml");
        //reconfigServer already does setMarkToEndOfLog() but not set for trace
        server.setTraceMarkToEndOfDefaultTrace();

        if ((Cipher.getMaxAllowedKeyLength("AES") >= 192) == true) {
            try {
                testRoutine(
                            thisMethod, //String thisMethod,
                            "SymEncSignBasic192Policy", // Testing policy name
                            "negative", // Positive, positive-1, negative or negative-1... etc
                            portNumber, //String portNumber,
                            "", //String portNumberSecure
                            "FatBAX31Service", //String strServiceName,
                            "UrnX509Token31"); //String strServicePort

            } catch (Exception e) {
                throw e;
            }

            Log.info(thisClass, thisMethod, "In test method testBasic192ServiceDisableReturnError, ");
            String messageToSearch = "org.apache.cxf.binding.soap.SoapFault: A security error was encountered when verifying the message";
            Log.info(thisClass, thisMethod, "Searching the input string in messages.log: " + messageToSearch);
            long startSearch = System.currentTimeMillis(); // Get the time before the search to give proper timeout if necessary
            String result_client = server.waitForStringInLog(messageToSearch, 20000);
            Log.info(thisClass, thisMethod, "The search result in messages.log is: " + result_client + ", took :" + (System.currentTimeMillis() - startSearch) + " ms.");

            String traceToSearch = "Fault occured, printing Exception cause to trace.";
            Log.info(thisClass, thisMethod, "Searching the input string in trace.log: " + traceToSearch);
            startSearch = System.currentTimeMillis(); // Get the time before the search to give proper timeout if necessary
            String result_server = server.waitForStringInTraceUsingMark(traceToSearch, 40000);
            Log.info(thisClass, thisMethod, "The search result in trace.log is: " + result_server + ", took :" + (System.currentTimeMillis() - startSearch) + " ms.");

            if (result_client == null) {
                fail("The message " + "'" + messageToSearch + "'" + " is not found in messages.log ");

            } else if (result_server == null) {
                fail("The message " + "'" + traceToSearch + "'" + " is not found in trace.log ");

            } else if ((result_client != null) && (result_server != null)) {
                Log.info(thisClass, thisMethod, "The test passed with the expected message in messages.log: " + result_client);
                Log.info(thisClass, thisMethod, "The test passed with the expected message in trace.log: " + result_server);

            }

        }

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf web service.
     * It needs to have X509 key set to sign and encrypt the SOAPBody
     * The request is request in https.
     * Though this test is not enforced it yet.
     *
     */

    protected void testRoutine(
                               String thisMethod,
                               String x509Policy,
                               String testMode, // Positive, positive-1, negative or negative-1... etc
                               String portNumber,
                               String portNumberSecure,
                               String strServiceName,
                               String strServicePort) throws Exception {
        testSubRoutine(
                       thisMethod,
                       x509Policy,
                       testMode, // Positive, positive-1, negative or negative-1... etc
                       portNumber,
                       portNumberSecure,
                       strServiceName,
                       strServicePort,
                       x509MigSymClientUrl,
                       "");

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf web service.
     * It needs to have X509 key set to sign and encrypt the SOAPBody
     * The request is request in https.
     * Though this test is not enforced it yet.
     *
     */
    protected void testSubRoutine(
                                  String thisMethod,
                                  String x509Policy,
                                  String testMode, // Positive, positive-1, negative or negative-1... etc
                                  String portNumber,
                                  String portNumberSecure,
                                  String strServiceName,
                                  String strServicePort,
                                  String strClientUrl,
                                  String strBadOrGood) throws Exception {
        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            Log.info(thisClass, methodFull, "Invoking: " + x509Policy + ":" + testMode);
            request = new GetMethodWebRequest(strClientUrl);

            request.setParameter("serverName", serverName);
            request.setParameter("thisMethod", thisMethod);
            request.setParameter("x509Policy", x509Policy);
            request.setParameter("testMode", testMode);
            request.setParameter("httpDefaultPort", portNumber);
            request.setParameter("httpSecureDefaultPort", portNumberSecure);
            request.setParameter("serviceName", strServiceName);
            request.setParameter("servicePort", strServicePort);
            request.setParameter("methodFull", methodFull);

            Log.info(thisClass, methodFull, "The request is: '" + request);

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
            Log.info(thisClass, methodFull, "The response received is: '" + respReceived + "'");
            //issue 24772 not using asserTrue here for the case of ws-security.return.security.error="false"
            //to avoid unnecessary failure
            //assertTrue("Failed to get back the expected text. But :" + respReceived, respReceived.contains("<p>pass:true:"));
            //assertTrue("Hmm... Strange! wrong testMethod back. But :" + respReceived, respReceived.contains(">m:" + thisMethod + "<"));
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

        Log.info(thisClass, "tearDown", "deleting usr/extension/lib/com.ibm.ws.wssecurity.example.cbh.jar");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/com.ibm.ws.wssecurity.example.cbh.jar");
        Log.info(thisClass, "tearDown", "deleting usr/extension/lib/features/wsseccbh-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/wsseccbh-1.0.mf");

    }

    //issue 24772 same method from CommonTest
    //private static void printMethodName(String strMethod) {
    //    Log.info(thisClass, strMethod, "*****************************"
    //                                   + strMethod);
    //    System.err.println("*****************************" + strMethod);
    //}

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
