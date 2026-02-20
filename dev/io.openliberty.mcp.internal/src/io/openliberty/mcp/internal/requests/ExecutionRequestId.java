/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.security.Principal;

import io.openliberty.mcp.internal.sessions.McpSessionId;
import io.openliberty.mcp.request.RequestId;

public record ExecutionRequestId(RequestId id, McpSessionId sessionId, Principal userId) {}
