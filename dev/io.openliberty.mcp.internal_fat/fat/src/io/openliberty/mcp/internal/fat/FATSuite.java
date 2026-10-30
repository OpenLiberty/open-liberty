/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat;

import static componenttest.rules.repeater.EERepeatActions.EE10;
import static componenttest.rules.repeater.EERepeatActions.EE11;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.mcp.internal.fat.conformance.tests.ConformanceTests;
import io.openliberty.mcp.internal.fat.oidc.tests.AuthorizationFlowTests;
import io.openliberty.mcp.internal.fat.oidc.tests.OidcTests;
import io.openliberty.mcp.internal.fat.protocol.ProtocolVersionConfigBetaTest;
import io.openliberty.mcp.internal.fat.serverinfo.CustomServerInfoTest;
import io.openliberty.mcp.internal.fat.statelessMode.StatelessConfigChangeOnRestoreTest;
import io.openliberty.mcp.internal.fat.statelessMode.StatelessModeTest;
import io.openliberty.mcp.internal.fat.suite.McpAsyncAuthServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpAsyncServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpAuthServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpMainServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpMonitorServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpStatelessAuthServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpTelemetryServerSuite;
import io.openliberty.mcp.internal.fat.timeout.ConfigurableAsyncTimeoutTest;
import io.openliberty.mcp.internal.fat.timeout.InvalidAsyncTimeoutTest;
import io.openliberty.mcp.internal.fat.tool.ConfigurableMcpPathTest;
import io.openliberty.mcp.internal.fat.tool.ConfigurableSessionTelemetryTest;
import io.openliberty.mcp.internal.fat.tool.DeploymentProblemTest;
import io.openliberty.mcp.internal.fat.tool.DualConfigurableMcpPathTest;
import io.openliberty.mcp.internal.fat.tool.DynamicMcpPathUpdateTest;
import io.openliberty.mcp.internal.fat.tool.InvalidSessionTimeoutTest;
import io.openliberty.mcp.internal.fat.tool.NoParamNameTest;
import io.openliberty.mcp.internal.fat.tool.ToolTest;

/**
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                // --- Servers with shared suite lifecycle ---
                McpAsyncServerSuite.class, // mcp-server-async
                McpAuthServerSuite.class, // mcp-server-auth
                McpAsyncAuthServerSuite.class, // mcp-server-async-auth
                McpStatelessAuthServerSuite.class, // mcp-stateless-server-auth
                McpTelemetryServerSuite.class, // mcp-server-telemetry
                McpMonitorServerSuite.class, // mcp-server-monitor-only
                McpMainServerSuite.class,

                // --- Tests on their own dedicated servers ---
                ConfigurableMcpPathTest.class,
                ConfigurableSessionTelemetryTest.class,
                ConfigurableAsyncTimeoutTest.class,
                CustomServerInfoTest.class,
                DualConfigurableMcpPathTest.class,
                DynamicMcpPathUpdateTest.class,
                InvalidAsyncTimeoutTest.class,
                InvalidSessionTimeoutTest.class,
                ProtocolVersionConfigBetaTest.class,
                StatelessConfigChangeOnRestoreTest.class,
                StatelessModeTest.class,
                // These deploy intentionally failing apps on "mcp-server" and start/stop it
                // themselves, so they must run after McpMainServerSuite has stopped it.
                DeploymentProblemTest.class,
                NoParamNameTest.class,
                // ToolTest must be last on "mcp-server": its LITE-mode @ClassRule repeat
                // requires server restarts between runs and must not affect prior tests.
                ToolTest.class,
                // TestContainer Tests
                ConformanceTests.class,
                OidcTests.class,
                AuthorizationFlowTests.class
})

public class FATSuite extends TestContainerSuite {

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(null, /* skipTransformation */ true, EE10, EE11);
}