/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.util.Map;

import io.openliberty.mcp.internal.McpProtocolVersion;

/**
 * A request to initialize a session.
 *
 * @param protocolVersion the protocol version requested by the client
 * @param clientCapabilities the unparsed capability map sent by the client
 * @param clientInfo the information sent by the client to identify itself
 */
public record InitializeRequest(McpProtocolVersion protocolVersion,
                                Map<String, Object> clientCapabilities,
                                ImplementationInfoImpl clientInfo) {}
