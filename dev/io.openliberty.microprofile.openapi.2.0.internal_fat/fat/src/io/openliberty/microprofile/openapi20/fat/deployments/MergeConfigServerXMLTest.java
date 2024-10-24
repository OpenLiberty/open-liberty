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
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

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
        server.stopServer("CWWKO1683W"); // Invalid info element
    }

    @After
    public void cleanup() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();
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

}
