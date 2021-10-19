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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;
import io.openliberty.microprofile.openapi20.fat.deployments.test2.DeploymentTestResource2;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

@RunWith(FATRunner.class)
public class MergeConfigTest {
    
    @Server("OpenAPIMergeTestServer")
    public static LibertyServer server;
    
    private Set<String> deployedApps = new HashSet<>();
    
    @After
    public void cleanup() throws Exception {
        server.stopServer();
        server.deleteAllDropinApplications();
        server.clearAdditionalSystemProperties();
        deployedApps.clear();
    }
    
    @Test
    public void testFirstModuleOnly() throws Exception {
        // start server
        server.startServer();
        
        // deploy app 1
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);
        
        // deploy app 2
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        deployApp(war2);
        
        // check that documentation includes only app 1
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        
        // Test that a message was output
        assertThat(server.findStringsInLogs(" I CWWKO1663I:.*Combining OpenAPI documentation from multiple modules is disabled.*test1"), hasSize(1));

        // remove app 1
        removeApp(war1);
        
        // check that documentation includes only app 2
        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test2");
    }
    
    @Test
    public void testFirstModuleOnlyEar() throws Exception {
        // start server
        server.startServer();
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
                        .addAsModules(war1, war2);
        
        deployApp(ear);
        
        // check that documentation includes only app 1
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        
        // Only one module is processed, but we don't know which one it will be
        OpenAPITestUtil.checkPaths(openapiNode, 1);
        
        // Test that a message was output
        assertThat(server.findStringsInLogs(" I CWWKO1663I.*Combining OpenAPI documentation from multiple modules is disabled.*test/(test1|test2)"), hasSize(1));
    }
    
    @Test
    public void testWarInclusion() throws Exception {
        setMergeConfig("test2, test3", null, null);
        server.startServer();
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war2);
        
        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war3);
        
        // check that documentation includes apps 2 & 3
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test2/test", "/test3/test");
        
        // Expect this warning because we're starting the server before deploying applications
        server.stopServer("CWWKO1667W"); // Config references application not deployed
    }
    
    @Test
    public void testWarExclusion() throws Exception {
        setMergeConfig("all", "test2", null);
        server.startServer();
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        deployApp(war2);
        
        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war3);
        
        // check that documentation includes apps 1 & 3, excluding 2
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test3/test");
        
        // check that merge is traced
        assertThat(server.findStringsInTrace("Merged document:"), hasSize(1));
        assertThat(server.findStringsInTrace("OpenAPIProvider retrieved from cache"), hasSize(0));
        
        // check merged model was cached and cache is used on subsequent requests
        OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertThat(server.findStringsInTrace("Merged document:"), hasSize(1));
        assertThat(server.findStringsInTrace("OpenAPIProvider retrieved from cache"), hasSize(1));
    }
    
    @Test
    public void testModuleInclusion() throws Exception {
        setMergeConfig("testEar/test2", null, null);
        server.startServer();
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        
        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                        .addAsModules(war1, war2, war3);
        deployApp(ear);
        
        // check that documentation includes only module war2
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test2");
        
        // Expect this warning because we're starting the server before deploying applications
        server.stopServer("CWWKO1667W"); // Config references application not deployed
    }
    
    @Test
    public void testModuleExclusion() throws Exception {
        setMergeConfig("all", "testEar/test3, testEar/test1", null);
        server.startServer();
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        
        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                        .addAsModules(war1, war2, war3);
        deployApp(ear);
        
        // check that documentation includes module 2, excluding 1 & 3
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test2");
    }
    
    @Test
    public void testInfoConfigured() throws Exception {
        String info = "{"
                        + "\"title\": \"test title\","
                        + "\"version\": \"3.7\","
                        + "\"termsOfService\": \"http://example.org/tos\","
                        + "\"contact\": {"
                        + "\"name\": \"John Smith\","
                        + "\"url\": \"http://example.org/contact\""
                        + "},"
                        + "\"license\": {"
                        + "\"name\": \"Apache 2.0\","
                        + "\"url\": \"https://www.apache.org/licenses/LICENSE-2.0.html\""
                        + "}"
                        + "}";
        
        setMergeConfig("all", null, info);
        server.startServer();
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        deployApp(war2);
        
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        
        JsonNode expectedInfo = new ObjectMapper().readTree(info);
        
        assertEquals(expectedInfo, openapiNode.path("info"));
    }
    
    @Test
    public void testInfoInvalidJson() throws Exception {
        setMergeConfig("all", null, "foo");
        server.startServer();
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        deployApp(war2);
        
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        
        server.findStringsInLogsUsingMark("CWWKO1665W:.*could not be parsed as JSON.*foo", server.getDefaultLogFile());
        assertEquals("Document title", "Generated API", openapiNode.path("info").path("title").asText());
        assertEquals("Version", "1.0", openapiNode.path("info").path("version").asText());
        
        server.stopServer("CWWKO1665W");
    }
    
    @Test
    public void testInfoNoTitle() throws Exception {
        setMergeConfig("all", null, "{\"version\": \"1.0\"}");
        server.startServer();
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        deployApp(war2);
        
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        
        server.findStringsInLogsUsingMark("CWWKO1664W:.*The title and version properties must be set.*1.0", server.getDefaultLogFile());
        assertEquals("Document title", "Generated API", openapiNode.path("info").path("title").asText());
        assertEquals("Version", "1.0", openapiNode.path("info").path("version").asText());
        
        server.stopServer("CWWKO1664W");
    }
    
    @Test
    public void testInfoConfiguredOneApp() throws Exception {
        String info = "{"
                        + "\"title\": \"test title\","
                        + "\"version\": \"3.7\""
                        + "}";
        
        setMergeConfig("all", null, info);
        server.startServer();
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);
        
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        
        JsonNode expectedInfo = new ObjectMapper().readTree(info);
        
        assertEquals(expectedInfo, openapiNode.path("info"));
    }
    
    @Test
    public void testUnusedConfigWarningPresent() throws Exception {
        setMergeConfig("test1, appWibble", null, null);
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        ShrinkHelper.exportDropinAppToServer(server, war1, SERVER_ONLY);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        ShrinkHelper.exportDropinAppToServer(server, war2, SERVER_ONLY);
        
        server.startServer();
        
        assertNotNull(server.waitForStringInLog("CWWKO1667W:.*appWibble")); // appWibble configured but not deployed at startup
        server.stopServer("CWWKO1667W");
    }
    
    @Test
    public void testUnusedConfigWarningNotPresent() throws Exception {
        setMergeConfig("test1, test2", null, null);
        
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        ShrinkHelper.exportDropinAppToServer(server, war1, SERVER_ONLY);
        
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                        .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        ShrinkHelper.exportDropinAppToServer(server, war2, SERVER_ONLY);
        
        server.startServer();
        
        assertNotNull(server.waitForStringInTrace("Checking for unused configuration entries"));
        assertThat(server.findStringsInLogs("CWWKO1667W"), is(empty())); // No warning about app configured but not deployed
    }

    
    private void setMergeConfig(String included, String excluded, String info) {
        Map<String, String> configProps = new HashMap<>();
        if (included != null) {
            configProps.put("mp_openapi_extensions_liberty_merged_include", included);
        }
        
        if (excluded != null) {
            configProps.put("mp_openapi_extensions_liberty_merged_exclude", excluded);
        }
        
        if (info != null) {
            configProps.put("mp_openapi_extensions_liberty_merged_info", info);
        }
        
        server.setAdditionalSystemProperties(configProps);
    }
    
    private void deployApp(Archive<?> app) throws Exception {
        server.setMarkToEndOfLog();
        ShrinkHelper.exportDropinAppToServer(server, app, SERVER_ONLY, DISABLE_VALIDATION); // Do our own validation because this is broken
        assertNotNull(server.waitForStringInLog("CWWKZ0001I:.*" + getInstalledName(app.getName())));
        deployedApps.add(app.getName());
    }
    
    private void removeApp(Archive<?> app) throws Exception {
        removeApp(app.getName());
    }
    
    private void removeApp(String archiveName) throws Exception {
        server.setMarkToEndOfLog();
        server.deleteFileFromLibertyServerRoot("dropins/" + archiveName);
        assertNotNull(server.waitForStringInLog("CWWKZ0009I:.*" + getInstalledName(archiveName)));
        deployedApps.remove(archiveName);
    }
    
    private String getInstalledName(String archiveName) {
        return archiveName.endsWith(".war") || archiveName.endsWith(".ear") ? archiveName.substring(0, archiveName.length()-4) : archiveName;
    }

}
