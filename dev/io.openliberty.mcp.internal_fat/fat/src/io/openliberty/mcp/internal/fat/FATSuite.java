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
import io.openliberty.mcp.internal.fat.introspector.IntrospectorMultiAppTest;
import io.openliberty.mcp.internal.fat.isolation.MultiAppIsolationTest;
import io.openliberty.mcp.internal.fat.lifecycle.tests.BeanLifecycleTest;
import io.openliberty.mcp.internal.fat.lifecycle.tests.LifecycleTest;
import io.openliberty.mcp.internal.fat.oidc.tests.AuthorizationFlowTests;
import io.openliberty.mcp.internal.fat.oidc.tests.OidcTests;
import io.openliberty.mcp.internal.fat.protocol.HttpTest;
import io.openliberty.mcp.internal.fat.protocol.ProtocolVersionSchemaTest;
import io.openliberty.mcp.internal.fat.protocol.ProtocolVersionTest;
import io.openliberty.mcp.internal.fat.serverinfo.CustomServerInfoTest;
import io.openliberty.mcp.internal.fat.statelessMode.StatefulModeTest;
import io.openliberty.mcp.internal.fat.statelessMode.StatelessConfigChangeOnRestoreTest;
import io.openliberty.mcp.internal.fat.statelessMode.StatelessModeTest;
import io.openliberty.mcp.internal.fat.suite.McpAsyncAuthServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpAsyncServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpAuthServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpMonitorServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpStatelessAuthServerSuite;
import io.openliberty.mcp.internal.fat.suite.McpTelemetryServerSuite;
import io.openliberty.mcp.internal.fat.timeout.ConfigurableAsyncTimeoutTest;
import io.openliberty.mcp.internal.fat.timeout.InvalidAsyncTimeoutTest;
import io.openliberty.mcp.internal.fat.tool.CancellationTest;
import io.openliberty.mcp.internal.fat.tool.ConfigurableMcpPathTest;
import io.openliberty.mcp.internal.fat.tool.ConfigurableSessionTelemetryTest;
import io.openliberty.mcp.internal.fat.tool.DefaultValueTest;
import io.openliberty.mcp.internal.fat.tool.DeploymentProblemTest;
import io.openliberty.mcp.internal.fat.tool.DualConfigurableMcpPathTest;
import io.openliberty.mcp.internal.fat.tool.DynamicMcpPathUpdateTest;
import io.openliberty.mcp.internal.fat.tool.EncoderTest;
import io.openliberty.mcp.internal.fat.tool.ExceptionLoggingTest;
import io.openliberty.mcp.internal.fat.tool.GenericToolTest;
import io.openliberty.mcp.internal.fat.tool.InactiveCdiTest;
import io.openliberty.mcp.internal.fat.tool.LocaleTest;
import io.openliberty.mcp.internal.fat.tool.McpUrlPathTest;
import io.openliberty.mcp.internal.fat.tool.MultiModuleToolTestToolManager;
import io.openliberty.mcp.internal.fat.tool.NoParamNameTest;
import io.openliberty.mcp.internal.fat.tool.NonRequiredArgsToolsTest;
import io.openliberty.mcp.internal.fat.tool.ToolCallEventTraceTest;
import io.openliberty.mcp.internal.fat.tool.ToolErrorHandlingTest;
import io.openliberty.mcp.internal.fat.tool.ToolManagerTest;
import io.openliberty.mcp.internal.fat.tool.ToolTest;
import io.openliberty.mcp.internal.fat.tool.UnsupportedAnnotationWarningTest;

/**
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                // --- Servers with shared suite lifecycle ---
                McpAsyncServerSuite.class,          // mcp-server-async
                McpAuthServerSuite.class,            // mcp-server-auth
                McpAsyncAuthServerSuite.class,       // mcp-server-async-auth
                McpMonitorServerSuite.class,         // mcp-server-monitor-only
                McpTelemetryServerSuite.class,       // mcp-server-telemetry
                McpStatelessAuthServerSuite.class,   // mcp-stateless-server-auth

                // --- Remaining tests (still one server per test class) ---
                BeanLifecycleTest.class,
                CancellationTest.class,
                ConfigurableMcpPathTest.class,
                ConfigurableSessionTelemetryTest.class,
                ConfigurableAsyncTimeoutTest.class,
                CustomServerInfoTest.class,
                DefaultValueTest.class,
                DeploymentProblemTest.class,
                DualConfigurableMcpPathTest.class,
                DynamicMcpPathUpdateTest.class,
                EncoderTest.class,
                ToolCallEventTraceTest.class,
                ExceptionLoggingTest.class,
                HttpTest.class,
                GenericToolTest.class,
                InactiveCdiTest.class,
                IntrospectorMultiAppTest.class,
                InvalidAsyncTimeoutTest.class,
                LocaleTest.class,
                LifecycleTest.class,
                McpUrlPathTest.class,
                MultiAppIsolationTest.class,
                MultiModuleToolTestToolManager.class,
                NonRequiredArgsToolsTest.class,
                NoParamNameTest.class,
                ProtocolVersionTest.class,
                ProtocolVersionSchemaTest.class,
                StatefulModeTest.class,
                StatelessConfigChangeOnRestoreTest.class,
                StatelessModeTest.class,
                TelemetryOperationsTest.class,
                TelemetrySessionsTest.class,
                ToolErrorHandlingTest.class,
                ToolManagerTest.class,
                UnsupportedAnnotationWarningTest.class,
                // Tool test must be last the last test on "mcp-server" because
                // it has special repeats in lite mode which would affect later tests
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
