/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.configureEnvVariable;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.fat.utils.OpenAPIConnection;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import mpOpenAPIConfig.OpenAPIConfigTestResource;

/**
 * Tests for the mpOpenAPI config element
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class OpenAPIConfigTest extends FATServletClient {

    private static final String SERVER_NAME = "checkpointMPOpenAPIConfig";

    private static String DEFAULT_DOC_PATH = "/openapi/docs";
    private static String DEFAULT_UI_PATH = DEFAULT_DOC_PATH + "/ui";

    private static String APP_NAME = "mpOpenAPIConfigApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultMPRepeat(SERVER_NAME);

    @BeforeClass
    public static void deployApp() throws Exception {
        // Set guards
        server.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));

        // Deploy test app
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClass(OpenAPIConfigTestResource.class);
        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
    }

    @Before
    public void setup() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    @After
    public void teardown() throws Exception {
        try {
            server.stopServer("CWWKE0701E: .*BundleContext is no longer valid io.openliberty.microprofile.openapi.2.0.internal.servlet*");
        } finally {
            configureEnvVariable(server, emptyMap());
        }
    }

    @Test
    public void testConfigureDynamicUpdate() throws Exception {
        assertWebAppStarts(DEFAULT_DOC_PATH);
        assertWebAppStarts(DEFAULT_UI_PATH);
        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath(DEFAULT_UI_PATH);

        server.stopServer(false, "");

        Map<String, String> configMap = new HashMap<>();
        configMap.put("OPEN_API_DOC_PATH", "/foo");
        configMap.put("OPEN_API_UI_PATH", "/bar");
        configureEnvVariable(server, configMap);

        server.checkpointRestore();
        assertWebAppStarts("/foo");
        assertWebAppStarts("/bar");

        assertDocumentPath("/foo");
        assertUiPath("/bar");
        assertMissing(DEFAULT_DOC_PATH);
        assertMissing(DEFAULT_UI_PATH);
    }

    /**
     * Assert that a CWWKT0016I: Web application available message is seen for a web app with the given path
     *
     * @param path the path to expect
     */
    private void assertWebAppStarts(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (!path.endsWith("/")) {
            path = path + "/";
        }

        // E.g. CWWKT0016I: Web application available (default_host):
        // http://1.2.3.4:8010/bar/
        assertNotNull("Web application available message not found for " + path,
                      server.waitForStringInLog("CWWKT0016I:.*:\\d+" + path + "$"));
    }

    /**
     * Assert that the OpenAPI UI is being served from the given path
     *
     * @param path the path
     * @throws Exception
     */
    private void assertUiPath(String path) throws Exception {
        // Check that we get something back
        String uiHTML = new OpenAPIConnection(server, path).download();
        // Check that it appears to be the UI HTML
        assertThat(uiHTML, containsString("oauth2RedirectUrl: SwaggerUI.getMpOAuth2Url()"));
    }

    /**
     * Assert that the OpenAPI document is being served from the given path
     *
     * @param path the path
     * @throws Exception
     */
    private void assertDocumentPath(String path) throws Exception {
        // Check that it parses as a model and contains the expected path from the test
        // app
        OpenAPI model = new OpenAPIConnection(server, path).downloadModel();
        assertThat(model.getPaths().toString(), containsString("/configTestPath"));
    }

    /**
     * Assert that nothing is found at the given path (request returns 404)
     *
     * @param path the path
     * @throws Exception
     */
    private void assertMissing(String path) throws Exception {
        OpenAPIConnection connection = new OpenAPIConnection(server, path);
        connection.expectedResponseCode(404);
        connection.download();
    }

}
