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

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.suite.McpAuthServerSuite;
import io.openliberty.mcp.internal.fat.tool.securityApps.DenyAllTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class DenyAllTests extends AbstractDenyAll {

    private static final String APP_NAME = "denyAllTools";

    // Do NOT copy McpAuthServerSuite.server into a local static field — it would capture null
    // because suite fields are assigned after static initializers run in the test class.
    Logger logger = Logger.getLogger(DenyAllTests.class.getName());

    @Rule
    public McpClient client = new McpClient(McpAuthServerSuite.server, "/" + APP_NAME);

    /** {@inheritDoc} */
    @Override
    McpClient getClient() {
        return client;
    }

    @BeforeClass
    public static void setup() throws Exception {
        McpAuthServerSuite.server.setMarkToEndOfLog();

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addClass(DenyAllTools.class);
        McpAuthServerSuite.deployWithConfiguration(war, app -> {
            // no extra <mcp> config needed for this app
        });

        assertNotNull(McpAuthServerSuite.server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + APP_NAME));
    }

    @AfterClass
    public static void teardown() throws Exception {
        McpAuthServerSuite.server.setMarkToEndOfLog();
        McpAuthServerSuite.undeployWithConfiguration(APP_NAME);
    }

}