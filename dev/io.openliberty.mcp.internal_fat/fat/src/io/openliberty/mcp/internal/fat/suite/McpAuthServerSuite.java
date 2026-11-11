/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.suite;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.mcp.internal.fat.security.AdminsRoleAllowedTests;
import io.openliberty.mcp.internal.fat.security.DenyAllTests;
import io.openliberty.mcp.internal.fat.security.NoClassAnnotationTests;
import io.openliberty.mcp.internal.fat.security.PermitAllTests;
import io.openliberty.mcp.internal.fat.tool.AuthCancellationTest;

/**
 * Suite that owns the lifecycle of the {@code mcp-server-auth} Liberty server.
 * The server is started once before all tests in this suite and stopped once
 * after all tests complete.
 *
 * <p>Test classes in this suite must NOT use {@code @Server} injection — doing
 * so would cause FATRunner to stop the shared server after each test class via
 * {@code tidyAllKnownServers}. Instead, each test class references
 * {@link McpAuthServerSuite#serverLifecycle} directly.
 *
 * <p>Test classes must use {@code serverLifecyclegetServer().setMarkToEndOfLog()}
 * to isolate their log searches from earlier tests.
 *
 * <p>Test classes should use {@link ServerLifecycleRule#deployWithConfiguration} and
 * {@link ServerLifecycleRule#undeployWithConfiguration} to deploy WARs that require an
 * {@code <application>} entry in server.xml.
 */
@RunWith(Suite.class)
@SuiteClasses({
                PermitAllTests.class,
                DenyAllTests.class,
                NoClassAnnotationTests.class,
                AdminsRoleAllowedTests.class,
                AuthCancellationTest.class,
})
public class McpAuthServerSuite {

    @ClassRule
    public static ServerLifecycleRule serverLifecycle = new ServerLifecycleRule("mcp-server-auth");
}
