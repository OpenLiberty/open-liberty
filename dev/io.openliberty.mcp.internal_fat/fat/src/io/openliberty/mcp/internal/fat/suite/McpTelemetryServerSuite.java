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

import io.openliberty.mcp.internal.fat.tool.TelemetryOperationsTest;
import io.openliberty.mcp.internal.fat.tool.TelemetrySessionsTest;

/**
 * Suite that owns the lifecycle of the {@code mcp-server-telemetry} Liberty server.
 * The server is started once before all tests in this suite and stopped once
 * after all tests complete.
 */
@RunWith(Suite.class)
@SuiteClasses({
                TelemetryOperationsTest.class,
                TelemetrySessionsTest.class,
})
public class McpTelemetryServerSuite {

    @ClassRule
    public static ServerLifecycleRule serverLifecycle = new ServerLifecycleRule("mcp-server-telemetry");

}
