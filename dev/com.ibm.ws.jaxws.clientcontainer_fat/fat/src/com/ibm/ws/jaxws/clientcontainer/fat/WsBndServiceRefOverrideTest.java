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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class WsBndServiceRefOverrideTest {
    public static final int CONN_TIMEOUT = 5;

    @Server("WebServiceRefTestServer")
    public static LibertyServer server;

    protected static String defaultEndpointAddr = null;

    private static final String helloserverwar = "helloServer";

    protected static String testClientName = "wsBndServiceRefOverride";

    @BeforeClass
    public static void setup() throws Exception {

        defaultEndpointAddr = new StringBuilder().append("http://")
                        .append(server.getHostname())
                        .append(":")
                        .append(server.getHttpDefaultPort())
                        .append("/helloServer/SimpleEchoService")
                        .toString();

        WebArchive war = ShrinkWrap.create(WebArchive.class, helloserverwar + ".war")
                        .addPackages(true, "com.ibm.samples")
                        .addPackages(true, "com.ibm.ws.jaxws.test.wsr.server/impl")
                        .addClass(com.ibm.ws.jaxws.test.wsr.server.People.class)
                        .addPackages(true, "com.ibm.ws.test.overriddenuri.server")
                        .add(new FileAsset(new File("test-applications/helloServer/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");

        ShrinkHelper.exportDropinAppToServer(server, war);

        server.startServer("WebServiceRefTest.log");
        server.waitForStringInLog("CWWKZ0001I.*helloServer");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server == null) {
            return;
        }

        if (server.isStarted()) {
            server.stopServer();
        }

    }

    /**
     * Test the wsdl location override in service-ref, and no address defined in port element.
     *
     * @throws Exception
     */
    @Test
    public void testWsdlLocationOverride() throws Exception {
        String testName = "wsBndServiceRefOverride";

        LibertyClient client = LibertyClientFactory.getLibertyClient("wsBndServiceRefOverride");

        String wsdlAddr = defaultEndpointAddr + "?wsdl";
        TestUtils.replaceLocalFileString("test-applications/wsBndServiceRefOverride/resources/META-INF/ibm-ws-bnd.xml", "#WSDL_LOCATION#", wsdlAddr);

        List<String> serverInfo = new ArrayList<String>();
        serverInfo.add(server.getHostname());
        serverInfo.add(Integer.toString(server.getHttpDefaultPort()));

        List<String> response = Arrays.asList("Hello");
        List<String> packages = Arrays.asList("com.ibm.ws.jaxws.test.wsr.client", "com.ibm.ws.test.overriddenuri.client", "com.ibm.ws.test.overriddenuri.client.servlet");
        CommonApi.createClientWithClientArgs(client, testName, testName, response, packages, testName, "wsBndServiceRefOverride", serverInfo);

        assertNotNull("FAIL: Client should report installed features: " + client.waitForStringInCopiedLog("CWWKF0034I:.*" + "client"));
        String s = response.get(0);
        assertNotNull("FAIL: Did not receive response from server: " + s, client.waitForStringInCopiedLog(s));

    }

}
