/*******************************************************************************
 Copyright (c) 2025 IBM Corporation and others.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-2.0/
 *
 SPDX-License-Identifier: EPL-2.0
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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.security.AuthHelper.ExpectedTestResult;
import io.openliberty.mcp.internal.fat.security.AuthHelper.Scenario;
import io.openliberty.mcp.internal.fat.tool.securityApps.PermitAllTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class PermitAllTests extends FATServletClient {

    @Server("mcp-server-auth")
    public static LibertyServer server;
    Logger logger = Logger.getLogger(PermitAllTests.class.getName());

    @Rule
    public McpClient client = new McpClient(server, "/permitAllTools");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "permitAllTools.war").addPackage(PermitAllTools.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
        assertNotNull(server.findStringsInLogs("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/toolTest/mcp"
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testPermitAllClass_echoPermitAll() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.PASS, client);
    }

    @Test
    public void testPermitAllClass_echoDenyAll() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, client);
    }

    @Test
    public void testPermitAllClass_echoAdminAllowed() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, client);
    }

    @Test
    public void testPermitAllClass_echoTestUserAllowed() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, client);
    }

    @Test
    public void testPermitAllClass_echoTwoRolesAllowed() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, client);
    }

    @Test
    public void testPermitAllClass_echoNoSecurityAnnotationExists() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.PASS, client);
    }

    @Test
    public void testPermitAllClass_echoRoleDoesNotExist() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, client);
    }
}