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
import io.openliberty.mcp.internal.fat.tool.TelemetryOperationsTest;
import io.openliberty.mcp.internal.fat.tool.TelemetrySessionsTest;

/**
 * Suite that owns the lifecycle of the {@code mcp-server-telemetry} Liberty server.
 * The server is started once before all tests in this suite and stopped once
 * after all tests complete.
 *
 * <p>Test classes in this suite must NOT use {@code @Server} injection — doing
 * so would cause FATRunner to stop the shared server after each test class via
 * {@code tidyAllKnownServers}. Instead, each test class references
 * {@link McpTelemetryServerSuite#server} directly.
 *
 * <p>Test classes must call {@code server.setMarkToEndOfLog()} as the very first
 * line of {@code @BeforeClass} to isolate their log searches from earlier tests.
 */
@RunWith(Suite.class)
@SuiteClasses({
    TelemetryOperationsTest.class,
    TelemetrySessionsTest.class,
})
public class McpTelemetryServerSuite {

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("mcp-server-telemetry");

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
                    // TelemetryOperationsTest
                    "CWMCM0010E",
                    // TelemetrySessionsTest, MpMetricsOperationsTest
                    "CWWKS9113E");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };
}
