/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.security;

import org.junit.Test;

import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.security.AuthHelper.ExpectedTestResult;
import io.openliberty.mcp.internal.fat.security.AuthHelper.Scenario;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
public abstract class AbstractNoClassAnnotation extends FATServletClient {

    abstract McpClient getClient();

    @Test
    public void testNoClassAnnotation_echoPermitAll() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.PASS, getClient());
    }

    @Test
    public void testNoClassAnnotation_echoAdminAllowed() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, getClient());
    }

    @Test
    public void testNoClassAnnotation_echoTestUserAllowed() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, getClient());
    }

    @Test
    public void testNoClassAnnotation_echoTwoRolesAllowed() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, getClient());
    }

    @Test
    public void testNoClassAnnotation_echoDenyAll() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, getClient());
    }

    public void testNoClassAnnotation_echoRoleDoesNotExist() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.FAIL, getClient());
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.FAIL, getClient());
    }

    public void testNoClassAnnotation_echoNoSecurityAnnotationExists() throws Exception {
        AuthHelper.test(Scenario.NO_AUTHENTICATION, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.ADMIN_PASS_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.TESTUSER_PASS_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.ADMIN_FAIL_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.TESTUSER_FAIL_LOGIN, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.UNKNOWN_USER, ExpectedTestResult.PASS, getClient());
        AuthHelper.test(Scenario.UNKNOWN_ROLE, ExpectedTestResult.PASS, getClient());
    }
}