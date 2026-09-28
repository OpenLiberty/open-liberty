/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.responses;

/**
 * Response body for the {@code GET /mcp/info} endpoint.
 *
 * @param sessions the number of currently active MCP sessions
 * @param tools    the number of registered MCP tools
 */
public record McpInfoResponse(int sessions, int tools) {}
