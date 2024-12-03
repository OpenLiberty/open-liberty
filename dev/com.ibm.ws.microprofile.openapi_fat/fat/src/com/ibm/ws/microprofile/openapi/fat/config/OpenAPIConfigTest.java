/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
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
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.microprofile.openapi.fat.FATSuite;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for the mpOpenAPI config element
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OpenAPIConfigTest {

    private static final String SERVER_NAME = "OpenAPIConfigServer";

    private static final String DEFAULT_DOC_PATH = "/openapi";
    private static final String DEFAULT_UI_PATH = DEFAULT_DOC_PATH + "/ui";

    private static String APP_NAME = "mpOpenAPIConfigTest";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static ConfigServerTestHelper helper;

    /**
     * Limited repeats because this test is slow because it involves lots of server restarts.
     * <p>
     * This should be safe because the code is common across all versions of mpOpenAPI.
     */
    @ClassRule
    public static RepeatTests r = FATSuite.repeatLimited(SERVER_NAME);

    @BeforeClass
    public static void init() {
        helper = new ConfigServerTestHelper(server);
    }

    @Before
    public void setup() throws Exception {
        // Test application startup is only checked in one case where is expected to
        // fail to start
        LibertyServer.setValidateApps(false);
        // Deploy test app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "mpOpenAPIConfigTest.war")
            .addClass(OpenAPIConfigTestResource.class);
        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
    }

    @After
    public void teardown() throws Exception {
        LibertyServer.setValidateApps(true);
        server.stopServer(
            "CWWKO1670E", // Expected
            "CWWKO1671E", // Expected
            "CWWKO1672E", // Expected
            "CWWKO1675E", // Expected
            "CWWKO1676E", // Expected
            "CWWKO1677E", // Expected
            "SRVE0164E", // Expected
            "CWWKZ0002E", // Expected
            "CWWKZ0202E" // Expected
        );
    }

    @Test
    public void testConfigureDocumentEndpoint() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        server.startServer(false);

        helper.assertWebAppStarts("/foo");
        helper.assertWebAppStarts("/foo/ui");

        helper.assertDocumentPath("/foo");
        helper.assertUiPath("/foo/ui");
        helper.assertMissing(DEFAULT_DOC_PATH);
        helper.assertMissing(DEFAULT_UI_PATH);
    }

    @Test
    public void testConfigureUiEndpoint() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("bar");
        server.updateServerConfiguration(config);

        server.startServer(false);

        helper.assertWebAppStarts(DEFAULT_DOC_PATH);
        helper.assertWebAppStarts("/bar");

        helper.assertDocumentPath(DEFAULT_DOC_PATH);
        helper.assertUiPath("/bar");
        helper.assertMissing(DEFAULT_UI_PATH);
    }

    @Test
    public void testConflictingPaths() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath(DEFAULT_DOC_PATH);
        server.updateServerConfiguration(config);

        server.startServer(false);

        // check conflict with default Doc Path
        helper.assertWebAppStarts(DEFAULT_DOC_PATH);
        assertNotNull("UI Web Appplication is not available at /openapi/",
            server.waitForStringInLog("CWWKO1672E")); // check that error indicating that Doc endpoint conflict is
                                                      // thrown
        helper.assertWebAppStarts(DEFAULT_UI_PATH);

        helper.assertDocumentPath(DEFAULT_DOC_PATH);
        helper.assertUiPath(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();

        // change both Doc and UI paths to be the same
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath("/foo");
        server.updateServerConfiguration(config);

        assertNotNull("UI Web Appplication is not available at /foo/",
            server.waitForStringInLogUsingMark("CWWKO1672E")); // check that error indicating that conflict is thrown

        helper.assertWebAppStarts("/foo/");
        helper.assertWebAppStarts("/foo/ui/");

        helper.assertDocumentPath("/foo");
        helper.assertUiPath("/foo/ui");
    }

    @Test
    public void testInvalidPaths() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/%4e");
        config.getMpOpenAPIElement().setUiPath("/foo?bar");
        server.updateServerConfiguration(config);

        server.startServer(false);

        assertNotNull("Document Web Appplication path contains invalid characters",
            server.waitForStringInLog("CWWKO1676E")); // check that error indicating that conflict is thrown
        assertNotNull("Document Web Appplication path is invalid",
            server.waitForStringInLog("CWWKO1671E")); // check that error indicating that conflict is thrown
        assertNotNull("UI Web Appplication path contains invalid characters",
            server.waitForStringInLog("CWWKO1675E")); // check that error indicates invalid characters is logged
        assertNotNull("UI Web Appplication path is invalid",
            server.waitForStringInLog("CWWKO1670E")); // check that error indicating that a failure has occurred

        // Check paths revert to defaults
        helper.assertWebAppStarts(DEFAULT_DOC_PATH);
        helper.assertWebAppStarts(DEFAULT_UI_PATH);

        helper.assertDocumentPath(DEFAULT_DOC_PATH);
        helper.assertUiPath(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();

        config.getMpOpenAPIElement().setDocPath("/foo/./bar");
        config.getMpOpenAPIElement().setUiPath("/../bar/foo");
        server.updateServerConfiguration(config);

        assertNotNull("UI Web Appplication path is invalid",
            server.waitForStringInLog("CWWKO1670E")); // check that error indicates invalid characters is logged
        assertNotNull("Document Web Appplication path is invalid",
            server.waitForStringInLog("CWWKO1671E")); // check that error indicates invalid characters is logged

        // both Web Apps will return the same error code for
        assertNotNull("Web Appplication path contains invalid segments",
            server.waitForStringInLog("CWWKO1677E")); // check that error indicating that a failure has occurred has
                                                      // been thrown

        assertNull("Web apps not restarted when config set to invalid values ",
            server.waitForStringInLogUsingMark("CWWKT0016I", 1000));

        helper.assertDocumentPath(DEFAULT_DOC_PATH);
        helper.assertUiPath(DEFAULT_UI_PATH);
    }

    @Test
    @AllowedFFDC
    public void testApplicationPathConfict() throws Exception {
        // Test if initial config has a conflict - as Test app starts last, expect it to
        // fail to start
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/mpOpenAPIConfigTest");
        server.updateServerConfiguration(config);

        server.startServer(false);
        // Web Application OpenAPIUI uses the context root /mpOpenAPIConfigTest/*, which
        // is already in use by Web Application mpOpenAPIConfigTest. Web Application
        // OpenAPIUI will not be loaded.
        assertNotNull("Web Application fails to start due to context path conflict with OPENAPIUI bundle",
            server.waitForStringInLog("SRVE0164E.*OpenAPIUI"));

        helper.assertWebAppStarts(DEFAULT_DOC_PATH);
        helper.assertWebAppStarts("/mpOpenAPIConfigTest");

        helper.assertDocumentPath(DEFAULT_DOC_PATH);

        server.stopServer("SRVE0164E", "CWWKZ0002E");

        // Test if on configuration change such that OpenAPI
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        server.startServer(false);

        helper.assertWebAppStarts(DEFAULT_DOC_PATH);
        helper.assertWebAppStarts(DEFAULT_UI_PATH);
        helper.assertDocumentPath(DEFAULT_DOC_PATH);
        helper.assertUiPath(DEFAULT_UI_PATH);

        // modify UI path to conflict with the running test application
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/mpOpenAPIConfigTest");
        server.updateServerConfiguration(config);

        // Unable to install bundle com.ibm.ws.microprofile.openapi.ui_* with context
        // root /mpOpenAPIConfigTest into the web container.
        assertNotNull("OpenAPI UI bundle fails to start due to context root conflict",
            server.waitForStringInLog("CWWKZ0202E.*openapi.ui"));

        helper.assertMissing("/mpOpenAPIConfigTest");
    }

    @Test
    public void testCustomizedEndpointProxyRefererHeader() throws Exception {
        // Defaults are tested in the ProxySupportTest.class
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath("/bar");
        server.updateServerConfiguration(config);

        server.startServer(false);

        // Esnure that OpenAPI endpoints ara available where they say they are.
        helper.assertWebAppStarts("/foo");
        helper.assertWebAppStarts("/bar");
        helper.assertDocumentPath("/foo");
        helper.assertUiPath("/bar");

        // Test changed path with various referer headers

        // Default HTTP protocol port with different host and matching docPath returns
        // single server entry
        String model = new OpenAPIConnection(server, "/foo").header("Referer", "http://testurl1/foo").download();
        assertThat(helper.getServerUrls(model), Matchers.hasSize(1));
        // check that the server hostname has changed to supplied value
        assertThat("Check that the servers entry use the host from the referer header",
            helper.getServerUrls(model).get(0), Matchers.containsString("http://testurl1/" + APP_NAME));

        // Default HTTPS protocol port with matching uiPath returns single server entry
        model = new OpenAPIConnection(server, "/foo").header("Referer", "https://testurl2/bar").download();
        assertThat(helper.getServerUrls(model), Matchers.hasSize(1));
        ;
        // check that the host name has changed and has maintained HTTPS protocol
        assertThat("Check that the servers entry use the host from the referer header",
            helper.getServerUrls(model).get(0),
            Matchers.containsString("https://testurl2/" + APP_NAME));

        // If the referer path does not match either UI or Doc endpoints that the
        // original hostname is used when config is not default
        model = new OpenAPIConnection(server, "/foo")
            .header("Referer", "http://testurl3:" + server.getHttpDefaultPort() + "/random/").download();
        System.out.println(model.toString());
        // Path mismatch, should revert to server host and server http port
        // Only a single server should be returned as HTTPS is disabled
        assertThat(helper.getServerUrls(model), Matchers.hasSize(1));
        ;
        // Server in String should correspond to the Request URL
        assertThat("Check host reverts to the requestUrl host", helper.getServerUrls(model).get(0),
            Matchers
                .containsString("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME));

        // Path does not match either doc or ui paths, but does end in `/ui`, so server
        // entries should revert to default host
        model = new OpenAPIConnection(server, "/foo").header("Referer", "http://testurl4/random/ui").download();
        System.out.println(model.toString());
        // Only a single server should be returned as HTTPS is disabled
        assertThat(helper.getServerUrls(model), Matchers.hasSize(1));
        // Server in should correspond to the Request URL
        assertThat("Check host reverts to the requestUrl host", helper.getServerUrls(model).get(0),
            Matchers
                .containsString("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME));
    }

}
