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

import com.ibm.websphere.simplicity.config.Mcp;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.suite.McpStatelessAuthServerSuite;
import io.openliberty.mcp.internal.fat.tool.securityApps.AdminsRoleTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.McpClient.StateMode;

/**
 *
 */
@RunWith(FATRunner.class)
public class AdminsRoleAllowedTestsStateless extends AbstractRolesAllowed {

    private static final String APP_NAME = "adminsRoleToolsStateless";

    // Do NOT copy McpStatelessAuthServerSuite.server into a local static field — it would capture null
    // because suite fields are assigned after static initializers run in the test class.
    Logger logger = Logger.getLogger(AdminsRoleAllowedTestsStateless.class.getName());

    @Rule
    public McpClient client = new McpClient(McpStatelessAuthServerSuite.server, "/" + APP_NAME, StateMode.STATELESS);

    /** {@inheritDoc} */
    @Override
    McpClient getClient() {
        return client;
    }

    @BeforeClass
    public static void setup() throws Exception {
        McpStatelessAuthServerSuite.server.setMarkToEndOfLog();

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addClass(AdminsRoleTools.class);
        McpStatelessAuthServerSuite.deployWithConfiguration(war, app -> {
            Mcp mcp = new Mcp();
            mcp.setStateless("true");
            app.getMcps().add(mcp);
        });

        assertNotNull(McpStatelessAuthServerSuite.server.waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        McpStatelessAuthServerSuite.server.setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        McpStatelessAuthServerSuite.undeployWithConfiguration(war);
    }
}