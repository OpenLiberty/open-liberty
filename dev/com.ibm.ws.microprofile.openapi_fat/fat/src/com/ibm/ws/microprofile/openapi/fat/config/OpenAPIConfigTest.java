/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat.config;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for the mpOpenAPI config element
 */
@RunWith(FATRunner.class)
public class OpenAPIConfigTest {

    private static final String SERVER_NAME = "OpenAPIConfigServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
        MicroProfileActions.MP60, // mpOpenAPI-3.1, LITE
        MicroProfileActions.MP50, // mpOpenAPI-3.0, FULL
        MicroProfileActions.MP41, // mpOpenAPI-2.0, FULL
        MicroProfileActions.MP33, // mpOpenAPI-1.1, FULL
        MicroProfileActions.MP22);// mpOpenAPI-1.0, FULL

    @Before
    public void setup() throws Exception {
        // Set guards
        server.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true", "-Dopen_api_path_enabled=true"));
        // Deploy test app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "mpOpenAPIConfigTest.war")
            .addClass(OpenAPIConfigTestResource.class);
        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
    }

    @After
    public void teardown() throws Exception {
        server.stopServer();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testConfigureDocumentEndpoint() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        server.startServer();

        assertDocumentPath("/foo");
        assertUiPath("/foo/ui");
        assertMissing("/openapi");
        assertMissing("/openapi/ui");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testConfigureUiEndpoint() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("bar");
        server.updateServerConfiguration(config);

        server.startServer();

        assertDocumentPath("/openapi");
        assertUiPath("/bar");
        assertMissing("/openapi/ui");
    }

    @Test
    public void testConfigureDynamicUpdate() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        server.startServer();

        assertDocumentPath("/openapi");
        assertUiPath("/openapi/ui");

        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath("/bar");
        server.updateServerConfiguration(config);

        assertDocumentPath("/foo");
        assertUiPath("/bar");
        assertMissing("/openapi");
        assertMissing("/openapi/ui");

        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        assertDocumentPath("/foo");
        assertUiPath("/foo/ui");
        assertMissing("/openapi");
        assertMissing("/openapi/ui");

        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/foo/ui");
        server.updateServerConfiguration(config);

        assertDocumentPath("/openapi");
        assertUiPath("/foo/ui");
        assertMissing("/openapi/ui");

        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/baz");
        server.updateServerConfiguration(config);

        assertDocumentPath("/openapi");
        assertUiPath("/baz");
        assertMissing("/openapi/ui");

        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/baz");
        server.updateServerConfiguration(config);

        assertDocumentPath("/openapi");
        assertUiPath("/baz");
        assertMissing("/openapi/ui");
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
        MatcherAssert.assertThat(model.getPaths(), Matchers.hasKey("/configTestPath"));
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
