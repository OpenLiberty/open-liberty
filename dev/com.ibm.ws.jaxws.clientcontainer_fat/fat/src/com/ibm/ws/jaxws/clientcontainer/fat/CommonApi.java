/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.clientcontainer.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/*
 * Api for all common tests associated with App Client framework
 */
@RunWith(FATRunner.class)
public class CommonApi {

    private static final Class<?> c = CommonApi.class;

    public static LibertyServer server;
    protected static LibertyClient client;

    @Rule
    public static TestName name = new TestName();

    /**
     * startServer successfully starts the server & asserts that it started
     *
     * @param serverName name of the server
     */
    public static void startServer(String serverName) {
        try {
            server = LibertyServerFactory.getLibertyServer(serverName);

            if (!server.isStarted() && serverName != "" && serverName != null) {
                server.startServer();
                assertNotNull("FAIL: Did not receive smarter planet message:CWWKF0011I", server.waitForStringInLog("CWWKF0011I"));
            }
        } catch (Exception e) {
            Log.info(c, "startServer", "Server is not starting: " + e.getStackTrace());
        }
    }

    /**
     * stopServer successfully stops the server & asserts that it stopped
     *
     * @param serverName name of the server
     */
    public static void stopServer(String serverName) throws Exception {
        if (server.isStarted() && serverName != "" && serverName != null) {
            Log.info(c, "stopServer", "Server was not stopped at the end of the test suite, stopping: " + server.getServerName());
            server.stopServer();
        }
    }

    /**
     * commonTestClientSetup starts the client and makes sure it's started properly
     *
     * @param clientName   name of the client that should start
     * @param ear          EJB archieve already built to be passed on in order to be deployed
     * @param appClientMsg msg that app emits when started successfully
     * @param appName      name of the app to be tested
     * @param response     response expected from JAX-WS to be checked
     * @throws Exception
     */
    public static void commonTestClientSetup(String clientName, EnterpriseArchive ear, String appClientMsg, String appName, String response) throws Exception {

        final String methodName = "commonTestClientSetup";
        Log.entering(c, methodName, "Starting clients & checking console messages");

        Log.info(c, methodName, "Name of client:" + clientName);

        if (appName != null) {
            client = LibertyClientFactory.getLibertyClient(clientName);
        }

        ShrinkHelper.exportAppToClient(client, ear);

        client.copyFileToLibertyClientRoot("../../publish/clients/" + appName + "/client.xml");
        client.copyFileToLibertyClientRoot("../../publish/clients/" + appName + "/bootstrap.properties");

        client.startClient();

        Log.info(c, methodName, "Client should report installed features: " + client.waitForStringInCopiedLog("CWWKF0034I:.*" + "client"));
        assertNotNull("FAIL: Client should report installed features: " + client.waitForStringInCopiedLog("CWWKF0034I:.*" + "client"));
        assertNotNull("FAIL: Did not receive response from server", client.waitForStringInCopiedLog(response));
        assertNotNull("FAIL: Did not receive application started message:CWWKZ0001I", client.waitForStringInCopiedLog("CWWKZ0001I:.*" + appName));
        Log.exiting(c, methodName);
    }

    /**
     * verifyClientStop makes sure that the client stop message "CWWKE0908I" is successfully stopped
     *
     * @param testClientName name of the client to who should stop
     * @throws Exception
     */

    public static void verifyClientStop(String testClientName) throws Exception {

        if (client.isStarted()) {
            assertNotNull("FAIL: Did not receive client stopped message: CWWKE0908I", client.waitForStringInCopiedLog("CWWKE0908I:.*" + testClientName));
        }
        Log.info(c, "verifyClientStop", "Client was not stopped at the end of the test suite:");
    }

    /**
     * verifyAppInstall confirms the console msg "CWWKT0016I: Web application available (default_host): http://test.canlab.ibm.com:9080/webAppName/"
     * and console msg "CWWKZ0001I: Application webAppName started in 0.555 seconds" with its appName
     *
     * @param appName    name of the app associated with CWWKT0016I
     * @param webAppName name of webapp associated with CWWKZ0001I
     * @param extension  extension of the app to strip
     */
    public static void verifyAppInstall(String appName, String webAppName, String extension) throws Exception {

        if (server.isStarted()) {
            assertNotNull("FAIL: Did not receive Web application available message:CWWKT0016I", server.waitForStringInLog("CWWKT0016I:.*" + webAppName));
            assertNotNull("FAIL: Did not receive application started message:CWWKZ0001I", server.waitForStringInLog("CWWKZ0001I:.*" + appName.split(extension)[0]));
        }
    }

    /**
     * commonTestClientSetupWithargs starts the client and makes sure its started properly
     * with arguments
     *
     * @param ear          EJB archieve already built to be passed on in order to be deployed
     * @param appClientMsg msg that app emits when started successfully
     * @param appName      name of the app to be tested
     * @param response     list of responses expected from JAX-WS to be checked
     * @param args         list of arguments to be passed when starting the server
     * @throws Exception
     */
    public static void commonTestClientSetupWithargs(EnterpriseArchive ear, String appClientMsg, String appName, List<String> response,
                                                     List<String> args) throws Exception {

        final String methodName = "commonTestClientSetupWithargs";
        Log.entering(c, methodName, "Starting clients & checking console messages");

        Log.info(c, methodName, "Name of client:" + appName);

        if (appName != null) {
            client = LibertyClientFactory.getLibertyClient(appName);
        }

        commonTestClientSetupWithClientargs(client, ear, appClientMsg, appName, response, args);

        Log.exiting(c, methodName);
    }

    /**
     * commonTestClientSetupWithargs starts the client and makes sure its started properly
     * with arguments having LibertyClient as parameter in order to allow same client to be
     * accessible from calling method
     *
     * @param client       LibertyClient added in order to share same client instance between calling method and this one
     * @param ear          EJB archieve already built to be passed on in order to be deployed
     * @param appClientMsg msg that app emits when started successfully
     * @param appName      name of the app to be tested
     * @param response     list of responses expected from JAX-WS to be checked
     * @param args         list of arguments to be passed when starting the server
     * @throws Exception
     */
    public static void commonTestClientSetupWithClientargs(LibertyClient client, EnterpriseArchive ear, String appClientMsg, String appName, List<String> response,
                                                           List<String> args) throws Exception {
        final String methodName = "commonTestClientSetupWithClientargs";

        ShrinkHelper.exportAppToClient(client, ear);

        client.copyFileToLibertyClientRoot("../../publish/clients/" + appName + "/client.xml");
        client.copyFileToLibertyClientRoot("../../publish/clients/" + appName + "/bootstrap.properties");

        args.add(0, "--");
        client.startClientWithArgs(true, true, true, false, "run", args, true);

        Log.info(c, methodName, "Client should report installed features: " + client.waitForStringInCopiedLog("CWWKF0034I:.*" + "client"));
        assertNotNull("FAIL: Client should report installed features: " + client.waitForStringInCopiedLog("CWWKF0034I:.*" + "client"));
        for (String s : response) {
            assertNotNull("FAIL: Did not receive response from server: " + s, client.waitForStringInCopiedLog(s));
        }

        assertNotNull("FAIL: Did not receive application started message:CWWKZ0001I", client.waitForStringInCopiedLog("CWWKZ0001I:.*" + appName));
        Log.exiting(c, methodName);
    }

    /**
     * createClientWithClientArgs creates EJB archieve, setup client and start the client using the giving LibertyClient
     *
     * @param client          LibertyClient added in order to share same client instance between calling method and this one
     * @param appName         name of the app to be tested
     * @param appClientMsg    msg that app emits when started successfully
     * @param responses       list of responses expected from JAX-WS to be checked
     * @param packageNames    list of package names to be included in the jar located in ear
     * @param appXMLPath      location of application.xml of ear in fat bucket build folder
     * @param appResourcePath location of resources needed for the jar file containing the test and dependent classes
     * @param ServerInfo      server host and port info for the client to connect
     * @throws Exception
     */
    public static void createClientWithClientArgs(LibertyClient client, String appName, String appClientMsg, List<String> responses, List<String> packageNames, String appXMLPath,
                                                  String appResourcePath, List<String> ServerInfo) throws Exception {
        EnterpriseArchive ear = createEJBArchieve(appName, appName, packageNames, appXMLPath, appResourcePath);

        try {
            Log.info(c, name.getMethodName(), "Starting the client ...");

            commonTestClientSetupWithClientargs(client, ear, appClientMsg, appName, responses, ServerInfo);
        } catch (Exception e) {
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * createClientWithArgs creates EJB archieve, setup client and start the client
     *
     * @param appName         name of the app to be tested
     * @param appClientMsg    msg that app emits when started successfully
     * @param responses       list of responses expected from JAX-WS to be checked
     * @param packageNames    list of package names to be included in the jar located in ear
     * @param appXMLPath      location of application.xml of ear in fat bucket build folder
     * @param appResourcePath location of resources needed for the jar file containing the test and dependent classes
     * @param ServerInfo      server host and port info for the client to connect
     * @throws Exception
     */
    public static void createClientWithArgs(String appName, String appClientMsg, List<String> responses, List<String> packageNames, String appXMLPath, String appResourcePath,
                                            List<String> ServerInfo) throws Exception {
        EnterpriseArchive ear = createEJBArchieve(appName, appName, packageNames, appXMLPath, appResourcePath);

        try {
            Log.info(c, name.getMethodName(), "Starting the client ...");

            commonTestClientSetupWithargs(ear, appClientMsg, appName, responses, ServerInfo);
        } catch (Exception e) {
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * createClient creates EJB archieve, setup client and start the client
     *
     * @param clientName      clientName that used to create LibertyClient
     * @param appName         name of the app to be tested
     * @param jarName         name of the jar file in case it's different than appName
     * @param appClientMsg    msg that app emits when started successfully
     * @param response        response expected from JAX-WS to be checked
     * @param packageNames    list of package names to be included in the jar located in ear
     * @param appXMLPath      location of application.xml of ear in fat bucket build folder
     * @param appResourcePath location of resources needed for the jar file containing the test and dependent classes
     * @throws Exception
     */
    public static void createClient(String clientName, String appName, String jarName, String appClientMsg, String response, List<String> packageNames, String appXMLPath,
                                    String appResourcePath) throws Exception {
        EnterpriseArchive ear = createEJBArchieve(appName, jarName, packageNames, appXMLPath, appResourcePath);

        try {
            Log.info(c, name.getMethodName(), "Starting the client ...");
            commonTestClientSetup(clientName, ear, appClientMsg, appName, response);
        } catch (Exception e) {
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * createEJBArchieve creates EJB archieve
     *
     * @param appName         name of the app to be tested
     * @param jarName         name of the jar file in case it's different than appName
     * @param packageNames    list of package names to be included in the jar located in ear
     * @param appXMLPath      location of application.xml of ear in fat bucket build folder
     * @param appResourcePath location of resources needed for the jar file containing the test and dependent classes
     * @throws Exception
     */
    private static EnterpriseArchive createEJBArchieve(String appName, String jarName, List<String> packageNames, String appXMLPath, String appResourcePath) throws Exception {
        JavaArchive jar = ShrinkHelper.buildJavaArchive(jarName + ".jar");
        for (String packageName : packageNames) {
            jar.addPackage(packageName);
        }

        ShrinkHelper.addDirectory(jar, "test-applications/" + appResourcePath + "/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear").addAsModule(jar);
        ear.addAsManifestResource(new File("lib/LibertyFATTestFiles/Ear/" + appXMLPath + "/META-INF/application.xml"));

        return ear;
    }
}
