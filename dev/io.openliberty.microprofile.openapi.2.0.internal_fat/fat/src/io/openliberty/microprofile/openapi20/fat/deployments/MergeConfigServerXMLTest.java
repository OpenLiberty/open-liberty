/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.deployments;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil.assertEqualIgnoringPropertyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.MpOpenAPIElement;
import com.ibm.websphere.simplicity.config.MpOpenAPIInfoElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi20.fat.FATSuite;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;
import io.openliberty.microprofile.openapi20.fat.deployments.test2.DeploymentTestResource2;
import io.openliberty.microprofile.openapi20.fat.utils.ApplicationXmlAsset;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;
import io.openliberty.microprofile.openapi20.fat.utils.WebXmlAsset;

@RunWith(FATRunner.class)
public class MergeConfigServerXMLTest {

    private static final String SERVER_NAME = "OpenAPIMergeWithServerXMLTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void startup() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKO1683W" // Invalid info element
        );
    }

    @After
    public void cleanup() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();
    }

    @Test
    public void testDefaultInclusion() throws Exception {
        setMergeConfig(null, null, null);

        // deploy app 1
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);

        // deploy app 2
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war2);

        // Download the documentation
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);

        if (OpenAPITestUtil.getOpenAPIFeatureVersion() < 4.0f) {
            // Default is first module only
            // check that documentation includes only app 1
            OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");

            // Test that a message was output
            assertThat(server.findStringsInLogs(" I CWWKO1663I:.*Combining OpenAPI documentation from multiple modules is disabled.*test1"),
                       hasSize(1));

            // remove app 1
            removeApp(war1);

            // check that documentation includes only app 2
            doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
            openapiNode = OpenAPITestUtil.readYamlTree(doc);
            OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        } else {
            // Default is all modules
            // Check that documentation includes both app 1 and app 2
            OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test2/test");

            // Check there's no "first module only" message
            assertThat(server.findStringsInLogs("CWWKO1663I"),
                       hasSize(0));
        }
    }

    @Test
    public void testFirstModuleOnly() throws Exception {
        setMergeConfig(list("first"), null, null);

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

        // remove app 1
        removeApp(war1);

        // check that documentation includes only app 2
        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test2");

        // Test that merging disabled message was output (at some point)
        assertThat(server.findStringsInLogs(" I CWWKO1663I:.*Combining OpenAPI documentation from multiple modules is disabled."),
                   hasSize(1));
    }

    @Test
    public void testFirstModuleOnlyEar() throws Exception {
        setMergeConfig(list("first"), null, null);

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

        // Test that merging disabled message was output (at some point)
        assertThat(server.findStringsInLogs(" I CWWKO1663I:.*Combining OpenAPI documentation from multiple modules is disabled."),
                   hasSize(1));
    }

    @Test
    public void testWarInclusion() throws Exception {
        setMergeConfig(list("test2", "test3"), null, null);

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
    }

    @Test
    public void testWarExclusion() throws Exception {
        setMergeConfig(list("all"), list("test2"), null);

        server.setTraceMarkToEndOfDefaultTrace();

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
        assertThat(server.findStringsInLogsAndTraceUsingMark("Merged document:"), hasSize(1));
        assertThat(server.findStringsInLogsAndTraceUsingMark("OpenAPIProvider retrieved from cache"), hasSize(0));

        // check merged model was cached and cache is used on subsequent requests
        server.setTraceMarkToEndOfDefaultTrace();
        OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertThat(server.findStringsInLogsAndTraceUsingMark("Merged document:"), hasSize(0));
        assertThat(server.findStringsInLogsAndTraceUsingMark("OpenAPIProvider retrieved from cache"), hasSize(1));
    }

    @Test
    public void testModuleInclusion() throws Exception {
        setMergeConfig(list("testEar/test2"), null, null);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                    .setWebXML(new WebXmlAsset("test3-named"));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModules(war1, war2, war3);
        deployApp(ear);

        // check that documentation includes only module war2
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test2");
    }

    @Test
    public void testModuleExclusion() throws Exception {
        setMergeConfig(list("all"), list("testEar/test3-named", "testEar/test1"), null);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                    .setWebXML(new WebXmlAsset("test3-named"));

        ApplicationXmlAsset appXml = new ApplicationXmlAsset("testEar-named").withWebModule(war1, "test1-custom")
                                                                             .withWebModule(war2, "test2-custom")
                                                                             .withWebModule(war3, "test3-custom");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModules(war1, war2, war3)
                                          .setApplicationXML(appXml);

        deployApp(ear);

        // check that documentation includes module 2, excluding 1 & 3
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test2");
    }

    @Test
    public void testSelectionConfigUpdateWithAppsDeployed() throws Exception {
        // Set the initial config
        setMergeConfig(list("none"), null, null);
        // Deploy apps (ear + war)
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                    .setWebXML(new WebXmlAsset("test3-named"));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModules(war1, war2, war3);
        deployApp(ear);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWar.war")
                                   .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 0);

        // Include just war
        setMergeConfig(list("testWar"), null, null);
        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        OpenAPITestUtil.checkServerContextRoots(openapiNode, "/testWar");

        // Include just module
        setMergeConfig(list("testEar/test1"), null, null);
        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        OpenAPITestUtil.checkServerContextRoots(openapiNode, "/test1");

        // Include war and one module
        setMergeConfig(list("testWar", "testEar/test3-named"), null, null);
        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test3-named/test", "/testWar/test");
        OpenAPITestUtil.checkServerContextRoots(openapiNode, "");

        // Include all exclude a module
        setMergeConfig(list("all"), list("testEar/test2"), null);
        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 3, "/test1/test", "/test3-named/test", "/testWar/test");
        OpenAPITestUtil.checkServerContextRoots(openapiNode, "");
    }

    @Test
    public void testInfoConfigured() throws Exception {
        MpOpenAPIInfoElement info = new MpOpenAPIInfoElement();
        info.setTitle("test title");
        info.setVersion("3.7");
        info.setTermsOfService("http://example.org/tos");
        info.setContactName("John Smith");
        info.setContactUrl("http://example.org/contact");
        info.setLicenseName("Apache 2.0");
        info.setLicenseUrl("https://www.apache.org/licenses/LICENSE-2.0.html");
        info.setDescription("This is a test API to test configuration of the info object");

        setMergeConfig(list("all"), null, info);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        deployApp(war2);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);

        ObjectNode expectedInfo = JsonNodeFactory.instance.objectNode();
        expectedInfo.put("title", "test title");
        expectedInfo.put("version", "3.7");
        expectedInfo.put("termsOfService", "http://example.org/tos");
        expectedInfo.put("description", "This is a test API to test configuration of the info object");

        ObjectNode contact = expectedInfo.putObject("contact");
        contact.put("name", "John Smith");
        contact.put("url", "http://example.org/contact");

        ObjectNode license = expectedInfo.putObject("license");
        license.put("name", "Apache 2.0");
        license.put("url", "https://www.apache.org/licenses/LICENSE-2.0.html");

        assertEqualIgnoringPropertyOrder(expectedInfo, openapiNode.path("info"));

        // Now update the info and check the change is reflected
        info.setVersion("3.8");
        setMergeConfig(list("all"), null, info);

        doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(doc);

        expectedInfo.put("version", "3.8");

        assertEqualIgnoringPropertyOrder(expectedInfo, openapiNode.path("info"));
    }

    @Test
    public void testInfoConfigured31() throws Exception {
        MpOpenAPIInfoElement info = new MpOpenAPIInfoElement();
        info.setTitle("test title");
        info.setVersion("3.7");
        info.setTermsOfService("http://example.org/tos");
        info.setContactName("John Smith");
        info.setContactUrl("http://example.org/contact");
        info.setLicenseName("Apache 2.0");
        info.setLicenseUrl("https://www.apache.org/licenses/LICENSE-2.0.html");
        info.setDescription("This is a test API to test configuration of the info object");
        info.setLicenseIdentifier("Apache-2.0");
        info.setSummary("A test API");

        setMergeConfig(list("all"), null, info);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        deployApp(war2);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);

        ObjectNode expectedInfo = JsonNodeFactory.instance.objectNode();
        expectedInfo.put("title", "test title");
        expectedInfo.put("version", "3.7");
        expectedInfo.put("termsOfService", "http://example.org/tos");
        expectedInfo.put("description", "This is a test API to test configuration of the info object");

        ObjectNode contact = expectedInfo.putObject("contact");
        contact.put("name", "John Smith");
        contact.put("url", "http://example.org/contact");

        ObjectNode license = expectedInfo.putObject("license");
        license.put("name", "Apache 2.0");
        license.put("url", "https://www.apache.org/licenses/LICENSE-2.0.html");

        if (OpenAPITestUtil.getOpenAPIFeatureVersion() >= 4.0f) {
            // Additional fields only available in OpenAPI 3.1
            expectedInfo.put("summary", "A test API");
            license.put("identifier", "Apache-2.0");
        }

        assertEqualIgnoringPropertyOrder(expectedInfo, openapiNode.path("info"));
    }

    @Test
    public void testInfoNoTitle() throws Exception {
        MpOpenAPIInfoElement info = new MpOpenAPIInfoElement();
        info.setVersion("1.0");

        server.setMarkToEndOfLog();
        setMergeConfig(list("all"), null, info);
        List<String> warnings = server.findStringsInLogsUsingMark("CWWKO1683W: The mpOpenAPI info configuration element in the server.xml file is invalid.",
                                                                  server.getDefaultLogFile());
        assertThat("Invalid info configuration warnings", warnings, hasSize(1));

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        deployApp(war2);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);

        assertEquals("Document title", "Generated API", openapiNode.path("info").path("title").asText());
        assertEquals("Version", "1.0", openapiNode.path("info").path("version").asText());

        // Check we only get the warning once
        warnings = server.findStringsInLogsUsingMark("CWWKO1683W: The mpOpenAPI info configuration element in the server.xml file is invalid.",
                                                     server.getDefaultLogFile());
        assertThat("Invalid info configuration warnings", warnings, hasSize(1));
    }

    @Test
    public void testInfoNoVersion() throws Exception {
        MpOpenAPIInfoElement info = new MpOpenAPIInfoElement();
        info.setTitle("test title");

        server.setMarkToEndOfLog();
        setMergeConfig(list("all"), null, info);
        List<String> warnings = server.findStringsInLogsUsingMark("CWWKO1683W: The mpOpenAPI info configuration element in the server.xml file is invalid.",
                                                                  server.getDefaultLogFile());
        assertThat("Invalid info configuration warnings", warnings, hasSize(1));

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        deployApp(war2);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);

        assertEquals("Document title", "Generated API", openapiNode.path("info").path("title").asText());
        assertEquals("Version", "1.0", openapiNode.path("info").path("version").asText());

        // Check we only get the warning once
        warnings = server.findStringsInLogsUsingMark("CWWKO1683W: The mpOpenAPI info configuration element in the server.xml file is invalid.",
                                                     server.getDefaultLogFile());
        assertThat("Invalid info configuration warnings", warnings, hasSize(1));
    }

    @Test
    public void testInfoConfiguredOneApp() throws Exception {
        MpOpenAPIInfoElement info = new MpOpenAPIInfoElement();
        info.setTitle("test title");
        info.setVersion("3.7");

        setMergeConfig(list("all"), null, info);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);

        JsonNode expectedInfo = JsonNodeFactory.instance.objectNode()
                                                        .put("title", "test title")
                                                        .put("version", "3.7");

        assertEqualIgnoringPropertyOrder(expectedInfo, openapiNode.path("info"));
    }

    private void setMergeConfig(List<String> included, List<String> excluded, MpOpenAPIInfoElement info) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        MpOpenAPIElement mpOpenAPI = config.getMpOpenAPIElement();

        List<String> includedApplications = mpOpenAPI.getIncludedApplications();
        includedApplications.clear();
        includedApplications.addAll(applications(included));

        List<String> includedModules = mpOpenAPI.getIncludedModules();
        includedModules.clear();
        includedModules.addAll(modules(included));

        List<String> excludedApplications = mpOpenAPI.getExcludedApplications();
        excludedApplications.clear();
        excludedApplications.addAll(applications(excluded));

        List<String> excludedModules = mpOpenAPI.getExcludedModules();
        excludedModules.clear();
        excludedModules.addAll(modules(excluded));

        mpOpenAPI.setInfo(info);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static List<String> applications(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                     .filter(v -> !v.contains("/"))
                     .collect(Collectors.toList());
    }

    private static List<String> modules(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                     .filter(v -> v.contains("/"))
                     .collect(Collectors.toList());
    }

    private void deployApp(Archive<?> app) throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, app, SERVER_ONLY);
    }

    private void removeApp(Archive<?> app) throws Exception {
        server.deleteFileFromLibertyServerRoot("dropins/" + app.getName());
        server.removeInstalledAppForValidation(getInstalledName(app.getName()));
    }

    private String getInstalledName(String archiveName) {
        return archiveName.endsWith(".war") || archiveName.endsWith(".ear") ? archiveName.substring(0, archiveName.length() - 4) : archiveName;
    }

}
