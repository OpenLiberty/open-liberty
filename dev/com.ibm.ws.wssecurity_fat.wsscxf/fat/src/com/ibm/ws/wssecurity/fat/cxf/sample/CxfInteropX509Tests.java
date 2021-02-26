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

package com.ibm.ws.wssecurity.fat.cxf.sample;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
//Added 11/2020
import org.junit.runner.RunWith;

//Added 11/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

//Added 11/2020
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 11/2020
@RunWith(FATRunner.class)
public class CxfInteropX509Tests {

    //orig from CL:
    //private static String serverName = "com.ibm.ws.wssecurity_fat.sample";
    //private static LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

    //Added 11/2020
    static final private String serverName = "com.ibm.ws.wssecurity_fat.sample";
    //Added 11/2020
    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfInteropX509Tests.class;

    private static String portNumber = "";
    private static String portNumberSecure = "";
    private static String serviceClientUrl = "";
    private static String WSSampleClientUrl = "";
    private static Boolean ibmJDK;

    static String hostName = "localhost";
    static boolean debug = false;

    /**
     * Sets up any configuration required for running the tests.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_x509.xml");
        // rename_webcontent(server);
        String thisMethod = "setup";

        //orig from CL
        //SharedTools.installCallbackHandler(server);

        //Added 11/2020
        //apps/webcontent and apps/WSSampleSeiClient are checked in the repo publish/server folder
        ShrinkHelper.defaultDropinApp(server, "WSSampleSei", "com.ibm.was.wssample.sei.echo");
        ShrinkHelper.defaultDropinApp(server, "webcontentprovider", "com.ibm.was.cxfsample.sei.echo");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");

        server.addInstalledAppForValidation("WSSampleSei");
        server.addInstalledAppForValidation("webcontentprovider");
        server.addInstalledAppForValidation("WSSampleSeiClient");
        server.addInstalledAppForValidation("webcontent");
        // start server "com.ibm.ws.wssecurity_fat.sample"
        server.startServer(); // check CWWKS0008I: The security service is ready.
        SharedTools.waitForMessageInLog(server, "CWWKS0008I");

        // get back the default http and https port number from server
        portNumber = "" + server.getHttpDefaultPort();
        portNumberSecure = "" + server.getHttpDefaultSecurePort();

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
        serviceClientUrl = "http://localhost:" + (debug ? "9085" : portNumber);
        WSSampleClientUrl = serviceClientUrl + "/WSSampleSeiClient/ClientServlet";
        Log.info(thisClass, thisMethod, "****portNumber is:" + portNumber + " **portNumberSecure is:" + portNumberSecure);

        String vendorName = System.getProperty("java.vendor");
        Log.info(thisClass, thisMethod, "JDK Vendor Name is: " + vendorName);
        ibmJDK = true;
        if (vendorName.contains("IBM")) {
            Log.info(thisClass, thisMethod, "Using an IBM JDK");
        } else {
            Log.info(thisClass, thisMethod, "Using a NON-IBM JDK - certain tests should not run!  WE WILL BE SKIPPING SOME TESTS");
            System.err.println("Using a NON-IBM JDK - certain tests should not run!");
            ibmJDK = false;
        }

        return;
    }

    @Test
    public void testEcho21Service() throws Exception {
        String thisMethod = "testEcho21Service";
        if (!ibmJDK) {
            Log.info(thisClass, thisMethod, "Using a NON-IBM JDK - this test should not run!  SKIPPING TEST");
            System.err.println("Using a NON-IBM JDK - this test should not run!");
            return;
        }

        printMethodName(thisMethod);

        try {
            testRoutine(
                        thisMethod, // String thisMethod,
                        WSSampleClientUrl,
                        serviceClientUrl + "/WSSampleSei/Echo21Service", // the serviceURL of the WebServiceProvider. This needs to be updated in the Echo wsdl files
                        "Echo21Service", // Secnerio name. For distinguish the testing scenario
                        "echo", // testing type: ping, echo, async
                        "1", // msgcount: how many times to run the test from service-client to  service-provider
                        "soap11", // options: soap11 or soap12 or else (will be added soap11 to its end)
                        "Hello21" // message: A string to be sent from service-client to service-provider **expect to be echoed back with a prefix
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    @Test
    public void testEcho22Service() throws Exception {
        String thisMethod = "testEcho22Service";
        if (!ibmJDK) {
            Log.info(thisClass, thisMethod, "Using a NON-IBM JDK - this test should not run!  SKIPPING TEST");
            System.err.println("Using a NON-IBM JDK - this test should not run!");
            return;
        }

        printMethodName(thisMethod);

        try {
            testRoutine(
                        thisMethod, // String thisMethod,
                        WSSampleClientUrl,
                        serviceClientUrl + "/WSSampleSei/Echo22Service", // the serviceURL of the WebServiceProvider. This needs to be updated in the Echo wsdl files
                        "Echo22Service", // Secnerio name. For distinguish the testing scenario
                        "echo", // testing type: ping, echo, async
                        "1", // msgcount: how many times to run the test from service-client to  service-provider
                        "soap11", // options: soap11 or soap12 or else (will be added soap11 to its end)
                        "Hello22" // message: A string to be sent from service-client to service-provider **expect to be echoed back with a prefix
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    @Test
    public void testEcho23Service() throws Exception {
        String thisMethod = "testEcho23Service";
        if (!ibmJDK) {
            Log.info(thisClass, thisMethod, "Using a NON-IBM JDK - this test should not run!  SKIPPING TEST");
            System.err.println("Using a NON-IBM JDK - this test should not run!");
            return;
        }

        printMethodName(thisMethod);

        try {
            testRoutine(
                        thisMethod, // String thisMethod,
                        WSSampleClientUrl,
                        serviceClientUrl + "/WSSampleSei/Echo23Service", // the serviceURL of the WebServiceProvider. This needs to be updated in the Echo wsdl files
                        "Echo23Service", // Secnerio name. For distinguish the testing scenario
                        "echo", // testing type: ping, echo, async
                        "1", // msgcount: how many times to run the test from service-client to  service-provider
                        "soap11", // options: soap11 or soap12 or else (will be added soap11 to its end)
                        "Hello23" // message: A string to be sent from service-client to service-provider **expect to be echoed back with a prefix
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
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            Log.info(thisClass, thisMethod, "Invoking: " + uriString + ":" + optionsString);
            request = new GetMethodWebRequest(clientUrl);

            request.setParameter("serviceURL", uriString); // serviceURL the serviceURL of the WebServiceProvider. This needs to be updated in the Echo wsdl files
            request.setParameter("scenario", scenarioString); // scenario   Secnerio name. For distinguish the testing scenario
            request.setParameter("test", testString); // test       testing type: ping, echo, async
            request.setParameter("msgcount", cntString); // msgcount   msgcount: how many times to run the test from service-client to  service-provider
            request.setParameter("options", optionsString); // options    options: soap11 or soap12 or else (will be added soap11 to its end)
            request.setParameter("message", messageString); // message    message: A string to be sent from service-client to service-provider **expect to be echoed back with a prefix

            // Invoke the client
            response = wc.getResponse(request);

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
