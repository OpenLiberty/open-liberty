/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import java.net.HttpURLConnection;
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
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi20.fat.FATSuite;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

/**
 * Tests for the {@code ?application=<name>} query parameter on the {@code /openapi} endpoint.
 * <p>
 * The parameter was added to allow clients to retrieve the OpenAPI document for a single named
 * application rather than the merged document for all deployed applications.
 */
@RunWith(FATRunner.class)
public class SingleAppQueryParamTest {

    private static final String SERVER_NAME = "OpenAPIMergeTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.repeatDefault(SERVER_NAME);

    private final List<String> deployedApps = new ArrayList<>();

    @BeforeClass
    public static void setupServer() throws Exception {
        server.setAdditionalSystemProperties(
                                             Collections.singletonMap("mp_openapi_extensions_liberty_merged_include", "all"));
        server.startServer();
    }

    @AfterClass
    public static void shutdownServer() throws Exception {
        server.stopServer();
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
        deployedApps.clear();

        if (!failedToStop.isEmpty()) {
            throw new AssertionError("The following apps failed to stop: " + failedToStop);
        }
    }

    /**
     * With a single deployed app, {@code ?application=<name>} returns the document for that app.
     */
    @Test
    public void testApplicationParamSingleApp() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false)
                                      .queryParam("application", "test1")
                                      .download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test1/test");
    }

    /**
     * With two deployed apps, {@code ?application=<name>} returns only that app's document,
     * while omitting the parameter still returns the merged document.
     */
    @Test
    public void testApplicationParamOneOfMultipleApps() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);
        deployApp(war2);

        // No parameter: merged document from both apps
        String mergedDoc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode mergedNode = OpenAPITestUtil.readYamlTree(mergedDoc);
        OpenAPITestUtil.checkPaths(mergedNode, 2, "/test1/test", "/test2/test");

        // ?application=test1: only test1's paths
        String app1Doc = OpenAPIConnection.openAPIDocsConnection(server, false)
                                          .queryParam("application", "test1")
                                          .download();
        JsonNode app1Node = OpenAPITestUtil.readYamlTree(app1Doc);
        OpenAPITestUtil.checkPaths(app1Node, 1, "/test1/test");

        // ?application=test2: only test2's paths
        String app2Doc = OpenAPIConnection.openAPIDocsConnection(server, false)
                                          .queryParam("application", "test2")
                                          .download();
        JsonNode app2Node = OpenAPITestUtil.readYamlTree(app2Doc);
        OpenAPITestUtil.checkPaths(app2Node, 1, "/test2/test");
    }

    /**
     * An unrecognised application name must return HTTP 404 Not Found.
     */
    @Test
    public void testApplicationParamNotFound() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        deployApp(war1);

        OpenAPIConnection.openAPIDocsConnection(server, false)
                         .queryParam("application", "doesNotExist")
                         .expectedResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
                         .download();
    }

    /**
     * For a multi-module EAR, {@code ?application=<ear-name>} returns the document with paths
     * from all web modules within that EAR merged together.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testApplicationParamMultiModuleEar() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
                                          .addAsModules(war1, war2);
        deployApp(ear);

        // The EAR is registered under the name "test"; its two WAR modules should be merged
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false)
                                      .queryParam("application", "test")
                                      .download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test2/test");
    }

    // --- helpers ---

    private void deployApp(Archive<?> archive) throws Exception {
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        ShrinkHelper.exportDropinAppToServer(server, archive, SERVER_ONLY, DISABLE_VALIDATION);
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getName(archive)));
        deployedApps.add(getName(archive));
    }

    private String getName(Archive<?> archive) {
        String name = archive.getName();
        int dot = name.lastIndexOf('.');
        return dot != -1 ? name.substring(0, dot) : name;
    }
}
