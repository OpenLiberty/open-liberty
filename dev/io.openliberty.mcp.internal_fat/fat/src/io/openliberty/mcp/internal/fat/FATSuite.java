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

import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.mcp.internal.fat.introspector.IntrospectorMultiAppTest;
import io.openliberty.mcp.internal.fat.lifecycle.tests.AsyncToolLifecycleTest;
import io.openliberty.mcp.internal.fat.lifecycle.tests.BeanLifecycleTest;
import io.openliberty.mcp.internal.fat.lifecycle.tests.LifecycleTest;
import io.openliberty.mcp.internal.fat.protocol.HttpTest;
import io.openliberty.mcp.internal.fat.protocol.ProtocolVersionSchemaTest;
import io.openliberty.mcp.internal.fat.protocol.ProtocolVersionTest;
import io.openliberty.mcp.internal.fat.security.AdminsRoleAllowedTests;
import io.openliberty.mcp.internal.fat.security.AdminsRoleAllowedTestsStateless;
import io.openliberty.mcp.internal.fat.security.AsyncAdminsRoleAllowedTests;
import io.openliberty.mcp.internal.fat.security.AsyncDenyAllTests;
import io.openliberty.mcp.internal.fat.security.AsyncNoClassAnnotationTests;
import io.openliberty.mcp.internal.fat.security.AsyncPermitAllTests;
import io.openliberty.mcp.internal.fat.security.DenyAllTests;
import io.openliberty.mcp.internal.fat.security.DenyAllTestsStateless;
import io.openliberty.mcp.internal.fat.security.NoClassAnnotationTests;
import io.openliberty.mcp.internal.fat.security.NoClassAnnotationTestsStateless;
import io.openliberty.mcp.internal.fat.security.PermitAllTests;
import io.openliberty.mcp.internal.fat.security.PermitAllTestsStateless;
import io.openliberty.mcp.internal.fat.statelessMode.StatefulModeTest;
import io.openliberty.mcp.internal.fat.statelessMode.StatelessModeTest;
import io.openliberty.mcp.internal.fat.tool.AsyncToolCancellationTest;
import io.openliberty.mcp.internal.fat.tool.AsyncToolsErrorHandlingTest;
import io.openliberty.mcp.internal.fat.tool.AsyncToolsTest;
import io.openliberty.mcp.internal.fat.tool.CancellationTest;
import io.openliberty.mcp.internal.fat.tool.DeploymentProblemTest;
import io.openliberty.mcp.internal.fat.tool.EncoderTest;
import io.openliberty.mcp.internal.fat.tool.ExceptionLoggingTest;
import io.openliberty.mcp.internal.fat.tool.GenericToolTest;
import io.openliberty.mcp.internal.fat.tool.InactiveCdiTest;
import io.openliberty.mcp.internal.fat.tool.McpUrlPathTest;
import io.openliberty.mcp.internal.fat.tool.NoParamNameTest;
import io.openliberty.mcp.internal.fat.tool.NonRequiredArgsToolsTest;
import io.openliberty.mcp.internal.fat.tool.ToolErrorHandlingTest;
import io.openliberty.mcp.internal.fat.tool.ToolTest;

/**
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                AsyncToolsTest.class,
                AsyncToolCancellationTest.class,
                AsyncToolsErrorHandlingTest.class,
                AsyncToolLifecycleTest.class,
                BeanLifecycleTest.class,
                CancellationTest.class,
                DeploymentProblemTest.class,
                EncoderTest.class,
                ExceptionLoggingTest.class,
                HttpTest.class,
                GenericToolTest.class,
                InactiveCdiTest.class,
                IntrospectorMultiAppTest.class,
                LifecycleTest.class,
                McpUrlPathTest.class,
                NonRequiredArgsToolsTest.class,
                NoParamNameTest.class,
                ProtocolVersionTest.class,
                ProtocolVersionSchemaTest.class,
                StatefulModeTest.class,
                StatelessModeTest.class,
                ToolErrorHandlingTest.class,
                ToolTest.class,
                // Authorisation Tests
                AdminsRoleAllowedTests.class,
                DenyAllTests.class,
                NoClassAnnotationTests.class,
                PermitAllTests.class,
                // Async Authorisation Tests
                AsyncAdminsRoleAllowedTests.class,
                AsyncDenyAllTests.class,
                AsyncNoClassAnnotationTests.class,
                AsyncPermitAllTests.class,
                // Stateless Authorisation Tests
                PermitAllTestsStateless.class,
                DenyAllTestsStateless.class,
                NoClassAnnotationTestsStateless.class,
                AdminsRoleAllowedTestsStateless.class,

})

public class FATSuite {
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(null, /* skipTransformation */ true, EE10, EE11);
}
