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

import static io.openliberty.mcp.internal.McpProtocolVersion.V_2025_03_26;
import static io.openliberty.mcp.internal.McpProtocolVersion.V_2026_07_28;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;

/**
 * A mutable {@link McpConfig} for tests. Use {@link #createStateless()} or {@link #createStateful()} to get a config with
 * default values, then use the setters (which return this instance for chaining) to change individual fields.
 */
public class MockMcpConfig implements McpConfig {

    public static final ServerInfo UNIT_TEST_SERVER_INFO = new ServerInfo("unitTest", null, "1.0", null);

    private boolean stateless = true;
    private String moduleName = "test";
    private String path = "/test";
    private String servicePid = "test";
    private Duration sessionTimeout = Duration.of(30, SECONDS);
    private ServerInfo serverInfo = UNIT_TEST_SERVER_INFO;
    private long asyncTimeoutMs = 30_000;
    private McpProtocolVersion minimumProtocolVersion = V_2025_03_26;
    private McpProtocolVersion maximumProtocolVersion = V_2026_07_28;

    public static MockMcpConfig createStateless() {
        return new MockMcpConfig().setStateless(true);
    }

    public static MockMcpConfig createStateful() {
        return new MockMcpConfig().setStateless(false);
    }

    @Override
    public boolean stateless() {
        return stateless;
    }

    public MockMcpConfig setStateless(boolean stateless) {
        this.stateless = stateless;
        return this;
    }

    @Override
    public String moduleName() {
        return moduleName;
    }

    public MockMcpConfig setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    @Override
    public String path() {
        return path;
    }

    public MockMcpConfig setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public String servicePid() {
        return servicePid;
    }

    public MockMcpConfig setServicePid(String servicePid) {
        this.servicePid = servicePid;
        return this;
    }

    @Override
    public Duration sessionTimeout() {
        return sessionTimeout;
    }

    public MockMcpConfig setSessionTimeout(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    @Override
    public ServerInfo serverInfo() {
        return serverInfo;
    }

    public MockMcpConfig setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
        return this;
    }

    @Override
    public long asyncTimeoutMs() {
        return asyncTimeoutMs;
    }

    public MockMcpConfig setAsyncTimeoutMs(long asyncTimeoutMs) {
        this.asyncTimeoutMs = asyncTimeoutMs;
        return this;
    }

    @Override
    public McpProtocolVersion minimumProtocolVersion() {
        return minimumProtocolVersion;
    }

    public MockMcpConfig setMinimumProtocolVersion(McpProtocolVersion minimumProtocolVersion) {
        this.minimumProtocolVersion = minimumProtocolVersion;
        return this;
    }

    @Override
    public McpProtocolVersion maximumProtocolVersion() {
        return maximumProtocolVersion;
    }

    public MockMcpConfig setMaximumProtocolVersion(McpProtocolVersion maximumProtocolVersion) {
        this.maximumProtocolVersion = maximumProtocolVersion;
        return this;
    }

    @Override
    public String toString() {
        return "MockMcpConfig[stateless=" + stateless
               + ", moduleName=" + moduleName
               + ", path=" + path
               + ", servicePid=" + servicePid
               + ", sessionTimeout=" + sessionTimeout
               + ", serverInfo=" + serverInfo
               + ", asyncTimeoutMs=" + asyncTimeoutMs
               + ", minimumProtocolVersion=" + minimumProtocolVersion
               + ", maximumProtocolVersion=" + maximumProtocolVersion
               + "]";
    }
}
