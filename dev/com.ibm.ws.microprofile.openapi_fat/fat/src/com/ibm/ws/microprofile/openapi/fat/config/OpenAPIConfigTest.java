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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
@RunWith(FATRunner.class)
public class OpenAPIConfigTest {

    private static final String SERVER_NAME = "OpenAPIConfigServer";

    private static String DEFAULT_DOC_PATH = "/openapi";
    private static String DEFAULT_UI_PATH = DEFAULT_DOC_PATH + "/ui";

    private static String APP_NAME = "mpOpenAPIConfigTest";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @Before
    public void setup() throws Exception {
        // Test application startup is only checked in one case where is expected to
        // fail to start
        server.setValidateApps(false);
        // Deploy test app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "mpOpenAPIConfigTest.war")
            .addClass(OpenAPIConfigTestResource.class);
        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
    }

    @After
    public void teardown() throws Exception {
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

    @Mode(TestMode.FULL)
    @Test
    public void testConfigureDocumentEndpoint() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        server.startServer(false);

        assertWebAppStarts("/foo");
        assertWebAppStarts("/foo/ui");

        assertDocumentPath("/foo");
        assertUiPath("/foo/ui");
        assertMissing(DEFAULT_DOC_PATH);
        assertMissing(DEFAULT_UI_PATH);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testConfigureUiEndpoint() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("bar");
        server.updateServerConfiguration(config);

        server.startServer(false);

        assertWebAppStarts(DEFAULT_DOC_PATH);
        assertWebAppStarts("/bar");

        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath("/bar");
        assertMissing(DEFAULT_UI_PATH);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testConflictingPaths() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath(DEFAULT_DOC_PATH);
        server.updateServerConfiguration(config);

        server.startServer(false);

        // check conflict with default Doc Path
        assertWebAppStarts(DEFAULT_DOC_PATH);
        assertNotNull("UI Web Appplication is not available at /openapi/",
            server.waitForStringInLog("CWWKO1672E")); // check that error indicating that Doc endpoint conflict is
                                                      // thrown
        assertWebAppStarts(DEFAULT_UI_PATH);

        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();

        // change both Doc and UI paths to be the same
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath("/foo");
        server.updateServerConfiguration(config);

        assertNotNull("UI Web Appplication is not available at /foo/",
            server.waitForStringInLogUsingMark("CWWKO1672E")); // check that error indicating that conflict is thrown

        assertWebAppStarts("/foo/");
        assertWebAppStarts("/foo/ui/");

        assertDocumentPath("/foo");
        assertUiPath("/foo/ui");
    }

    @Mode(TestMode.FULL)
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
        assertWebAppStarts(DEFAULT_DOC_PATH);
        assertWebAppStarts(DEFAULT_UI_PATH);

        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath(DEFAULT_UI_PATH);

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

        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath(DEFAULT_UI_PATH);
    }

    @Mode(TestMode.FULL)
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

        assertWebAppStarts(DEFAULT_DOC_PATH);
        assertWebAppStarts("/mpOpenAPIConfigTest");

        assertDocumentPath(DEFAULT_DOC_PATH);

        server.stopServer("SRVE0164E", "CWWKZ0002E");

        // Test if on configuration change such that OpenAPI
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        server.startServer(false);

        assertWebAppStarts(DEFAULT_DOC_PATH);
        assertWebAppStarts(DEFAULT_UI_PATH);
        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath(DEFAULT_UI_PATH);

        // modify UI path to conflict with the running test application
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/mpOpenAPIConfigTest");
        server.updateServerConfiguration(config);

        // Unable to install bundle com.ibm.ws.microprofile.openapi.ui_* with context
        // root /mpOpenAPIConfigTest into the web container.
        assertNotNull("OpenAPI UI bundle fails to start due to context root conflict",
            server.waitForStringInLog("CWWKZ0202E.*openapi.ui"));

        assertMissing("/mpOpenAPIConfigTest");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testCustomizedEndpointProxyRefererHeader() throws Exception {
        // Defaults are tested in the ProxySupportTest.class
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath("/bar");
        server.updateServerConfiguration(config);

        server.startServer(false);

        // Esnure that OpenAPI endpoints ara available where they say they are.
        assertWebAppStarts("/foo");
        assertWebAppStarts("/bar");
        assertDocumentPath("/foo");
        assertUiPath("/bar");

        // Test changed path with various referer headers

        // Default HTTP protocol port with different host and matching docPath returns
        // single server entry
        OpenAPI model = new OpenAPIConnection(server, "/foo").header("Referer", "http://testurl1/foo").downloadModel();
        assertThat(model.getServers(), Matchers.hasSize(1));
        // check that the server hostname has changed to supplied value
        assertThat("Check that the servers entry use the host from the referer header",
            model.getServers().get(0).getUrl(), Matchers.containsString("http://testurl1/" + APP_NAME));

        // Default HTTPS protocol port with matching uiPath returns single server entry
        model = new OpenAPIConnection(server, "/foo").header("Referer", "https://testurl2/bar").downloadModel();
        assertThat(model.getServers(), Matchers.hasSize(1));
        ;
        // check that the host name has changed and has maintained HTTPS protocol
        assertThat("Check that the servers entry use the host from the referer header",
            model.getServers().get(0).getUrl(),
            Matchers.containsString("https://testurl2/" + APP_NAME));

        // If the referer path does not match either UI or Doc endpoints that the
        // original hostname is used when config is not default
        model = new OpenAPIConnection(server, "/foo")
            .header("Referer", "http://testurl3:" + server.getHttpDefaultPort() + "/random/").downloadModel();
        System.out.println(model.toString());
        // Path mismatch, should revert to server host and server http port
        // Only a single server should be returned as HTTPS is disabled
        assertThat(model.getServers(), Matchers.hasSize(1));
        ;
        // Server in String should correspond to the Request URL
        assertThat("Check host reverts to the requestUrl host", model.getServers().get(0).getUrl(),
            Matchers
                .containsString("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME));

        // Path does not match either doc or ui paths, but does end in `/ui`, so server
        // entries should revert to default host
        model = new OpenAPIConnection(server, "/foo").header("Referer", "http://testurl4/random/ui").downloadModel();
        System.out.println(model.toString());
        // Only a single server should be returned as HTTPS is disabled
        assertThat(model.getServers(), Matchers.hasSize(1));
        // Server in should correspond to the Request URL
        assertThat("Check host reverts to the requestUrl host", model.getServers().get(0).getUrl(),
            Matchers
                .containsString("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME));
    }

    @Test
    public void testConfigureDynamicUpdate() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        server.startServer(false);

        assertWebAppStarts(DEFAULT_DOC_PATH);
        assertWebAppStarts(DEFAULT_UI_PATH);

        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath("/bar");
        server.updateServerConfiguration(config);

        assertWebAppStarts("/foo");
        assertWebAppStarts("/bar");

        assertDocumentPath("/foo");
        assertUiPath("/bar");
        assertMissing(DEFAULT_DOC_PATH);
        assertMissing(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        assertWebAppStarts("/foo/ui");
        assertNoWebAppStart("/foo");

        assertDocumentPath("/foo");
        assertUiPath("/foo/ui");
        assertMissing(DEFAULT_DOC_PATH);
        assertMissing(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/foo/ui");
        server.updateServerConfiguration(config);

        assertWebAppStarts(DEFAULT_DOC_PATH);
        assertNoWebAppStart("/foo/ui");

        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath("/foo/ui");
        assertMissing(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/baz");
        server.updateServerConfiguration(config);

        assertWebAppStarts("/baz");
        assertNoWebAppStart(DEFAULT_DOC_PATH);

        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath("/baz");
        assertMissing(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/baz");
        server.updateServerConfiguration(config);

        // Check there's no web app start message when the config does not change
        assertNull("Web app restarted when config was not changed",
            server.waitForStringInLogUsingMark("CWWKT0016I", 3000));

        assertDocumentPath(DEFAULT_DOC_PATH);
        assertUiPath("/baz");
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
     * Assert that a CWWKT0016I: Web application available message is not seen for a web app with the given path
     *
     * @param path the path to check for
     */
    private void assertNoWebAppStart(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (!path.endsWith("/")) {
            path = path + "/";
        }

        // E.g. CWWKT0016I: Web application available (default_host):
        // http://1.2.3.4:8010/bar/
        assertNull("Unexpected web application available message found for " + path,
            server.waitForStringInLog("CWWKT0016I:.*:\\d+" + path + "$", 1000));
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
