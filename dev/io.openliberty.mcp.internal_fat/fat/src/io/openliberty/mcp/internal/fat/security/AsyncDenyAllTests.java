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

import static io.openliberty.mcp.internal.fat.suite.McpAsyncAuthServerSuite.serverLifecycle;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.security.AuthHelper.ExpectedTestResult;
import io.openliberty.mcp.internal.fat.security.AuthHelper.Scenario;
import io.openliberty.mcp.internal.fat.tool.securityApps.AsyncDenyAllTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class AsyncDenyAllTests extends FATServletClient {

    private static final String APP_NAME = "asyncDenyAllTools";

    // Do NOT copy McpAsyncAuthServerSuite.server into a local static field — it would capture null
    // because suite fields are assigned after static initializers run in the test class.
    Logger logger = Logger.getLogger(AsyncDenyAllTests.class.getName());

    @Rule
    public McpClient client = new McpClient(serverLifecycle.getServer(), "/" + APP_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addClass(AsyncDenyAllTools.class);
        serverLifecycle.deployWithConfiguration(war, app -> {
            // no extra <mcp> config needed for this app
        });
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployWithConfiguration(APP_NAME);
    }

    @Test
    public void testDenyAllAsyncClass_echoPermitAll() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.PASS, client);
    }

    @Test
    public void testDenyAllAsyncClass_echoAdminAllowed() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL_401, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL_401, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL_401, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL_401, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL_403, client);
    }

    @Test
    public void testDenyAllAsyncClass_echoNoSecurityAnnotationExists() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL_403, client);
    }

    @Test
    public void testDenyAllAsyncClass_echoDenyAll() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL_403, client);
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL_403, client);
    }
}
