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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.mcp.internal.fat.introspector.IntrospectorMultiAppTest;
import io.openliberty.mcp.internal.fat.isolation.MultiAppIsolationTest;
import io.openliberty.mcp.internal.fat.lifecycle.tests.BeanLifecycleTest;
import io.openliberty.mcp.internal.fat.lifecycle.tests.LifecycleTest;
import io.openliberty.mcp.internal.fat.protocol.HttpTest;
import io.openliberty.mcp.internal.fat.protocol.ProtocolVersionSchemaTest;
import io.openliberty.mcp.internal.fat.protocol.ProtocolVersionTest;
import io.openliberty.mcp.internal.fat.statelessMode.StatefulModeTest;
import io.openliberty.mcp.internal.fat.tool.CancellationTest;
import io.openliberty.mcp.internal.fat.tool.DefaultValueTest;
import io.openliberty.mcp.internal.fat.tool.DeploymentProblemTest;
import io.openliberty.mcp.internal.fat.tool.EncoderTest;
import io.openliberty.mcp.internal.fat.tool.ExceptionLoggingTest;
import io.openliberty.mcp.internal.fat.tool.GenericToolTest;
import io.openliberty.mcp.internal.fat.tool.InactiveCdiTest;
import io.openliberty.mcp.internal.fat.tool.McpUrlPathTest;
import io.openliberty.mcp.internal.fat.tool.MultiModuleToolTestToolManager;
import io.openliberty.mcp.internal.fat.tool.NoParamNameTest;
import io.openliberty.mcp.internal.fat.tool.NonRequiredArgsToolsTest;
import io.openliberty.mcp.internal.fat.tool.ToolCallEventTraceTest;
import io.openliberty.mcp.internal.fat.tool.ToolErrorHandlingTest;
import io.openliberty.mcp.internal.fat.tool.ToolManagerTest;
import io.openliberty.mcp.internal.fat.tool.UnsupportedAnnotationWarningTest;

/**
 * Suite that owns the lifecycle of the {@code mcp-server} Liberty server.
 * The server is started once before all tests in this suite and stopped once
 * after all tests complete.
 *
 * <p>Test classes in this suite must NOT use {@code @Server} injection — doing
 * so would cause FATRunner to stop the shared server after each test class via
 * {@code tidyAllKnownServers}. Instead, each test class references
 * {@link McpMainServerSuite#serverLifecycle} directly.
 *
 * <p>Each test class deploys its WAR in {@code @BeforeClass} via
 * {@link ServerLifecycleRule#deployDropinApp} and undeploys it in
 * {@code @AfterClass} via {@link ServerLifecycleRule#undeployDropinApp}.
 * The suite does NOT pre-deploy any applications.
 *
 * <p>Note: {@link io.openliberty.mcp.internal.fat.tool.ToolTest} is intentionally
 * excluded because its LITE-mode {@code @ClassRule} repeat requires a server restart
 * between repeat runs. It runs as a standalone flat entry in {@code FATSuite} after
 * this suite completes.
 */
@RunWith(Suite.class)
@SuiteClasses({
                // Batch 1
                BeanLifecycleTest.class,
                CancellationTest.class,
                DefaultValueTest.class,
                EncoderTest.class,
                ExceptionLoggingTest.class,
                // Batch 2
                GenericToolTest.class,
                HttpTest.class,
                InactiveCdiTest.class,
                IntrospectorMultiAppTest.class,
                LifecycleTest.class,
                // Batch 3
                McpUrlPathTest.class,
                MultiAppIsolationTest.class,
                MultiModuleToolTestToolManager.class,
                NonRequiredArgsToolsTest.class,
                // Batch 4
                ProtocolVersionTest.class,
                ProtocolVersionSchemaTest.class,
                StatefulModeTest.class,
                ToolCallEventTraceTest.class,
                ToolErrorHandlingTest.class,
                // Batch 5
                ToolManagerTest.class,
                UnsupportedAnnotationWarningTest.class,
                NoParamNameTest.class,
                DeploymentProblemTest.class,
})
public class McpMainServerSuite {

    @ClassRule
    public static ServerLifecycleRule serverLifecycle = new ServerLifecycleRule("mcp-server");

}
