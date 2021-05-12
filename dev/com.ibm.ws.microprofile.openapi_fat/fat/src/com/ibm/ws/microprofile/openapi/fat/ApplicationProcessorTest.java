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
package com.ibm.ws.microprofile.openapi.fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import app.web.pure.jaxrs.JAXRSApp;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

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
    private static final String APP_NAME_1 = "appWithAnnotations";
    private static final String APP_NAME_2 = "appWithStaticDoc";
    private static final String APP_NAME_3 = "simpleServlet";
    private static final String APP_NAME_4 = "staticDocWithServerObject";
    private static final String APP_NAME_5 = "staticDocWithoutServerObject";
    private static final String APP_NAME_6 = "openAPIEarWithServer";
    private static final String APP_NAME_7 = "openAPIEarWithoutServerObject";
    private static final String APP_NAME_8 = "OpenAPIEarOverwriteContextRoot";
    private static final String APP_NAME_9 = "staticDocWithContextRootInPath";
    private static final String APP_NAME_10 = "pure-jaxrs";
    private static final String APP_NAME_11 = "complete-flow";

    @Server("ApplicationProcessorServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        ShrinkHelper.defaultApp(server, APP_NAME_1, "app.web.airlines.*");
        ShrinkHelper.defaultApp(server, APP_NAME_2);
        ShrinkHelper.defaultApp(server, APP_NAME_3, "app.web.servlet");
        ShrinkHelper.defaultApp(server, APP_NAME_10, "app.web.pure.jaxrs");
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
        assertNotNull("Server did not report that it has started",
            server.waitForStringInLog("CWWKF0011I.*"));

        assertNotNull("Http port not opened",
            server.waitForStringInLog("CWWKO0219I.* defaultHttpEndpoint ")); // Wait for http port
        assertNotNull("Https port not opened",
            server.waitForStringInLog("CWWKO0219I.* defaultHttpEndpoint-ssl ")); // Wait for https port to open (this
                                                                                 // can sometimes take a while)
    }

    /**
     * This ensures all the applications are removed before running each test to
     * make sure we start with a clean server.xml.
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

    /**
     * Tests for validating Application Processor behaviour in the following
     * scenarios: single app with OAS, two apps with OAS, two apps one with OAS and
     * one without, single app without OAS. This tests deploys and undeploys apps to
     * ensure once deployed, nothing is left behind.
     */
    @Test
    public void testApplicationProcessor() throws Exception {
        // Validate the app is deployed

        OpenAPITestUtil.addApplication(server, APP_NAME_1);
        String app1Doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(app1Doc);
        OpenAPITestUtil.checkServer(openapiNode, "https://{username}.gigantic-server.com:{port}/{basePath}",
            "https://test-server.com:80/basePath");
        OpenAPITestUtil.checkPaths(openapiNode, 16);

        // Add a second app and ensure it's not deployed
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.addApplication(server, APP_NAME_2, false);
        String openapi = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertEquals("FAIL: Only a single application must be processed by the application processor.", app1Doc,
            openapi);

        // Remove the first application and ensure now the appWithStaticDoc app is now
        // processed
        OpenAPITestUtil.removeApplication(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationProcessorAddedEvent(server, APP_NAME_2);
        String app2Doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(app2Doc);
        OpenAPITestUtil.checkServer(openapiNode, OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(),
            server.getHttpDefaultSecurePort(), APP_NAME_2));
        OpenAPITestUtil.checkPaths(openapiNode, 1);

        // Remove the appWithStaticDoc app and ensure that the default empty OpenAPI
        // documentation is created
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.removeApplication(server, APP_NAME_2);
        String emptyDoc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(emptyDoc);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort()));
        OpenAPITestUtil.checkPaths(openapiNode, 0);

        // Add an empty servlet app is deployed and ensure the default empty OpenAPI
        // documentation is created
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.addApplication(server, APP_NAME_3);
        openapi = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertEquals("FAIL: Server with a single empty app should not change the default OpenAPI document.", emptyDoc,
            openapi);

        // Now add an app with OpenAPI artifacts and ensure it shows up
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.addApplication(server, APP_NAME_1);
        openapi = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertEquals("FAIL: Only a single application must be processed by the application processor.", app1Doc,
            openapi);
    }

    /**
     * This test ensures that the OpenAPI document always reflects the correct
     * host/port
     *
     * @throws Exception s
     */
    @Test
    public void testHostPort() throws Exception {
        // Start with the default http/https ports and validate the servers
        String openapi = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort()));
        openapi = OpenAPIConnection.openAPIDocsConnection(server, true).download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort()));

        // Set http/https to different ports and validate the OpenAPI documentation is
        // reflecting it
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.changeServerPorts(server, server.getHttpSecondaryPort(), server.getHttpSecondarySecurePort());
        openapi = OpenAPIConnection.openAPIDocsConnection(server, false).port(server.getHttpSecondaryPort()).download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, server.getHttpSecondaryPort(), server.getHttpSecondarySecurePort()));
        openapi = OpenAPIConnection.openAPIDocsConnection(server, true).port(server.getHttpSecondarySecurePort())
            .download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, server.getHttpSecondaryPort(), server.getHttpSecondarySecurePort()));

        // Set http to -1, https to the default and validate the OpenAPI documentation
        // is reflecting it
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.changeServerPorts(server, -1, server.getHttpDefaultSecurePort());
        openapi = OpenAPIConnection.openAPIDocsConnection(server, true).download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, -1, server.getHttpDefaultSecurePort()));

        // Set https and http to the secondary default ports and validate the OpenAPI
        // documentation is reflecting it
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.changeServerPorts(server, server.getHttpSecondaryPort(), server.getHttpSecondarySecurePort());
        openapi = OpenAPIConnection.openAPIDocsConnection(server, false).port(server.getHttpSecondaryPort()).download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, server.getHttpSecondaryPort(), server.getHttpSecondarySecurePort()));
        openapi = OpenAPIConnection.openAPIDocsConnection(server, true).port(server.getHttpSecondarySecurePort())
            .download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, server.getHttpSecondaryPort(), server.getHttpSecondarySecurePort()));

        // Set https to -1, http to the default and validate the OpenAPI documentation
        // is reflecting it
        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.changeServerPorts(server, server.getHttpSecondaryPort(), -1);
        openapi = OpenAPIConnection.openAPIDocsConnection(server, false).port(server.getHttpSecondaryPort()).download();
        openapiNode = OpenAPITestUtil.readYamlTree(openapi);
        OpenAPITestUtil.checkServer(openapiNode,
            OpenAPITestUtil.getServerURLs(server, server.getHttpSecondaryPort(), -1));

        OpenAPITestUtil.setMarkToEndOfAllLogs(server);
        OpenAPITestUtil.changeServerPorts(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());
    }

    /**
     * This test ensures that the OpenAPI document always reflects the correct
     * server object when a WAR with server object is deployed
     *
     * @throws Exception s
     */
    @Test
    public void testContextRootWARWithServerObject() throws Exception {
        OpenAPITestUtil.addApplication(server, APP_NAME_4);
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkServer(openapiNode, "https:///MySimpleAPI/1.0.0", "https:///MySimpleAPI/2.0.0");
    }

    /**
     * This test ensures that the OpenAPI document always reflects the correct
     * server object when a WAR without server object is deployed
     *
     * @throws Exception s
     */
    @Test
    public void testContextRootWARWithoutServerObject() throws Exception {
        OpenAPITestUtil.addApplication(server, APP_NAME_5);
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkServer(openapiNode, OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(),
            server.getHttpDefaultSecurePort(), APP_NAME_5));
    }

    /**
     * This test ensures that the OpenAPI document always reflects the correct
     * server object when a EAR application includes a WAR with server object.
     *
     * @throws Exception s
     */
    @Test
    public void testContextRootEARWithServerObject() throws Exception {
        OpenAPITestUtil.addApplication(server, APP_NAME_6, "${server.config.dir}/apps/" + APP_NAME_6 + ".ear", "ear");
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkServer(openapiNode, "https:///MySimpleAPI/1.0.0", "https:///MySimpleAPI/2.0.0");
    }

    @Test
    public void testContextRootEARWithoutServerObject() throws Exception {
        OpenAPITestUtil.addApplication(server, APP_NAME_7, "${server.config.dir}/apps/" + APP_NAME_7 + ".ear", "ear");
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        // staticDocWithoutServerObject should be appended into the server since it's
        // the name of the only WAR containing OAS
        OpenAPITestUtil.checkServer(openapiNode, OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(),
            server.getHttpDefaultSecurePort(), APP_NAME_5));
    }

    @Test
    public void testOverwrittenContextRootEAR() throws Exception {
        OpenAPITestUtil.addApplication(server, APP_NAME_8, "${server.config.dir}/apps/" + APP_NAME_8 + ".ear", "ear");
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        // EAR has an Applicaiton.xml that should overwrite the context root
        OpenAPITestUtil.checkServer(openapiNode, OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(),
            server.getHttpDefaultSecurePort(), "custom-context-root"));
    }

    /**
     * This test ensures that the OpenAPI document always reflects the correct
     * server object when a WAR without server object is deployed
     *
     * @throws Exception s
     */
    @Test
    public void testContextRootWARWithPathPrefixedContextRoot() throws Exception {
        OpenAPITestUtil.addApplication(server, APP_NAME_9);
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkServer(openapiNode, OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(),
            server.getHttpDefaultSecurePort(), null));
    }

    @Test
    public void testPureJaxRsApp() throws Exception {
        OpenAPITestUtil.addApplication(server, APP_NAME_10);
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkServer(openapiNode, OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(),
            server.getHttpDefaultSecurePort(), APP_NAME_10));
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test-service/test");
    }

    /**
     * Deploys two test applications at the same time, hoping to catch possible
     * thread safety problems.
     * <p>
     * It's important that the apps have not been deployed before otherwise the
     * generated model may have already been cached.
     */
    @Test
    public void testSimultaneousDeployment() throws Exception {

        // Create and deploy two new jax-rs apps
        WebArchive test1 = ShrinkWrap.create(WebArchive.class, "test1.war")
            .addPackage(JAXRSApp.class.getPackage());

        WebArchive test2 = ShrinkWrap.create(WebArchive.class, "test2.war")
            .addPackage(JAXRSApp.class.getPackage());

        ShrinkHelper.exportAppToServer(server, test1, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, test2, SERVER_ONLY);

        // Deploy both in the same server.xml update and ensure they start
        server.setMarkToEndOfLog(server.getDefaultLogFile());

        ServerConfiguration config = server.getServerConfiguration();
        config.addApplication("test1", "${server.config.dir}/apps/test1.war", "war");
        config.addApplication("test2", "${server.config.dir}/apps/test2.war", "war");
        server.updateServerConfiguration(config);

        server.waitForStringInLogUsingMark("CWWKZ0001I.*test1");
        server.waitForStringInLogUsingMark("CWWKZ0001I.*test2");

        // Check there were no errors or warnings emitted during startup
        List<String> errorsAndWarnings = server.findStringsInLogsUsingMark("[EW] .*\\d{4}[EW]:",
            server.getDefaultLogFile());
        assertThat(errorsAndWarnings, is(empty()));
    }

    @Test
    @SkipForRepeat("mpOpenAPI-2.0")
    public void testCompleteFlow() throws Exception {
        OpenAPITestUtil.addApplication(server, APP_NAME_11);
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkServer(openapiNode, "https://test-server.com:80/#1", "https://test-server.com:80/#2",
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
}
