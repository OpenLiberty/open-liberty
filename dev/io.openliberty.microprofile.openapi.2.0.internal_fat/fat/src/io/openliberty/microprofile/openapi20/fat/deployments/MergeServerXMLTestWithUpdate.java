/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.fat.deployments;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.openapi20.fat.FATSuite;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

/**
 * Test that applications are merged correctly
 * <p>
 * These tests supplement the MergeProcessor unit tests
 */
@RunWith(FATRunner.class)
public class MergeServerXMLTestWithUpdate {

    private static final String SERVER_NAME = "OpenAPIMergeWithServerXMLTestServer";
    private static final String SERVER_XML_NAME = "updatedserver.xml";
    private static final String OLD_XML_NAME = "originalserver.xml";
    private static final String APP1_XML_NAME = "App1Only.xml";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.repeatDefault(SERVER_NAME);

    private final List<String> deployedApps = new ArrayList<>();

    @BeforeClass
    public static void setupServer() throws Exception {
        server.setAdditionalSystemProperties(
                                             Collections.singletonMap("mp_openapi_extensions_liberty_merged_include", "none"));
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKF0011I"); //ready to run a smarter planet

    }

    @AfterClass
    public static void shutdownServer() throws Exception {
        server.stopServer("CWWKO1662W"); // Problems occurred while merging
    }

    @After
    public void cleanup() throws Exception {
        server.setMarkToEndOfLog();
        server.deleteAllDropinApplications();

        List<String> failedToStop = new ArrayList<>();
        for (String app : deployedApps) {
            if (server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + app) == null) {
                failedToStop.add(app);
            }
        }

        if (!failedToStop.isEmpty()) {
            throw new AssertionError("The following apps failed to stop: " + failedToStop);
        }

    }

    @Test
    public void testTwoWars() throws Exception {
        System.out.println("testTwoWars - begin");
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        deployApp(war1);

        //Swap the server.xml from saying everything is excluded to saying its included
        logOnServer("/test1/log", "testTwoWarsUpdate1");
        server.swapInServerXMLFromPublish(SERVER_XML_NAME);
        server.waitForStringInLogUsingMark("CWWKG0017I"); //The server configuration was successfully updated

        assertRest("/test1/test");

        // With one app deployed, we should only have the paths from war1 listed
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        assertEquals("/test1/test", OpenAPITestUtil.expandPath(openapiNode, "/test"));

        // check that merge is not traced
        assertThat(server.findStringsInLogsAndTraceUsingMark("Merged document:"), hasSize(0));
        assertThat(server.findStringsInLogsAndTraceUsingMark("OpenAPIProvider retrieved from cache"), hasSize(0));

        // check merged model was cached and cache is used on subsequent requests
        OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertThat(server.findStringsInLogsAndTraceUsingMark("Merged document:"), hasSize(0));
        assertThat(server.findStringsInLogsAndTraceUsingMark("OpenAPIProvider retrieved from cache"), hasSize(1));

        deployApp(war2);
        deployApp(war3);
        assertRest("/test1/test");
        assertRest("/test2/test");
        assertRest("/test3/test");
        //Swap the server.xml from saying everything is excluded to saying its included
        logOnServer("/test1/log", "testTwoWarsUpdate2");
        //server.swapInServerXMLFromPublish(SERVER_XML_NAME);
        //server.waitForStringInLogUsingMark("CWWKG0017I"); //The server configuration was successfully updated

        // With three apps deployed, we should have a merged doc with paths from all
        // apps listed
        // and a new info section
        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);
        System.out.println(new ObjectMapper().writeValueAsString(openapiNode));
        OpenAPITestUtil.checkPaths(openapiNode, 3, "/test1/test", "/test2/test", "/test3/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
        assertServerContextRoot(openapiNode, null);
        assertEquals("/test1/test", OpenAPITestUtil.expandPath(openapiNode, "/test1/test"));

        // check that merge is traced
        assertThat(server.findStringsInLogsAndTraceUsingMark("Merged document:"), hasSize(1));
        assertThat(server.findStringsInLogsAndTraceUsingMark("OpenAPIProvider retrieved from cache"), hasSize(0));

        // check merged model was cached and cache is used on subsequent requests
        OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertThat(server.findStringsInLogsAndTraceUsingMark("Merged document:"), hasSize(1));
        assertThat(server.findStringsInLogsAndTraceUsingMark("OpenAPIProvider retrieved from cache"), hasSize(1));

        // Remove war1
        undeployApp(war1);

        // Now we should just have war2 and war3
        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test2/test", "/test3/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
        assertServerContextRoot(openapiNode, null);

        // check that merge is traced
        assertThat(server.findStringsInLogsAndTraceUsingMark("Merged document:"), hasSize(1));
        assertThat(server.findStringsInLogsAndTraceUsingMark("OpenAPIProvider retrieved from cache"), hasSize(0));

        // check merged model was cached and cache is used on subsequent requests
        OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertThat(server.findStringsInLogsAndTraceUsingMark("Merged document:"), hasSize(1));
        assertThat(server.findStringsInLogsAndTraceUsingMark("OpenAPIProvider retrieved from cache"), hasSize(1));

        //Cleanup after test
        server.swapInServerXMLFromPublish(OLD_XML_NAME);
        server.waitForStringInLogUsingMark("CWWKG0017I"); //The server configuration was successfully updated
    }

    @Test
    public void testMultiModuleEar() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
                                          .addAsModules(war1, war2);

        deployApp(ear);

        //Swap the server.xml from saying everything is excluded to saying its included
        server.swapInServerXMLFromPublish(SERVER_XML_NAME);
        server.waitForStringInLogUsingMark("CWWKG0017I"); //The server configuration was successfully updated

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test2/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
        assertServerContextRoot(openapiNode, null);

        //Cleanup after test
        server.swapInServerXMLFromPublish(OLD_XML_NAME);
        server.waitForStringInLogUsingMark("CWWKG0017I"); //The server configuration was successfully updated
    }

    private void assertRest(String path) throws Exception {
        String response = new HttpRequest(server, path).run(String.class);
        assertEquals("Failed to call test resource at " + path, "OK", response);
    }

    private void logOnServer(String pathUpToEndpoint, String message) throws Exception {
        String path = pathUpToEndpoint + "?message=" + message;
        new HttpRequest(server, path).run(String.class);
    }

    private void assertServerContextRoot(JsonNode model,
                                         String contextRoot) {
        OpenAPITestUtil.checkServer(model,
                                    OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(), -1, contextRoot));
    }

    private void deployApp(Archive<?> archive) throws Exception {
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        ShrinkHelper.exportDropinAppToServer(server, archive, SERVER_ONLY, DISABLE_VALIDATION);
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getName(archive)));
        deployedApps.add(getName(archive));
    }

    private void undeployApp(Archive<?> archive) throws Exception {
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.deleteFileFromLibertyServerRoot("dropins/" + archive.getName());
        deployedApps.remove(getName(archive));
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getName(archive)));
    }

    private String getName(Archive<?> archive) {
        return getName(archive.getName());
    }

    private String getName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot != -1) {
            fileName = fileName.substring(0, lastDot);
        }
        return fileName;
    }

}
