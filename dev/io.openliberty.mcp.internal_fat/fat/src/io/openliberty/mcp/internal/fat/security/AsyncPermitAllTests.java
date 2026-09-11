/*******************************************************************************
 Copyright (c) 2025, 2026 IBM Corporation and others.
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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.security.AuthHelper.ExpectedTestResult;
import io.openliberty.mcp.internal.fat.security.AuthHelper.Scenario;
import io.openliberty.mcp.internal.fat.suite.McpAsyncAuthServerSuite;
import io.openliberty.mcp.internal.fat.tool.securityApps.AsyncPermitAllTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class AsyncPermitAllTests extends FATServletClient {

    // Server is managed by McpAsyncAuthServerSuite — do NOT add @Server here.
    public static LibertyServer server = McpAsyncAuthServerSuite.server;
    Logger logger = Logger.getLogger(AsyncPermitAllTests.class.getName());

    @Rule
    public McpClient client = new McpClient(server, "/asyncPermitAllTools");

    @BeforeClass
    public static void setup() throws Exception {
        server.setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, "asyncPermitAllTools.war").addClass(AsyncPermitAllTools.class);
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.setMarkToEndOfLog();
        server.deleteFileFromLibertyServerRoot("dropins/asyncPermitAllTools.war");
        server.waitForStringInLog("CWWKZ0009I:.*asyncPermitAllTools");
        server.removeInstalledAppForValidation("asyncPermitAllTools");
    }

    @Test
    public void testPermitAllAsyncClass_echoPermitAll() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.PASS, client);
    }

    @Test
    public void testPermitAllAsyncClass_echoDenyAll() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL_403, client);
    }

    @Test
    public void testPermitAllAsyncClass_echoAdminAllowed() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL_401, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL_401, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL_401, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL_401, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL_403, client);
    }

    @Test
    public void testPermitAllAsyncClass_echoNoSecurityAnnotationExists() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.PASS, client);
    }

}