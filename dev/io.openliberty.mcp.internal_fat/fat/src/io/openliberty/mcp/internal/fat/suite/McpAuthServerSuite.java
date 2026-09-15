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
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
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
 * {@link McpAuthServerSuite#server} directly.
 *
 * <p>Test classes must call {@code server.setMarkToEndOfLog()} as the very first
 * line of {@code @BeforeClass} to isolate their log searches from earlier tests.
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

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("mcp-server-auth");

    @ClassRule
    public static ExternalResource serverLifecycle = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            server.startServer();
            server.waitForLTPAConfigReady();
        }

        @Override
        protected void after() {
            try {
                server.stopServer(
                    // AuthCancellationTest
                    "CWMCM0010E");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };
}
