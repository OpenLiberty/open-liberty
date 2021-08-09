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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class AppClientTest {

    private static final String helloserverwar = "helloServer";
    private static final Class<?> c = AppClientTest.class;

    protected static LibertyClient client;
    @Server("WebServiceRefTestServer")
    public static LibertyServer server;
    private static String BASE_URL;

    static List<String> serverInfo = null;

    public AppClientTest() {
        // Set server info once
        serverInfo = new ArrayList<String>();
        serverInfo.add(server.getHostname());
        serverInfo.add(Integer.toString(server.getHttpDefaultPort()));
    }

    @BeforeClass
    public static void setUp() throws Exception {
        BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
        System.out.println("BASE_URL:" + BASE_URL);
        WebArchive war = ShrinkWrap.create(WebArchive.class, helloserverwar + ".war")
                        .addPackages(true, "com.ibm.samples")
                        .addPackages(true, "com.ibm.ws.jaxws.test.wsr.server/impl")
                        .addClass(com.ibm.ws.jaxws.test.wsr.server.People.class)
                        .addPackages(true, "com.ibm.ws.test.overriddenuri.server")
                        .add(new FileAsset(new File("test-applications/helloServer/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");

        ShrinkHelper.exportDropinAppToServer(server, war);

        String thisMethod = "setUp";
        server.startServer("WebServiceRefTest.log");
        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*helloServer");
        Log.info(c, thisMethod, "setup complete ...");
    }

    /**
     * Testing individual clients specified by their name and the msg that gets emitted
     * once the client starts successfully
     *
     * @throws Exception
     */
    @Test
    public void testPortTypeInjectionNormal() throws Exception {
        String testName = "PortTypeInjectionNormal";
        List<String> response = Arrays.asList("Hello Response from PortTypeInjectionNormal",
                                              "Hello received result from unmanged dynamic proxy client",
                                              "received body content from unmanged dispath client:Hello Hello");
        List<String> packages = Arrays.asList("com.ibm.ws.jaxws.test.wsr.client", "com.ibm.ws.jaxws.test.wsr.server.stub");
        CommonApi.createClientWithArgs(testName, testName, response, packages, testName, "helloClient/src/" + testName, getServerInfo());
    }

    @Test
    public void testServiceInjectionClassLevel() throws Exception {
        String testName = "ServiceInjectionClassLevel";
        List<String> response = Arrays.asList("Hello Response from ServiceInjectionClassLevel");
        List<String> packages = Arrays.asList("com.ibm.ws.jaxws.test.wsr.client", "com.ibm.ws.jaxws.test.wsr.server.stub");
        CommonApi.createClientWithArgs(testName, testName, response, packages, testName, "helloClient/src/" + testName, getServerInfo());
    }

    @Test
    public void testServiceInjectionMultiTargets() throws Exception {
        String testName = "ServiceInjectionMultiTargets";
        List<String> response = Arrays.asList("Hello Response from ServiceInjectionMultiTargets");
        List<String> packages = Arrays.asList("com.ibm.ws.jaxws.test.wsr.client", "com.ibm.ws.jaxws.test.wsr.server.stub");
        CommonApi.createClientWithArgs(testName, testName, response, packages, testName, "helloClient/src/" + testName, getServerInfo());
    }

    @Test
    public void testPortTypeInjectionClassLevel() throws Exception {
        String testName = "PortTypeInjectionClassLevel";
        List<String> response = Arrays.asList("Hello Response from PortTypeInjectionClassLevel");
        List<String> packages = Arrays.asList("com.ibm.ws.jaxws.test.wsr.client", "com.ibm.ws.jaxws.test.wsr.server.stub", "com.ibm.ws.jaxws.test.wsr.clientddmerge");
        CommonApi.createClientWithArgs(testName, testName, response, packages, testName, "helloClient/src/" + testName, getServerInfo());
    }

    @Test
    public void testServiceInjectionMemberLevel() throws Exception {
        String testName = "ServiceInjectionMemberLevel";
        List<String> response = Arrays.asList("Hello Response from ServiceInjectionMemberLevel");
        List<String> packages = Arrays.asList("com.ibm.ws.jaxws.test.wsr.client", "com.ibm.ws.jaxws.test.wsr.server.stub",
                                              "com.ibm.ws.jaxws.test.wsr.clientddmerge");
        CommonApi.createClientWithArgs(testName, testName, response, packages, testName, "helloClient/src/" + testName, getServerInfo());
    }

    @Test
    public void testResourseServiceInjectionMemberLevel() throws Exception {
        String testName = "ResourceServiceInjectionMemberLevel";
        List<String> response = Arrays.asList("Hello Response from ServiceInjectionMemberLevel");
        List<String> packages = Arrays.asList("com.ibm.ws.jaxws.test.wsr.client", "com.ibm.ws.jaxws.test.wsr.server.stub",
                                              "com.ibm.ws.jaxws.test.wsr.clientddmerge", "com.ibm.ws.jaxws.test.wsr.clientserviceresource");
        CommonApi.createClientWithArgs(testName, testName, response, packages, testName, "helloClient/src/" + testName, getServerInfo());
    }

    @Test
    public void testClientHandler() throws Exception {
        String testName = "ClientHandler";
        List<String> response = new ArrayList<String>();
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestLogicalHandler: handle outbound message");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestSOAPHandler: handle outbound message");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestSOAPHandler: handle inbound message");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestLogicalHandler: handle inbound message");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestSOAPHandler is closed");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestLogicalHandler is closed");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestLogicalHandler: handle outbound message");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestSOAPHandler: handle outbound message");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestSOAPHandler: handle inbound message");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestLogicalHandler: handle inbound message");

        // Test initParams

        response.add("init param \"soapArg0\" = testServiceInitParamInSoapFromWSRef");
        response.add("init param \"arg0\" = testServiceInitParamFromWSRef");

        response.add("init param \"soapArg0\" = testPortInitParamInSoapFromRes");
        response.add("init param \"arg0\" = testPortInitParamFromRes");

        // Test postConstruct and preDestroy
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestSOAPHandler: postConstruct is invoked");
        response.add("com.ibm.ws.jaxws.test.wsr.clienthandler.TestLogicalHandler: postConstruct is invoked");
        //PreDestroy for the handlerChain on the client side is not supported, as it is not a managed environment
        // response.add("com.ibm.samples.jaxws.client.handler.TestSOAPHandler: PreDestroy is invoked");
        // response.add("com.ibm.samples.jaxws.client.handler.TestLogicalHandler: PreDestroy is invoked");
        List<String> packages = Arrays.asList("com.ibm.ws.jaxws.test.wsr.client", "com.ibm.ws.jaxws.test.wsr.server.stub", "com.ibm.ws.jaxws.test.wsr.clienthandler");
        CommonApi.createClientWithArgs(testName, testName, response, packages, testName, "helloClient/src/" + testName, getServerInfo());
    }

    private List<String> getServerInfo() {
        return serverInfo;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            Log.info(c, "tearDown", "Server was not stopped at the end of the test suite, stopping: " + server.getServerName());
            server.stopServer();
        }
    }
}
