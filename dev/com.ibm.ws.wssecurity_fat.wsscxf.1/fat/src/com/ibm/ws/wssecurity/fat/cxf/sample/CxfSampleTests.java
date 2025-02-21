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

package com.ibm.ws.wssecurity.fat.cxf.sample;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLEndpoint;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.rules.repeater.CheckpointRule.ServerMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//@SkipForRepeat({ NO_MODIFICATION, EE8_FEATURES })
@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
public class CxfSampleTests {

    static final private String serverName = "com.ibm.ws.wssecurity_fat.sample";

    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfSampleTests.class;

    private static String portNumber = "";
    private static String portNumberSecure = "";
    private static String serviceClientUrl = "";
    private static String securedServiceClientUrl = "";
    private static String WSSampleClientUrl = "";
    private static String sampleClientUrl = "";

    static String hostName = "localhost";

    @ClassRule
    public static CheckpointRule checkpointRule = new CheckpointRule()
                                                        .setConsoleLogName(CxfSampleTests.class.getSimpleName()+ ".log")
                                                        .setServerSetup(CxfSampleTests::serverSetUp)
                                                        .setServerStart(CxfSampleTests::serverStart)
                                                        .setServerTearDown(CxfSampleTests::serverTearDown)
                                                        .addUnsupportedRepeatIDs(EmptyAction.ID, RepeatWithEE7cbh20.ID)
                                                        .addCheckpointRegexIgnoreMessages("CWWKG0101W", "SRVE0274W")
                                                        .setPostCheckpointLambda(server -> {
                                                            try {
                                                                configureBeforeRestore();
                                                            } catch (Exception e) {
                                                                // TODO Auto-generated catch block
                                                                e.printStackTrace();
                                                            }
                                                        });

    public static LibertyServer serverSetUp(ServerMode mode) throws Exception {
        //issue 23060
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-1.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_asym.xml");
        }
        if (features.contains("usr:wsseccbh-2.0")) {
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
            copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_asym_wss4j.xml");
        }

        //apps/webcontent and apps/WSSampleSeiClient are checked in the repo publish/server folder
        ShrinkHelper.defaultDropinApp(server, "WSSampleSei", "com.ibm.was.wssample.sei.echo");
        ShrinkHelper.defaultDropinApp(server, "webcontentprovider", "com.ibm.was.cxfsample.sei.echo");

        //JakartaEE9 transforms the sample client applications which exist at '<server>/apps/<appname>'.
        if (JakartaEEAction.isEE9OrLaterActive()) {
            Path webcontent_archive = Paths.get(server.getServerRoot() + File.separatorChar + "apps" + File.separatorChar + "webcontent");
            Path WSSampleSeiClient_archive = Paths.get(server.getServerRoot() + File.separatorChar + "apps" + File.separatorChar + "WSSampleSeiClient");
            JakartaEEAction.transformApp(webcontent_archive);
            JakartaEEAction.transformApp(WSSampleSeiClient_archive);
        }
        if (JakartaEEAction.isEE10OrLaterActive()) {
            Path WSSampleSei_archive = Paths.get(server.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "WSSampleSei.war");
            JakartaEEAction.transformApp(WSSampleSei_archive);
        }

        //Environment variable values are not set before checkpoint.
        if (CheckpointRule.isActive()) {
            configureEnvVariable(server, emptyMap());
        }
        return server;
    }

    private static void configureBeforeRestore() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("WS_SECURITY_PWD", "security");
        config.put("CLIENT_SIGNATURE_PWD", "LibertyX509Client");
        config.put("CLIENT_SIGNATURE_KEYSTORE", "x509ClientDefault.jks");
        config.put("PROVIDER_ENCRYPTION_KEYSTORE", "x509ServerDefault.jks");
        config.put("PROVIDER_ENCRYPTION_PWD", "LibertyX509Server");
        configureEnvVariable(server, config);
    }

    private static void configureEnvVariable(LibertyServer server, Map<String, String> newEnv) throws Exception {
        Properties serverEnvProperties = new Properties();
        serverEnvProperties.putAll(newEnv);
        File serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        try (OutputStream out = new FileOutputStream(serverEnvFile)) {
            serverEnvProperties.store(out, "");
        }
    }

    public static void serverStart(ServerMode mode, LibertyServer server) throws Exception {
        String thisMethod = "serverStart";
        String defaultPort = "8010";
        // get the default http and https port number from server
        portNumber = "" + server.getHttpDefaultPort();
        portNumberSecure = "" + server.getHttpDefaultSecurePort();

        // start server "com.ibm.ws.wssecurity_fat.sample"
        if (!CheckpointRule.isActive()) {
            server.addInstalledAppForValidation("WSSampleSei");
            server.addInstalledAppForValidation("webcontentprovider");
            server.addInstalledAppForValidation("WSSampleSeiClient");
            server.addInstalledAppForValidation("webcontent");
        }

        //LibertyX509Client

        server.startServer(); // check CWWKS0008I: The security service is ready.
        SharedTools.waitForMessageInLog(server, "CWWKS0008I");

        // Make sure the server is starting by checking the port number in server runtime logs
        server.waitForStringInLog("port " + portNumber);
        // server.waitForStringInLog("port " + portNumberSecure);

        // check  message.log
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now lis....Port 8010
        assertNotNull("defaultHttpendpoint may not started at :" + portNumber,
                      server.waitForStringInLog("CWWKO0219I.*" + portNumber));
        //// CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now lis....Port 8020
        //assertNotNull("defaultHttpEndpoint SSL port may not be started at:" + portNumberSecure,
        //              server.waitForStringInLog("CWWKO0219I.*" + portNumberSecure));

        // using the original port to send the parameters to CxfClientServlet in service client (webcontent)
        serviceClientUrl = "http://localhost:" + portNumber;
        securedServiceClientUrl = "https://localhost:" + portNumberSecure;
        sampleClientUrl = serviceClientUrl + "/webcontent/CxfClientServlet";
        WSSampleClientUrl = serviceClientUrl + "/WSSampleSeiClient/ClientServlet";
        Log.info(thisClass, thisMethod, "****portNumber is:" + portNumber + " **portNumberSecure is:" + portNumberSecure);

        // debug: set to tcpmon port 9085
        //String wsProviderPort = "9085";
        String wsProviderPort = portNumber;
        if (!wsProviderPort.equals(defaultPort)) {
            // handle the endpoints in the Echo wsdl files in the case of interop
            UpdateWSDLEndpoint updateWsdlEndpoint = new UpdateWSDLEndpoint(server);

            String strServerConfigDir = updateWsdlEndpoint.getServerConfigDir();
            Log.info(thisClass, thisMethod,
                     "****update " + strServerConfigDir +
                                            "/apps/webcontent/Echo.wsdl" + ":replace '" +
                                            defaultPort + "' to '" + wsProviderPort + "'");

            Log.info(thisClass, thisMethod, "before updateWsdlEndpoint in CxfSampleTests setup()");
            updateWsdlEndpoint.updateEndpoint("/apps/webcontent/Echo.wsdl",
                                              defaultPort,
                                              wsProviderPort);
            Log.info(thisClass, thisMethod, "after updateWsdlEndpoint in CxfSampleTests setup()");

        }

        return;
    }

    public static void serverTearDown(ServerMode mode, LibertyServer server) throws Exception {
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

    @Test
    public void testEchoServiceCXF() throws Exception {

        String thisMethod = "testEchoServiceCXF";

        printMethodName(thisMethod);

        try {
            testRoutine(
                        thisMethod, // String thisMethod,
                        sampleClientUrl,
                        "serviceURL", // the serviceURL of the WebServiceProvider. This needs to be updated in the Echo wsdl files
                        "EchoService", // Scenario name. For distinguish the testing scenario
                        "echo", // testing type: ping, echo, async
                        "1", // msgcount: how many times to run the test from service-client to  service-provider
                        "soap11", // options: soap11 or soap12 or else (will be added soap11 to its end)
                        "Hello" // message: A string to be sent from service-client to service-provider **expect to be echoed back with a prefix
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    @Test
    public void testEchoService() throws Exception {
        String thisMethod = "testEchoService";

        printMethodName(thisMethod);

        try {
            testRoutine(
                        thisMethod, // String thisMethod,
                        WSSampleClientUrl,
                        securedServiceClientUrl + "/WSSampleSei/EchoService", // the serviceURL of the WebServiceProvider. This needs to be updated in the Echo wsdl files
                        "EchoService", // Scenario name. For distinguish the testing scenario
                        "echo", // testing type: ping, echo, async
                        "1", // msgcount: how many times to run the test from service-client to  service-provider
                        "soap11", // options: soap11 or soap12 or else (will be added soap11 to its end)
                        "Hello" // message: A string to be sent from service-client to service-provider **expect to be echoed back with a prefix
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    @Test
    public void testEcho4Service() throws Exception {
        String thisMethod = "testEcho4Service";

        printMethodName(thisMethod);

        try {
            testRoutine(
                        thisMethod, // String thisMethod,
                        WSSampleClientUrl,
                        securedServiceClientUrl + "/WSSampleSei/Echo4Service", // the serviceURL of the WebServiceProvider. This needs to be updated in the Echo wsdl files
                        "Echo4Service", // Secnerio name. For distinguish the testing scenario
                        "echo", // testing type: ping, echo, async
                        "1", // msgcount: how many times to run the test from service-client to  service-provider
                        "soap11", // options: soap11 or soap12 or else (will be added soap11 to its end)
                        "Hello" // message: A string to be sent from service-client to service-provider **expect to be echoed back with a prefix
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
                               String thisMethod, // thisMethod testing Method
                               String clientUrl, // The serviceClient URL
                               String uriString, // serviceURL the serviceURL of the WebServiceProvider. This needs to be updated in the Echo wsdl files
                               String scenarioString, // scenario   Secnerio name. For distinguish the testing scenario
                               String testString, // test       testing type: ping, echo, async
                               String cntString, // msgcount   msgcount: how many times to run the test from service-client to  service-provider
                               String optionsString, // options    options: soap11 or soap12 or else (will be added soap11 to its end)
                               String messageString // message    message: A string to be sent from service-client to service-provider **expect to be echoed back with a prefix
    ) throws Exception {
        try {

            if (scenarioString == null || scenarioString.isEmpty()) {
                scenarioString = thisMethod;
            }

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us

            Log.info(thisClass, thisMethod, "before new conversation in testRoutine");
            WebConversation wc = new WebConversation();
            Log.info(thisClass, thisMethod, "after new conversation in testRoutine");

            // Invoke the service client - servlet
            Log.info(thisClass, thisMethod, "before getmethodwebrequest in testRoutine");
            Log.info(thisClass, thisMethod, "Invoking: " + uriString + ":" + optionsString);
            request = new GetMethodWebRequest(clientUrl);
            Log.info(thisClass, thisMethod, "after getmethodwebrequest in testRoutine");

            request.setParameter("serviceURL", uriString); // serviceURL the serviceURL of the WebServiceProvider. This needs to be updated in the Echo wsdl files
            request.setParameter("scenario", scenarioString); // scenario   Scenario name. For distinguish the testing scenario
            request.setParameter("test", testString); // test       testing type: ping, echo, async
            request.setParameter("msgcount", cntString); // msgcount   msgcount: how many times to run the test from service-client to  service-provider
            request.setParameter("options", optionsString); // options    options: soap11 or soap12 or else (will be added soap11 to its end)
            request.setParameter("message", messageString); // message    message: A string to be sent from service-client to service-provider **expect to be echoed back with a prefix

            // Invoke the client
            Log.info(thisClass, thisMethod, "before getresponse in testRoutine");
            response = wc.getResponse(request);
            Log.info(thisClass, thisMethod, "after getresponse in testRoutine");

            // Read the response page from client jsp
            String respReceived = response.getText();
            String methodFull = thisMethod;

            // Log.info(thisClass, methodFull, "\"" + respReceived + "\"");

            String strStatus = getAttribute(respReceived, "status");
            String strScenario = getAttribute(respReceived, "scenario");
            String strTest = getAttribute(respReceived, "test");
            String strTime = getAttribute(respReceived, "time"); // not implemented
            String strServiceURL = getAttribute(respReceived, "serviceURL"); // not implemented
            String strOptions = getAttribute(respReceived, "options");
            String strDetail = getAttribute(respReceived, "detail");

            Log.info(thisClass, methodFull,
                     "\n status:\"" + strStatus +
                                            "\" scenario:\"" + strScenario +
                                            "\" test:\"" + strTest +
                                            "\" time:\"" + strTime +
                                            "\" serviceURL:\"" + strServiceURL +
                                            "\" options:\"" + strOptions +
                                            "\" detail:\"" + strDetail +
                                            "\"");

            assertTrue("The servlet indicated test failed. See results:" + respReceived, strStatus.equals("pass"));
            assertTrue("Failed to get back the \"" + messageString + "\" text. See results:" + respReceived, strDetail.contains(messageString));
        } catch (Exception e) {
            Log.info(thisClass, thisMethod, "Exception occurred:", e);
            System.err.println("Exception: " + e);
            throw e;
        }

        return;
    }

    private static void printMethodName(String strMethod) {
        Log.info(thisClass, strMethod, "*****************************"
                                       + strMethod);
        System.err.println("*****************************" + strMethod);
    }

    private String getAttribute(String result, String strAttr) {
        String strReturn = "";
        if (result != null) {
            if (strAttr.equals("detail")) {
                int beginLessThan = result.indexOf(">");
                int tmpLessThan = beginLessThan;
                int endLessThan = beginLessThan;
                while (endLessThan >= 0) {
                    tmpLessThan = endLessThan;
                    endLessThan = result.indexOf(">", endLessThan + 1);
                    if (endLessThan >= 0) {
                        beginLessThan = tmpLessThan;
                    }
                }
                if (beginLessThan >= 0) {
                    beginLessThan++;
                    int indexGreatThan = result.indexOf("</", beginLessThan);
                    strReturn = result.substring(beginLessThan, indexGreatThan);
                }

            } else {
                String strIndex = strAttr.concat("='");
                int index = result.indexOf(strIndex);
                if (index >= 0) {
                    int indexBegins = index + strIndex.length(); //  the begin of returning value
                    int indexEnds = result.indexOf("'", indexBegins);
                    strReturn = result.substring(indexBegins, indexEnds);
                }
            }
        }
        return restoreTags(strReturn);
    }

    /**
     * Restore HTML tags out of input string
     *
     * @param input String that was returnd from servlet
     *
     * @return String restore HTML Tags
     */
    private String restoreTags(String input) {
        return (input.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'"));
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

    /**
     * The WebContent ought not to be in Personal Build environment
     * Even it's there, it will not be in use....
     **/
    public static void rename_webcontent(LibertyServer server) throws Exception {
        try {
            String serverFileLoc = (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();
            String strWebContent = serverFileLoc + "/apps/WebContent";
            String strwebcontent = serverFileLoc + "/apps/webcontent";
            File fileWC = new File(strWebContent);
            if (fileWC.exists()) { // The app name had been changed from WebContent to webcontent
                File filewc = new File(strwebcontent);
                fileWC.renameTo(filewc);
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }
}
