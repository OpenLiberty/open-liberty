/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.MpOpenAPIElement;
import com.ibm.websphere.simplicity.config.MpOpenAPIElement.MpOpenAPIElementBuilder;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

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
import io.openliberty.microprofile.openapi20.fat.utils.WebXmlAsset;

/**
 * Test that applications are merged correctly
 * <p>
 * These tests supplement the MergeProcessor unit tests
 */
@RunWith(FATRunner.class)
public class MergeServerXMLTest {

    private static final String SERVER_NAME = "OpenAPIMergeWithServerXMLTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.repeatDefault(SERVER_NAME);

    private final Map<String, Archive<?>> deployedConfiguredApps = new HashMap<>(); //configued in server.xml
    private final List<String> deployedApps = new ArrayList<>(); //apps in dropins

    @BeforeClass
    public static void setupServer() throws Exception {
        //This will be ignored because we have openapi includes/excludes in server.xml
        server.setAdditionalSystemProperties(
                                             Collections.singletonMap("mp_openapi_extensions_liberty_merged_include", "none"));
        server.saveServerConfiguration();
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKF0011I"); //ready to run a smarter planet
    }

    @AfterClass
    public static void shutdownServer() throws Exception {
        server.stopServer("CWWKO1662W", "CWWKO1678W", "CWWKO1679W"); // Problems occurred while merging. Warning message for invalid config we're testing.
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

        for (String app : deployedConfiguredApps.keySet()) {
            if (deleteAppFromApps(deployedConfiguredApps.get(app), app) == null) {
                failedToStop.add(app);
            }
        }

        if (!failedToStop.isEmpty()) {
            throw new AssertionError("The following apps failed to stop: " + failedToStop);
        }
        server.restoreServerConfiguration();
    }

    @Test
    public void testInvalidServerXML() throws Exception {
        server.setMarkToEndOfLog();

        MpOpenAPIElement.MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                .addIncludedApplicaiton("testEar/invalid")
                                                .addExcludedModule("testEar")
                                                .buildAndPushToServer();

        List<String> list = new ArrayList<>(Arrays.asList("CWWKO1678W", "CWWKO1679W"));
        server.waitForStringsInLogUsingMark(list);

    }

    @Test
    public void testMultiModuleEarWithServerXMLAppNameAndWebXmlModuleName() throws Exception {

        String appName = "serverXMLName";

        MpOpenAPIElement.MpOpenAPIElementBuilder builder = MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                                                  .addIncludedApplicaiton("serverXMLName")
                                                                                  .addExcludedModule("serverXMLName/nameFromWar");

        ServerConfiguration serverConfig = server.getServerConfiguration().clone();
        serverConfig.addApplication(appName, "testEar.ear", "ear");
        MpOpenAPIElement openAPIConfig = serverConfig.getMpOpenAPIElement();
        builder.buildAndOverwrite(openAPIConfig);

        server.updateServerConfiguration(serverConfig);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                    .setWebXML(new WebXmlAsset("nameFromWar"));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModules(war1, war2, war3);

        deployAppToApps(ear, appName);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test2/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
        assertServerContextRoot(openapiNode, null);
    }

    @Test
    public void testMultiModuleEarComplexServerXML() throws Exception {

        MpOpenAPIElement.MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                .addIncludedApplicaiton("testEar")
                                                .addExcludedModule("testEar/test3")
                                                .addExcludedModule("testEar/test4")
                                                .buildAndPushToServer();

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "test3.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war4 = ShrinkWrap.create(WebArchive.class, "test4.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModules(war1, war2, war3, war4);

        deployApp(ear);

        logOnServer("/test1/log", "testMultiModuleEarComplexServerXML");

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test2/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
        assertServerContextRoot(openapiNode, null);
    }

    @Test
    public void testMPConfigIgnoredInServerXMLMode() throws Exception {

        MpOpenAPIElement.MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                .addIncludedApplicaiton("all")
                                                .buildAndPushToServer();

        //The combo of all and an exclude should be all except testEar/test2, but since this is set via
        //mpConfig this should be ignored.
        setMergeConfig(null, "testEar/test2", null);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModules(war1, war2);

        deployApp(ear);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test2/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
        assertServerContextRoot(openapiNode, null);
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

    private void deployAppToApps(Archive<?> archive, String appName) throws Exception {
        deployAppToApps(archive, appName, true);
    }

    private void deployAppToApps(Archive<?> archive, String appName, boolean serverStarted) throws Exception {
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        ShrinkHelper.exportAppToServer(server, archive, SERVER_ONLY, DISABLE_VALIDATION);
        if (serverStarted) {
            assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getName(appName)));
        }
        deployedConfiguredApps.put(appName, archive);
    }

    private String deleteAppFromApps(Archive<?> archive, String appName) throws Exception {
        ServerConfiguration sc = server.getServerConfiguration();
        sc.removeApplicationsByName(appName);
        server.updateServerConfiguration(sc);
        server.deleteFileFromLibertyServerRoot("apps/" + archive.getName());
        deployedConfiguredApps.remove(appName);
        return server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getName(appName));
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

    private void logOnServer(String pathUpToEndpoint, String message) throws Exception {
        String path = pathUpToEndpoint + "?message=" + message;
        new HttpRequest(server, path).run(String.class);
    }

    private void setMergeConfig(String included,
                                String excluded,
                                String info) {
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

}
