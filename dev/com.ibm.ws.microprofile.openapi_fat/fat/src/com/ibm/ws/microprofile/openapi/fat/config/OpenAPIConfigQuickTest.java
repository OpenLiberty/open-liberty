/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat.config;

import static org.junit.Assert.assertNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.microprofile.openapi.fat.FATSuite;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * A single dynamic test for the mpOpenAPI config element
 */
@RunWith(FATRunner.class)
public class OpenAPIConfigQuickTest {

    private static final String SERVER_NAME = "OpenAPIConfigServer";

    private static final String DEFAULT_DOC_PATH = "/openapi";
    private static final String DEFAULT_UI_PATH = DEFAULT_DOC_PATH + "/ui";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static ConfigServerTestHelper helper;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        helper = new ConfigServerTestHelper(server);

        // Deploy test app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "mpOpenAPIConfigTest.war")
            .addClass(OpenAPIConfigTestResource.class);
        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        LibertyServer.setValidateApps(true);
        server.stopServer();
    }

    @Test
    public void testConfigureDynamicUpdate() throws Exception {

        helper.assertWebAppStarts(DEFAULT_DOC_PATH);
        helper.assertWebAppStarts(DEFAULT_UI_PATH);

        helper.assertDocumentPath(DEFAULT_DOC_PATH);
        helper.assertUiPath(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        ServerConfiguration config;
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath("/bar");
        server.updateServerConfiguration(config);

        helper.assertWebAppStarts("/foo");
        helper.assertWebAppStarts("/bar");

        helper.assertDocumentPath("/foo");
        helper.assertUiPath("/bar");
        helper.assertMissing(DEFAULT_DOC_PATH);
        helper.assertMissing(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath("/foo");
        config.getMpOpenAPIElement().setUiPath(null);
        server.updateServerConfiguration(config);

        helper.assertWebAppStarts("/foo/ui");
        helper.assertNoWebAppStart("/foo");

        helper.assertDocumentPath("/foo");
        helper.assertUiPath("/foo/ui");
        helper.assertMissing(DEFAULT_DOC_PATH);
        helper.assertMissing(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/foo/ui");
        server.updateServerConfiguration(config);

        helper.assertWebAppStarts(DEFAULT_DOC_PATH);
        helper.assertNoWebAppStart("/foo/ui");

        helper.assertDocumentPath(DEFAULT_DOC_PATH);
        helper.assertUiPath("/foo/ui");
        helper.assertMissing(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/baz");
        server.updateServerConfiguration(config);

        helper.assertWebAppStarts("/baz");
        helper.assertNoWebAppStart(DEFAULT_DOC_PATH);

        helper.assertDocumentPath(DEFAULT_DOC_PATH);
        helper.assertUiPath("/baz");
        helper.assertMissing(DEFAULT_UI_PATH);

        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setDocPath(null);
        config.getMpOpenAPIElement().setUiPath("/baz");
        server.updateServerConfiguration(config);

        // Check there's no web app start message when the config does not change
        assertNull("Web app restarted when config was not changed",
            server.waitForStringInLogUsingMark("CWWKT0016I", 3000));

        helper.assertDocumentPath(DEFAULT_DOC_PATH);
        helper.assertUiPath("/baz");
        helper.assertMissing(DEFAULT_UI_PATH);
    }

}
