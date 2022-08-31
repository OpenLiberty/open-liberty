/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * ApplicationProcessor tests which require the servlet feature
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ApplicationProcessorServletTest {

    private static final Class<?> c = ApplicationProcessorServletTest.class;

    private static final String SERVER_NAME = "ApplicationProcessorServletServer";

    private static final String APP_NAME_3 = "simpleServlet";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
        MicroProfileActions.MP60, // mpOpenAPI-3.1, LITE
        MicroProfileActions.MP50, // mpOpenAPI-3.0, FULL
        MicroProfileActions.MP41, // mpOpenAPI-2.0, FULL
        MicroProfileActions.MP33, // mpOpenAPI-1.1, FULL
        MicroProfileActions.MP22);// mpOpenAPI-1.0, FULL

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        DeployOptions[] opts = {
            DeployOptions.SERVER_ONLY
        };
        ShrinkHelper.defaultApp(server, APP_NAME_3, opts, "app.web.servlet");

        LibertyServer.setValidateApps(false);

        // Change server ports to the default ones
        OpenAPITestUtil.changeServerPorts(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        server.startServer(c.getSimpleName() + ".log");
        assertNotNull("Web application is not available at /openapi/",
            server.waitForStringInLog("CWWKT0016I.*/openapi/")); // wait for /openapi/ endpoint to become available
        assertNotNull("Web application is not available at /openapi/ui/",
            server.waitForStringInLog("CWWKT0016I.*/openapi/ui/")); // wait for /openapi/ui/ endpoint to become
                                                                    // available
        assertNotNull("Server did not report that it has started",
            server.waitForStringInLog("CWWKF0011I.*"));

        assertNotNull("Http port not opened",
            server.waitForStringInLog("CWWKO0219I.* defaultHttpEndpoint ")); // Wait for http port
        assertNotNull("Https port not opened",
            server.waitForStringInLog("CWWKO0219I.* defaultHttpEndpoint-ssl ")); // Wait for https port to open (this
                                                                                 // can sometimes take a while)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKO1650E", "CWWKO1651W");
    }

    @Test
    public void testApplicationProcessorWithServlet() throws Exception {

        // With no apps deployed, ensure that the default empty OpenAPI documentation is
        // created
        String emptyDoc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(emptyDoc);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort()));
        OpenAPITestUtil.checkPaths(openapiNode, 0);

        // Add an empty servlet app is deployed and ensure the default empty OpenAPI
        // documentation is created
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.addApplication(server, APP_NAME_3);
        String openapi = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertEquals("FAIL: Server with a single empty app should not change the default OpenAPI document.", emptyDoc,
            openapi);
    }

}
