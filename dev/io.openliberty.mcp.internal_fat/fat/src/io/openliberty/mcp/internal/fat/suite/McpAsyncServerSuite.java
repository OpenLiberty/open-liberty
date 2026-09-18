/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 * <p>Each test class manages its own WAR deployment in {@code @BeforeClass} /
 * {@code @AfterClass}. The suite does NOT pre-deploy any applications.
 * Note that {@code mcp-server-async/server.xml} only declares
 * {@code asyncToolsTestShortTimeout.war} (which needs a custom MCP timeout); all
 * other WARs use dropins and require no server.xml declaration.
 *
 * <p>Test classes must call {@code server.setMarkToEndOfLog()} as the very first
 * line of {@code @BeforeClass} to isolate their log searches from earlier tests.
 *
 * <p><b>Important:</b> test classes must reference {@code McpAsyncServerSuite.server}
 * directly — never copy it into a static field. The suite's {@code @ClassRule} assigns
 * {@code server} inside {@code before()}, which runs <em>after</em> test-class static
 * initializers execute, so a static copy would always capture {@code null}.
 */
@RunWith(Suite.class)
@SuiteClasses({
                AsyncToolsErrorHandlingTest.class,
                AsyncToolCallEventTraceTest.class,
                AsyncToolLifecycleTest.class,
                AsyncToolsTest.class,
})
public class McpAsyncServerSuite {

    public static LibertyServer server;

    @ClassRule
    public static ExternalResource serverLifecycle = new ServerLifecycle();

    static class ServerLifecycle extends ExternalResource {
        @Override
        protected void before() throws Throwable {
            // getLibertyServer is called here rather than in a static initializer so
            // that it runs on every repeat.  Between repeats the JakartaEEAction deletes
            // the server root; calling getLibertyServer again re-copies the server files
            // from the autoFVT source directory before the next repeat starts the server.
            server = LibertyServerFactory.getLibertyServer("mcp-server-async");
            server.startServer();
        }

        @Override
        protected void after() {
            try {
                server.stopServer(
                    // AsyncToolsErrorHandlingTest
                    "CWMCM0010E",
                    // asyncToolsTestShortTimeout.war is declared in server.xml but deployed by
                    // AsyncToolsTest after startup, so Liberty warns at startup (CWWKZ0014W) and
                    // at shutdown when still configured but already removed (CWWKZ0059E)
                    "CWWKZ0014W",
                    "CWWKZ0059E",
                    // AsyncToolsTest / AsyncToolCallEventTraceTest / AsyncToolLifecycleTest
                    "Method call caused runtime exception. This is expected if the input was 'throw error'");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
