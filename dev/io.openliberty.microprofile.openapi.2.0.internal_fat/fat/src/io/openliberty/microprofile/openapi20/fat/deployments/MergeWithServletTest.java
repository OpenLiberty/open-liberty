/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

/**
 * Merge tests which require the servlet feature
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MergeWithServletTest {

    private static final String SERVER_NAME = "OpenAPIMergeWithServletTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
                                                             MicroProfileActions.MP60, // mpOpenAPI-3.1, FULL
                                                             MicroProfileActions.MP50, // mpOpenAPI-3.0, FULL
                                                             MicroProfileActions.MP41);// mpOpenAPI-2.0, FULL

    private final List<String> deployedApps = new ArrayList<>();

    @BeforeClass
    public static void setupServer() throws Exception {
        server.setAdditionalSystemProperties(
                                             Collections.singletonMap("mp_openapi_extensions_liberty_merged_include", "all"));
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

        // Check that war1 is documented and no merging is done, (no context root
        // prepended to path)
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
        assertServerContextRoot(openapiNode, "test1");
    }

    private void assertRest(String path) throws Exception {
        String response = new HttpRequest(server, path).run(String.class);
        assertEquals("Failed to call test resource at " + path, "OK", response);
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
