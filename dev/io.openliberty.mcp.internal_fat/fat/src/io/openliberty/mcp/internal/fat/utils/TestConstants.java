/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import java.time.Duration;

public class TestConstants {

    public static final Duration POSITIVE_TIMEOUT = Duration.ofMillis(10_000);
    public static final Duration NEGATIVE_TIMEOUT = Duration.ofMillis(500);

    // HTTP header names
    public static final String ACCEPT = "Accept";
    public static final String MCP_PROTOCOL_VERSION = "MCP-Protocol-Version";
    public static final String MCP_SESSION_ID = "Mcp-Session-Id";

    // Header values
    public static final String VALUE_ACCEPT_DEFAULT = "application/json, text/event-stream";
    public static final String VALUE_APPLICATION_JSON = "application/json";
    public static final String VALUE_MCP_PROTOCOL_VERSION = "2025-06-18";
}
