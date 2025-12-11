/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.annotation.MinimumJavaLevel;


@MinimumJavaLevel(javaLevel = 17)
@SkipForRepeat({ EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfX509MigSymSha2NegativeTests {

    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509migsym";
    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfX509MigSymSha2NegativeTests.class;

    private static String errMsgVersion = "";

    static boolean debugOnHttp = true;

    private static String portNumber = "";
    private static String portNumberSecure = "";
    private static String x509MigSymClientUrl = "";

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
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badsha2.xml");
        } else if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_badsha2_wss4j.xml");
        }
        //errMsgVersion = "wss4j";

        ShrinkHelper.defaultDropinApp(server, "x509migclient", "com.ibm.ws.wssecurity.fat.x509migclient", "test.libertyfat.x509mig.contract", "test.libertyfat.x509mig.types");
        ShrinkHelper.defaultDropinApp(server, "x509migbadclient", "com.ibm.ws.wssecurity.fat.x509migbadclient", "test.libertyfat.x509mig.contract",
                                      "test.libertyfat.x509mig.types");
        ShrinkHelper.defaultDropinApp(server, "x509migtoken", "basicplcy.wssecfvt.test");

        server.startServer();// check CWWKS0008I: The security service is ready.
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
     * TestDescription:
     * This tested in a wrong Server Setup
     * The webservice client is set to default signatureAlgorithem which is sha1
     * and the webservice provider is set to sha512
     *
     */

    @Test
    //issue 23060
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID, RepeatWithEE7cbh20.ID })
    public void testCxfX509KeyIdMigSymServiceSha1ToSha512() throws Exception {

        String thisMethod = "testCxfX509KeyIdMigSymService";
        methodFull = "testCxfX509KeyIdMigSymServiceSha1ToSha512";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509KeyIdentifierPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX01Service", //String strServiceName,
                        "UrnX509Token01" //String strServicePort
            //errMsgVersion //CxfX509MigSvcClient
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This tested in a wrong Server Setup
     * The webservice client is set to default signatureAlgorithem which is sha1
     * and the webservice provider is set to sha512
     *
     */

    @Test
    //issue 23060
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID, RepeatWithEE7cbh20.ID })
    public void testCxfX509KeyIdMigSymServiceHttpsSha1ToSha512() throws Exception {

        String thisMethod = "testCxfX509KeyIdMigSymService";
        methodFull = "testCxfX509KeyIdMigSymServiceHttpsSh1ToSha512";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509KeyIdentifierPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX01Service", //String strServiceName,
                        "UrnX509Token01" //String strServicePort
            //errMsgVersion //CxfX509MigSvcClient
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This tested in a wrong Server Setup
     * The webservice client is set to default signatureAlgorithem which is sha1
     * and the webservice provider is set to sha512
     *
     */

    @Test
    //issue 23060
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID, RepeatWithEE7cbh20.ID })
    public void testCxfX509IssuerSerialMigSymServiceSha1ToSha512() throws Exception {

        String thisMethod = "testCxfX509IssuerSerialMigSymService";
        methodFull = "testCxfX509IssuerSerialMigSymServiceSha1ToSha512";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509IssuerSerialPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX03Service", //String strServiceName,
                        "UrnX509Token03" //String strServicePort
            //errMsgVersion //CxfX509MigSvcClient
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
        //null);

        return;
    }

/*
 * protected void testRoutine(
 * String thisMethod,
 * String x509Policy,
 * String testMode, // Positive, positive-1, negative or negative-1... etc
 * String portNumber,
 * String portNumberSecure,
 * String strServiceName,
 * String strServicePort,
 * String errMsgVersion) throws Exception { //2/2021
 * testSubRoutine(
 * thisMethod,
 * x509Policy,
 * testMode, // Positive, positive-1, negative or negative-1... etc
 * portNumber,
 * portNumberSecure,
 * strServiceName,
 * strServicePort,
 * x509MigSymClientUrl,
 * "",
 * errMsgVersion);
 * 
 * return;
 * }
 */
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
        //String errMsgVersion) throws Exception {
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

            //request.setParameter("errorMsgVersion", errMsgVersion);

            Log.info(thisClass, methodFull, "The request is: " + request);

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
            assertTrue("Failed to get back the expected text. But : " + respReceived, respReceived.contains("<p>pass:true:"));
            assertTrue("Hmm... Strange! wrong testMethod back. But : " + respReceived, respReceived.contains(">m:" + thisMethod + "<"));
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
