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
import io.openliberty.mcp.internal.fat.security.AsyncAdminsRoleAllowedTests;
import io.openliberty.mcp.internal.fat.security.AsyncDenyAllTests;
import io.openliberty.mcp.internal.fat.security.AsyncNoClassAnnotationTests;
import io.openliberty.mcp.internal.fat.security.AsyncPermitAllTests;
import io.openliberty.mcp.internal.fat.tool.AsyncToolCancellationTest;

/**
 * Suite that owns the lifecycle of the {@code mcp-server-async-auth} Liberty server.
 * The server is started once before all tests in this suite and stopped once
 * after all tests complete.
 *
 * <p>Test classes in this suite must NOT use {@code @Server} injection — doing
 * so would cause FATRunner to stop the shared server after each test class via
 * {@code tidyAllKnownServers}. Instead, each test class references
 * {@link McpAsyncAuthServerSuite#server} directly.
 *
 * <p>Test classes must call {@code server.setMarkToEndOfLog()} as the very first
 * line of {@code @BeforeClass} to isolate their log searches from earlier tests.
 *
 * <p>Test classes should use {@link #deployWithConfiguration} and
 * {@link #undeployWithConfiguration} to deploy WARs that require an
 * {@code <application>} entry in server.xml.
 */
@RunWith(Suite.class)
@SuiteClasses({
    AsyncPermitAllTests.class,
    AsyncDenyAllTests.class,
    AsyncNoClassAnnotationTests.class,
    AsyncAdminsRoleAllowedTests.class,
    AsyncToolCancellationTest.class,
})
public class McpAsyncAuthServerSuite {

    /**
     * Assigned in {@code before()} so that {@link LibertyServerFactory#getLibertyServer}
     * runs again on every EE repeat.
     */
    public static LibertyServer server;

    @ClassRule
    public static ExternalResource serverLifecycle = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            server = LibertyServerFactory.getLibertyServer("mcp-server-async-auth");
            server.startServer();
            server.waitForLTPAConfigReady();
        }

        @Override
        protected void after() {
            try {
                // AsyncToolCancellationTest deliberately cancels operations that throw OperationCancelledException
                server.stopServer("OperationCancelledException");
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

    /**
     * Delegates to {@link McpDeployHelper#undeployDropinApp(LibertyServer, String)}.
     *
     * @see McpDeployHelper#undeployDropinApp(LibertyServer, String)
     */
    public static void undeployDropinApp(String appName) throws Exception {
        McpDeployHelper.undeployDropinApp(server, appName);
    }
}
