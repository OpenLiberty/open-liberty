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

package com.ibm.ws.wssecurity.fat.cxf.x509token;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
//Added 11/2020
import org.junit.runner.RunWith;

//Added 11/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
//Mei:
import componenttest.annotation.ExpectedFFDC;
//End
//Added 11/2020
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 11/2020
@RunWith(FATRunner.class)
public class CxfX509OverRideTests {

    //orig from CL:
    //private static String serverName = "com.ibm.ws.wssecurity_fat.x509_1";
    //private static LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

    //Added 11/2020
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509_1";
    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfX509OverRideTests.class;

    private static String portNumber = "";
    private static String portNumberSecure = "";
    private static String x509ClientUrl = "";

    static String hostName = "localhost";

    final static String badUsernameToken = "The security token could not be authenticated or authorized";
    final static String msgExpires = "The message has expired";
    final static String badHttpsToken = "HttpsToken could not be asserted";
    final static String badHttpsClientCert = "Could not send Message.";

    //2/2021
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES().forServers(serverName).removeFeature("jsp-2.2").removeFeature("jaxws-2.2").removeFeature("servlet-3.1").removeFeature("usr:wsseccbh-1.0").addFeature("jsp-2.3").addFeature("jaxws-2.3").addFeature("servlet-4.0").addFeature("usr:wsseccbh-2.0"));

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the applications
     * in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        String thisMethod = "setup";

        //orig from CL:
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
        ShrinkHelper.defaultDropinApp(server, "x509client", "com.ibm.ws.wssecurity.fat.x509client", "test.wssecfvt.basicplcy", "test.wssecfvt.basicplcy.types");
        ShrinkHelper.defaultDropinApp(server, "x509token", "basicplcy.wssecfvt.test");

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

        //2/2021 Orig:
        //x509ClientUrl = "http://localhost:" + portNumber
        //                + "/x509client/CxfX509SvcClient";

        //2/2021
        if (features.contains("jaxws-2.2")) {
            x509ClientUrl = "http://localhost:" + portNumber
                            + "/x509client/CxfX509SvcClient";
        }
        if (features.contains("jaxws-2.3")) {
            x509ClientUrl = "http://localhost:" + portNumber
                            + "/x509client/CxfX509SvcClientWss4j";
        }

        Log.info(thisClass, thisMethod, "****portNumber is(2):" + portNumber);
        Log.info(thisClass, thisMethod, "****portNumberSecure is(2):" + portNumberSecure);

        return;

    }

    //
    // The server.xml in this server did not have the appropriate settings.
    // This test asks the cxf service client to set the properties to
    // overwrite them.
    //

    @Test
    public void testCxfX509Service() throws Exception {
        String thisMethod = "testCxfX509Service";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "positive", // testMode: positive, positive-1, negative, negative-1....
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FVTVersionBAXService", //String strServiceName,
                        "UrnX509Token" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //
    //   This test that the cxf service client can set the properties to overwrite the server.xml
    //   with "negative", the cxf service won't set the x509 properties. And the test ought to throw exception
    //
    //2/2021 to test with EE7, then the corresponding exception can be expected
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    //Orig:
    //public void testCxfX50NegativeService() throws Exception {
    public void testCxfX50NegativeServiceEE7Only() throws Exception {

        String thisMethod = "testCxfX509Service";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "negative", // testMode: positive, positive-1, negative, negative-1....
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FVTVersionBAXService", //String strServiceName,
                        "UrnX509Token" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //2/2021 to test with EE8, then the corresponding exception can be expected
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC("org.apache.wss4j.common.ext.WSSecurityException") //@AV999
    public void testCxfX50NegativeServiceEE8Only() throws Exception {

        String thisMethod = "testCxfX509Service";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "negative", // testMode: positive, positive-1, negative, negative-1....
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FVTVersionBAXService", //String strServiceName,
                        "UrnX509Token" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service.
     *
     */
    protected void testRoutine(
                               String thisMethod,
                               String testMode, // Positive, positive-1, negative or negative-1... etc
                               String portNumber,
                               String portNumberSecure,
                               String strServiceName,
                               String strServicePort) throws Exception {
        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            Log.info(thisClass, thisMethod, "Invoking: " + x509ClientUrl);
            request = new GetMethodWebRequest(x509ClientUrl);

            request.setParameter("serverName", serverName);
            request.setParameter("thisMethod", thisMethod);
            request.setParameter("testMode", testMode);
            request.setParameter("httpDefaultPort", portNumber);
            request.setParameter("httpSecureDefaultPort", portNumberSecure);
            request.setParameter("serviceName", strServiceName);
            request.setParameter("servicePort", strServicePort);

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            String respReceived = response.getText();
            Log.info(thisClass, thisMethod, "'" + respReceived + "'");
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
        //orig from CL:
        //SharedTools.unInstallCallbackHandler(server);

        //2/2021
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/wsseccbh-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/wsseccbh-2.0.mf");

    }

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
