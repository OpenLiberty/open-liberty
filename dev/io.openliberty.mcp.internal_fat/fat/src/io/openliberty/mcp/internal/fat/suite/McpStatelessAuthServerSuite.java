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
import io.openliberty.mcp.internal.fat.security.AdminsRoleAllowedTestsStateless;
import io.openliberty.mcp.internal.fat.security.DenyAllTestsStateless;
import io.openliberty.mcp.internal.fat.security.NoClassAnnotationTestsStateless;
import io.openliberty.mcp.internal.fat.security.PermitAllTestsStateless;

/**
 * Suite that owns the lifecycle of the {@code mcp-stateless-server-auth} Liberty server.
 * The server is started once before all tests in this suite and stopped once
 * after all tests complete.
 *
 * <p>Test classes in this suite must NOT use {@code @Server} injection — doing
 * so would cause FATRunner to stop the shared server after each test class via
 * {@code tidyAllKnownServers}. Instead, each test class references
 * {@link McpStatelessAuthServerSuite#server} directly.
 *
 * <p>Test classes must call {@code server.setMarkToEndOfLog()} as the very first
 * line of {@code @BeforeClass} to isolate their log searches from earlier tests.
 */
@RunWith(Suite.class)
@SuiteClasses({
    PermitAllTestsStateless.class,
    DenyAllTestsStateless.class,
    NoClassAnnotationTestsStateless.class,
    AdminsRoleAllowedTestsStateless.class,
})
public class McpStatelessAuthServerSuite {

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("mcp-stateless-server-auth");

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
                    // The apps are declared in server.xml so that <mcp stateless="true"/> is applied,
                    // but the WARs are deployed/undeployed dynamically by each test class.
                    // CWWKZ0014W fires at server start (WAR not yet present) and
                    // CWWKZ0059E fires at teardown (WAR deleted while app is still configured).
                    "CWWKZ0014W",
                    "CWWKZ0059E");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };
}
