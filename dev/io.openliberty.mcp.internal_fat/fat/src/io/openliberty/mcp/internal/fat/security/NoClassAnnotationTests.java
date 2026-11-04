/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.security;

import static io.openliberty.mcp.internal.fat.suite.McpAuthServerSuite.serverLifecycle;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.tool.securityApps.NoClassAnnotationTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class NoClassAnnotationTests extends AbstractNoClassAnnotation {

    private static final String APP_NAME = "noClassAnnotationTools";

    // Server is managed by McpAuthServerSuite — do NOT add @Server or copy the field here.
    Logger logger = Logger.getLogger(NoClassAnnotationTests.class.getName());

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    /** {@inheritDoc} */
    @Override
    McpClient getClient() {
        return client;
    }

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addClass(NoClassAnnotationTools.class);
        serverLifecycle.deployDropinApp(war);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }
}