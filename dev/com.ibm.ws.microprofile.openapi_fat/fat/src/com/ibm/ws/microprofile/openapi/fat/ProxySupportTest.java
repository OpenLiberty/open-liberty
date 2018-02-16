/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import test.common.TestLogger;

/**
 * Tests to ensure that the proxy support feature changes server object in the final document.
 * This class tests the following scenarios: requests to /openapi with no Referer header, requests
 * to /openapi with Referer header but different ports than the server and finally requests to /openapi
 * with Referer header and same ports than the server. In these three scenarios, requests are sent
 * to both http and https ports.
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class ProxySupportTest extends FATServletClient {
    private static final Class<?> c = ProxySupportTest.class;
    private static final String REFERER = "Referer";

    private static final String APP_NAME_1 = "pure-jaxrs";

    @Server("ApplicationProcessorServer")
    public static LibertyServer server;

    @Rule
    public final TestLogger logger = new TestLogger();

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        ShrinkHelper.defaultApp(server, APP_NAME_1, "app.web.pure.jaxrs");

        LibertyServer.setValidateApps(false);
        server.startServer(c.getSimpleName() + ".log");

        OpenAPITestUtil.addApplication(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationProcessorProcessedEvent(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationProcessorAddedEvent(server, APP_NAME_1);
    }

    /**
     * This ensures all the applications are removed before running each test to make sure
     * we start with a clean server.xml.
     */
    @Before
    public void setUp() throws Exception {
        // Change server ports to the default ones
        OpenAPITestUtil.changeServerPorts(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKO1650E");
    }

    /**
     * Tests to ensure when the call to /openapi doesn't include referer header, the OAS doc has the server's host and port
     *
     * @throws Exception
     */
    @Test
    public void testBasic() throws Exception {
        String openapi = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode, OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort(), APP_NAME_1));

        openapi = OpenAPIConnection.openAPIDocsConnection(server, true).download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode, OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort(), APP_NAME_1));
    }

    /**
     * Tests to ensure when the call to /openapi with a referer header that has different port than the server,
     * the OAS doc has correct host and port
     *
     * @throws Exception
     */
    @Test
    public void testRefererDifferentPort() throws Exception {
        String openapi = OpenAPIConnection.openAPIDocsConnection(server, false).header(REFERER, "http://openliberty.io/openapi").download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode, "http://openliberty.io" + "/" + APP_NAME_1);

        openapi = OpenAPIConnection.openAPIDocsConnection(server, true).header(REFERER, "https://openliberty.io/openapi").download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode, "https://openliberty.io" + "/" + APP_NAME_1);
    }

    /**
     * Tests to ensure when the call to /openapi with a referer header that has the same port than the server,
     * the OAS doc has correct host and port
     *
     * @throws Exception
     */
    @Test
    public void testRefererSamePort() throws Exception {
        String referer = "http://openliberty.io:" + server.getHttpDefaultPort() + "/openapi";
        String openapi = OpenAPIConnection.openAPIDocsConnection(server, false).header(REFERER, referer).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode, "http://openliberty.io:" + server.getHttpDefaultPort() + "/" + APP_NAME_1,
                                    "https://openliberty.io:" + server.getHttpDefaultSecurePort() + "/" + APP_NAME_1);

        referer = "https://openliberty.io:" + server.getHttpDefaultSecurePort() + "/openapi";
        openapi = OpenAPIConnection.openAPIDocsConnection(server, true).header(REFERER, referer).download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode, "http://openliberty.io:" + server.getHttpDefaultPort() + "/" + APP_NAME_1,
                                    "https://openliberty.io:" + server.getHttpDefaultSecurePort() + "/" + APP_NAME_1);
    }
}
