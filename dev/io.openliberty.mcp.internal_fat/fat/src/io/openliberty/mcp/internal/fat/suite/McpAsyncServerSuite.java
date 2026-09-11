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
import io.openliberty.mcp.internal.fat.lifecycle.tests.AsyncToolLifecycleTest;
import io.openliberty.mcp.internal.fat.tool.AsyncToolCallEventTraceTest;
import io.openliberty.mcp.internal.fat.tool.AsyncToolsErrorHandlingTest;
import io.openliberty.mcp.internal.fat.tool.AsyncToolsTest;

/**
 * Suite that owns the lifecycle of the {@code mcp-server-async} Liberty server.
 * The server is started once before all tests in this suite and stopped once
 * after all tests complete.
 *
 * <p>Test classes in this suite must NOT use {@code @Server} injection — doing
 * so would cause FATRunner to stop the shared server after each test class via
 * {@code tidyAllKnownServers}. Instead, each test class references
 * {@link McpAsyncServerSuite#server} directly.
 *
 * <p>Test classes must call {@code server.setMarkToEndOfLog()} as the very first
 * line of {@code @BeforeClass} to isolate their log searches from earlier tests.
 */
@RunWith(Suite.class)
@SuiteClasses({
    AsyncToolsErrorHandlingTest.class,
    AsyncToolCallEventTraceTest.class,
    AsyncToolLifecycleTest.class,
    AsyncToolsTest.class,
})
public class McpAsyncServerSuite {

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("mcp-server-async");

    @ClassRule
    public static ExternalResource serverLifecycle = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            server.startServer();
        }

        @Override
        protected void after() {
            try {
                server.stopServer(
                    // AsyncToolsErrorHandlingTest
                    "CWMCM0010E",
                    // AsyncToolsTest, AsyncToolCallEventTraceTest, AsyncToolLifecycleTest
                    "Method call caused runtime exception. This is expected if the input was 'throw error'",
                    // asyncToolsTestShortTimeout.war is declared in server.xml but only deployed
                    // during AsyncToolsTest.setup() — not present at server start (CWWKZ0014W)
                    // and deleted before the server stops while still configured (CWWKZ0059E)
                    "CWWKZ0014W",
                    "CWWKZ0059E");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };
}
