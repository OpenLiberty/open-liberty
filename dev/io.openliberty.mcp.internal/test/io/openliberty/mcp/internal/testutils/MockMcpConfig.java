/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.testutils;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;

import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;

public record MockMcpConfig(boolean stateless,
                            String moduleName,
                            String path,
                            String servicePid,
                            Duration sessionTimeout,
                            ServerInfo serverInfo,
                            long asyncTimeoutMs) implements McpConfig {

    public static final ServerInfo UNIT_TEST_SERVER_INFO = new ServerInfo("unitTest", null, "1.0", null);

    public static MockMcpConfig createStateless() {
        return new MockMcpConfig(true, "test", "/test", "test", Duration.of(30, SECONDS), UNIT_TEST_SERVER_INFO, 30_000);
    }

    public static MockMcpConfig createStateful() {
        return new MockMcpConfig(false, "test", "/test", "test", Duration.of(30, SECONDS), UNIT_TEST_SERVER_INFO, 30_000);
    }
}
