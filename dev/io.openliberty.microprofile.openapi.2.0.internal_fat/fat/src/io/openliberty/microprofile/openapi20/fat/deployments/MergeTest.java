/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
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
public class MergeTest {

    @Server("OpenAPIMergeTestServer")
    public static LibertyServer server;
    
    private List<String> deployedApps = new ArrayList<>();

    @BeforeClass
    public static void setupServer() throws Exception {
        server.setAdditionalSystemProperties(Collections.singletonMap("mp_openapi_extensions_liberty_merged_include", "all"));
        server.startServer();
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
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        deployApp(war1);

        assertRest("/test1/test");

        // With one app deployed, we should only have the paths from war1 listed
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        
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

        // With three apps deployed, we should have a merged doc with paths from all apps listed
        // and a new info section
        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 3, "/test1/test", "/test2/test", "/test3/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
        assertServerContextRoot(openapiNode, null);
        
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

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test2/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
        assertServerContextRoot(openapiNode, null);
    }

    @Test
    public void testNonJaxrsEarModule() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(TestServlet.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
                                          .addAsModules(war1, war2);

        deployApp(ear);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        assertServerContextRoot(openapiNode, "test1");
    }

    @Test
    public void testNonJaxrsAppNotMerged() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(TestServlet.class);

        deployApp(war1);
        deployApp(war2);

        assertRest("/test1/test");
        assertRest("/test2/test");

        // Check that war1 is documented and no merging is done, (no context root prepended to path)
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        assertServerContextRoot(openapiNode, "test1");
    }

    @Test
    public void testMergeClash() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                    .addAsManifestResource(openApiJsonWithServers("http://example.org/server1"), "openapi.json");

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                    .addAsManifestResource(openApiJsonWithServers("http://example.org/server2"), "openapi.json");

        deployApp(war1);
        assertRest("/test1/test");

        deployApp(war2);
        assertRest("/test2/test");
        
        server.setMarkToEndOfLog();

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        OpenAPITestUtil.checkServer(openapiNode, "http://example.org/server1");

        // check for clash message
        assertNotNull(server.waitForStringInLogUsingMark("CWWKO1662W", server.getDefaultLogFile()));
        assertThat(server.findStringsInLogsUsingMark(" - The /test path.*test2.* clashes with a path from the.*test1.*test2.*will not be merged", server.getDefaultLogFile()), hasSize(1));
    }

    private Asset openApiJsonWithServers(String... urls) {
        ObjectNode openApi = JsonNodeFactory.instance.objectNode();
        ArrayNode servers = openApi.putArray("servers");
        for (String url : urls) {
            ObjectNode server = servers.addObject();
            server.put("description", "test server");
            server.put("url", url);
        }
        try {
            return new StringAsset(new ObjectMapper().writer().writeValueAsString(openApi));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error creating test openapi json file", e);
        }
    }

    private void assertRest(String path) throws Exception {
        String response = new HttpRequest(server, path).run(String.class);
        assertEquals("Failed to call test resource at " + path, "OK", response);
    }

    private void assertServerContextRoot(JsonNode model, String contextRoot) {
        OpenAPITestUtil.checkServer(model, OpenAPITestUtil.getServerURLs(server, server.getHttpDefaultPort(), -1, contextRoot));
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
