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

import io.openliberty.mcp.internal.fat.monitor.McpMonitorMXBeanAccessTest;
import io.openliberty.mcp.internal.fat.monitor.McpMonitorTest;

/**
 * Suite that owns the lifecycle of the {@code mcp-server-monitor-only} Liberty server.
 * The server is started once before all tests in this suite and stopped once
 * after all tests complete.
 *
 * <p>Test classes in this suite must NOT use {@code @Server} injection — doing
 * so would cause FATRunner to stop the shared server after each test class via
 * {@code tidyAllKnownServers}. Instead, each test class references
 * {@link McpMonitorServerSuite#serverLifecycle} directly.
 *
 * <p>Both test classes query MCP MBeans server-wide, so each one must deploy its
 * application in {@code @BeforeClass} and undeploy it in {@code @AfterClass}.
 * Undeploying removes the application's MBeans, which keeps the classes
 * isolated from each other.
 */
@RunWith(Suite.class)
@SuiteClasses({
                McpMonitorMXBeanAccessTest.class,
                McpMonitorTest.class,
})
public class McpMonitorServerSuite {

    @ClassRule
    public static ServerLifecycleRule serverLifecycle = new ServerLifecycleRule("mcp-server-monitor-only");

}
