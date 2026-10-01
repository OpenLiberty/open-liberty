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

import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.config.Application;

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
 * {@code @AfterClass} using {@link #deployWithConfiguration} or dropins.
 * The suite does NOT pre-deploy any applications — {@code mcp-server-async/server.xml}
 * contains no {@code <application>} declarations.
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
                // CWWKZ0010E is expected: AsyncToolsErrorHandlingTest deliberately
                // invokes tools that throw exceptions to verify error handling behaviour.
                server.stopServer("CWMCM0010E");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    /**
     * Delegates to {@link McpDeployHelper#deployWithConfiguration}.
     *
     * @see McpDeployHelper#deployWithConfiguration(LibertyServer, WebArchive, Consumer)
     */
    public static void deployWithConfiguration(WebArchive war,
                                               Consumer<Application> configurator) throws Exception {
        McpDeployHelper.deployWithConfiguration(server, war, configurator);
    }

    /**
     * Delegates to {@link McpDeployHelper#undeployWithConfiguration(LibertyServer, String)}.
     *
     * @see McpDeployHelper#undeployWithConfiguration(LibertyServer, String)
     */
    public static void undeployWithConfiguration(String appName) throws Exception {
        McpDeployHelper.undeployWithConfiguration(server, appName);
    }

    /**
     * Delegates to {@link McpDeployHelper#undeployWithConfiguration(LibertyServer, WebArchive)}.
     *
     * @see McpDeployHelper#undeployWithConfiguration(LibertyServer, WebArchive)
     */
    public static void undeployWithConfiguration(WebArchive war) throws Exception {
        McpDeployHelper.undeployWithConfiguration(server, war);
    }
}
