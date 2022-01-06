/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

/**
 * Test to ensure exercise Application Processor. Here's summary of all the scenarios being tested:
 * - Deploy a single app and ensure it's documentation shows up in /openapi
 * - Deploy two apps and ensure one app's documentation shows up in /openapi
 * - Remove the app that was picked from the above scenario and ensure that the other app's documentation now shows up in /openapi
 * - Remove all apps and ensure no documentation (for any endpoint) is shown in /openapi
 * - Scenarios involving context root, host/port, servers
 * - Make a pure JAX-RS app with the ApplicationPath annotation and ensure that the annotations are scanned and a document is generated
 * - Complete flow: model, static, annotation, filter in order
 */
@RunWith(FATRunner.class)
public class ApplicationProcessorTest extends FATServletClient {
    private static final Class<?> c = ApplicationProcessorTest.class;
    private static final String APP_NAME_11 = "complete-flow";

    private static final String SERVER_NAME = "ApplicationProcessorServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
        MicroProfileActions.MP50, // mpOpenAPI-3.0, LITE
        MicroProfileActions.MP41);// mpOpenAPI-2.0, FULL

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        ShrinkHelper.defaultApp(server, APP_NAME_11, "app.web.complete.flow.*");

        LibertyServer.setValidateApps(false);

        // Change server ports to the default ones
        OpenAPITestUtil.changeServerPorts(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        server.startServer(c.getSimpleName() + ".log");
        assertNotNull("Web application is not available at /openapi/",
            server.waitForStringInLog("CWWKT0016I.*/openapi/")); // wait for /openapi/ endpoint to become available
        assertNotNull("Web application is not available at /openapi/ui/",
            server.waitForStringInLog("CWWKT0016I.*/openapi/ui/")); // wait for /openapi/ui/ endpoint to become
                                                                    // available
        assertNotNull("Server did not report that it has started", server.waitForStringInLog("CWWKF0011I.*"));
    }

    /**
     * This ensures all the applications are removed before running each test to make sure
     * we start with a clean server.xml.
     */
    @Before
    public void setUp() throws Exception {
        // Remove all the deployed apps from server.xml
        OpenAPITestUtil.removeAllApplication(server);

        // Change server ports to the default ones
        OpenAPITestUtil.changeServerPorts(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKO1650E", "CWWKO1651W");
    }

    @Test
    public void testCompleteFlow() throws Exception {
        OpenAPITestUtil.addApplication(server, APP_NAME_11);
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkServer(
            openapiNode,
            "https://test-server.com:80/#1",
            "https://test-server.com:80/#2",
            "https://test-server.com:80/#3",
            "https://test-server.com:80/#4");

        OpenAPITestUtil.checkPaths(openapiNode, 3, "/test-service/test", "/modelReader", "/staticFile");
        JsonNode infoNode = openapiNode.get("info");
        assertNotNull(infoNode);
        assertTrue(infoNode.isObject());
        JsonNode titleNode = infoNode.get("title");
        assertNotNull(titleNode);
        assertEquals(titleNode.asText(), "Title from JAX-RS app + title from filter");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testScanDisabled() throws Exception {
        PropertiesAsset config = new PropertiesAsset()
            .addProperty("mp.openapi.scan.disable", "true");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "testScanDisabled.war")
            .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
            .addAsResource(config, "META-INF/microprofile-config.properties");

        server.setTraceMarkToEndOfDefaultTrace();
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);

        // Scanning disabled, expect no paths
        OpenAPITestUtil.checkPaths(openapiNode, 0);

        // Assert that we didn't go near the scanning code
        assertThat(server.findStringsInLogsUsingMark("openapi20.utils.IndexUtils", server.getDefaultTraceFile()),
            is(empty()));
    }
}
