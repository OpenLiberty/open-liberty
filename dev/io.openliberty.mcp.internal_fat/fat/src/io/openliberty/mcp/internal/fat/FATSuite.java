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
import io.openliberty.mcp.internal.fat.suite.McpAsyncAuthServerSuite;

/**
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
// --- Servers with shared suite lifecycle ---
//                McpAsyncServerSuite.class, // mcp-server-async
//                McpAuthServerSuite.class, // mcp-server-auth
                McpAsyncAuthServerSuite.class, // mcp-server-async-auth
//                McpStatelessAuthServerSuite.class, // mcp-stateless-server-auth

// --- Remaining tests (still one server per test class) ---
//                BeanLifecycleTest.class,
//                CancellationTest.class,
//                ConfigurableMcpPathTest.class,
//                ConfigurableSessionTelemetryTest.class,
//                ConfigurableAsyncTimeoutTest.class,
//                CustomServerInfoTest.class,
//                DefaultValueTest.class,
//                DeploymentProblemTest.class,
//                DualConfigurableMcpPathTest.class,
//                DynamicMcpPathUpdateTest.class,
//                EncoderTest.class,
//                ToolCallEventTraceTest.class,
//                ExceptionLoggingTest.class,
//                HttpTest.class,
//                GenericToolTest.class,
//                InactiveCdiTest.class,
//                IntrospectorMultiAppTest.class,
//                InvalidAsyncTimeoutTest.class,
//                LocaleTest.class,
//                LifecycleTest.class,
//                McpMonitorMXBeanAccessTest.class,
//                McpMonitorTest.class,
//                McpUrlPathTest.class,
//                MultiAppIsolationTest.class,
//                MultiModuleToolTestToolManager.class,
//                NonRequiredArgsToolsTest.class,
//                NoParamNameTest.class,
//                ProtocolVersionTest.class,
//                ProtocolVersionSchemaTest.class,
//                StatefulModeTest.class,
//                StatelessConfigChangeOnRestoreTest.class,
//                StatelessModeTest.class,
//                TelemetryOperationsTest.class,
//                TelemetrySessionsTest.class,
//                ToolErrorHandlingTest.class,
//                ToolManagerTest.class,
//                UnsupportedAnnotationWarningTest.class,
//                // Tool test must be last the last test on "mcp-server" because
//                // it has special repeats in lite mode which would affect later tests
//                ToolTest.class,
//                // TestContainer Tests
//                ConformanceTests.class,
//                OidcTests.class,
//                AuthorizationFlowTests.class
})

public class FATSuite extends TestContainerSuite {

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(null, /* skipTransformation */ true, EE10, EE11);
}
