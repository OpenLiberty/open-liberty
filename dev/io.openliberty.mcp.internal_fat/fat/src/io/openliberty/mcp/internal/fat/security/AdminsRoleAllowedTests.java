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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.suite.McpAuthServerSuite;
import io.openliberty.mcp.internal.fat.tool.securityApps.AdminsRoleTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class AdminsRoleAllowedTests extends AbstractRolesAllowed {

    // Do NOT copy McpAuthServerSuite.server into a local static field — it would capture null
    // because suite fields are assigned after static initializers run in the test class.
    Logger logger = Logger.getLogger(AdminsRoleAllowedTests.class.getName());

    @Rule
    public McpClient client = new McpClient(McpAuthServerSuite.server, "/adminsRoleTools");

    /** {@inheritDoc} */
    @Override
    McpClient getClient() {
        return client;
    }

    @BeforeClass
    public static void setup() throws Exception {
        McpAuthServerSuite.server.setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, "adminsRoleTools.war").addClass(AdminsRoleTools.class);
        ShrinkHelper.exportDropinAppToServer(McpAuthServerSuite.server, war, SERVER_ONLY);
        assertNotNull(McpAuthServerSuite.server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        McpAuthServerSuite.server.setMarkToEndOfLog();
        McpAuthServerSuite.server.deleteFileFromLibertyServerRoot("dropins/adminsRoleTools.war");
        McpAuthServerSuite.server.removeInstalledAppForValidation("adminsRoleTools");
    }
}