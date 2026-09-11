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
 * {@link McpAsyncServerSuite#serverLifecycle} directly.
 *
 * <p>Each test class manages its own WAR deployment in {@code @BeforeClass} /
 * {@code @AfterClass} using {@link ServerLifecycleRule#deployWithConfiguration} or dropins.
 * The suite does NOT pre-deploy any applications — {@code mcp-server-async/server.xml}
 * contains no {@code <application>} declarations.
 *
 *
 * <p>Test classes must use {@code serverLifecyclegetServer().setMarkToEndOfLog()}
 * to isolate their log searches from earlier tests.
 */
@RunWith(Suite.class)
@SuiteClasses({
                AsyncToolsErrorHandlingTest.class,
                AsyncToolCallEventTraceTest.class,
                AsyncToolLifecycleTest.class,
                AsyncToolsTest.class,
})
public class McpAsyncServerSuite {

    @ClassRule
    public static ServerLifecycleRule serverLifecycle = new ServerLifecycleRule("mcp-server-async");

}