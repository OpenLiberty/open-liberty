/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.deployments;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.MpOpenAPIElement;
import com.ibm.websphere.simplicity.config.MpOpenAPIInfoElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi20.fat.FATSuite;
import io.openliberty.microprofile.openapi20.fat.deployments.zosconnect.app.ZOSConnectTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.zosconnect.app.ZOSConnectTestResource;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ZOSConnectExtensionTest {

    private static final String SERVER_NAME = "OpenAPIMergeWithServerXMLTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void startup() throws Exception {
        server.saveServerConfiguration();
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKO1683W", // Invalid info element
                          "CWWKO1678W", // Invalid application name
                          "CWWKO1679W" // Invalid module name
        );
    }

    @After
    public void cleanup() throws Exception {
        server.setMarkToEndOfLog();
        server.restoreServerConfiguration(); // Will stop all apps deployed via server.xml and clear openapi config
        server.waitForConfigUpdateInLogUsingMark(null);

        server.deleteAllDropinApplications(); // Will stop all dropin apps
        server.removeAllInstalledAppsForValidation(); // Validates that all apps stop

        // Delete everything from the apps directory
        server.deleteDirectoryFromLibertyServerRoot("apps");
        LibertyFileManager.createRemoteFile(server.getMachine(), server.getServerRoot() + "/apps").mkdir();
    }

    //This test creates an openAPI doc with a map containing values from two apps, and checks they preserve their ordering
    @Test
    public void testMergePreservesMapOrdering() throws Exception {
        setMergeConfig(list("test1", "test2"), null, null);

        PropertiesAsset scanConfig = new PropertiesAsset().addProperty("mp.openapi.scan.disable",
                                                                       "true");

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(ZOSConnectTestApp.class, ZOSConnectTestResource.class)
                                    .addAsResource(scanConfig, "META-INF/microprofile-config.properties")
                                    .addAsManifestResource(ZOSConnectTestApp.class.getPackage(), "static-file-foo.json", "openapi.json");
        deployApp(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(ZOSConnectTestApp.class, ZOSConnectTestResource.class)
                                    .addAsResource(scanConfig, "META-INF/microprofile-config.properties")
                                    .addAsManifestResource(ZOSConnectTestApp.class.getPackage(), "static-file-bar.json", "openapi.json");
        deployApp(war2);

        // check that documentation includes all paths in the right order
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc).get("paths");

        List<String> pathNames = new ArrayList<>();
        openapiNode.fieldNames().forEachRemaining(pathNames::add);

        assertEquals("There should be exactly six instances of [x-ibm-zcon-roles-allowed] (one per operation) in " + doc,
                     6, StringUtils.countMatches(openapiNode.toPrettyString(), "x-ibm-zcon-roles-allowed"));

        assertEquals("Bobs", openapiNode.path("/test1/foo1").path("get").path("x-ibm-zcon-roles-allowed").get(0).asText());
        assertEquals("Bobs", openapiNode.path("/test1/foo2").path("get").path("x-ibm-zcon-roles-allowed").get(0).asText());
        assertEquals("Robs", openapiNode.path("/test1/foo3").path("get").path("x-ibm-zcon-roles-allowed").get(0).asText());
        assertEquals("Staff", openapiNode.path("/test2/bar1").path("get").path("x-ibm-zcon-roles-allowed").get(0).asText());
        assertEquals("Staff", openapiNode.path("/test2/bar2").path("get").path("x-ibm-zcon-roles-allowed").get(0).asText());
        assertEquals("Guests", openapiNode.path("/test2/bar3").path("get").path("x-ibm-zcon-roles-allowed").get(0).asText());

    }

    private void setMergeConfig(List<String> included, List<String> excluded, MpOpenAPIInfoElement info) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        MpOpenAPIElement mpOpenAPI = config.getMpOpenAPIElement();

        clearMergeConfig(mpOpenAPI);

        mpOpenAPI.getIncludedApplications().addAll(applications(included));
        mpOpenAPI.getIncludedModules().addAll(modules(included));
        mpOpenAPI.getExcludedApplications().addAll(applications(excluded));
        mpOpenAPI.getExcludedModules().addAll(modules(excluded));

        mpOpenAPI.setInfo(info);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    private static void clearMergeConfig(MpOpenAPIElement mpOpenAPI) {
        List<String> includedApplications = mpOpenAPI.getIncludedApplications();
        includedApplications.clear();

        List<String> includedModules = mpOpenAPI.getIncludedModules();
        includedModules.clear();

        List<String> excludedApplications = mpOpenAPI.getExcludedApplications();
        excludedApplications.clear();

        List<String> excludedModules = mpOpenAPI.getExcludedModules();
        excludedModules.clear();

        mpOpenAPI.setInfo(null);
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
