/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.tools;

import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.requests.McpOperationRequest;

/**
 * A request to call a tool that has been parsed and validated
 *
 * @param request the parent operation request
 * @param tool the tool being called
 * @param args the arguments to the tool, parsed as the correct types
 */
@Trivial
public record ToolCallRequest(McpOperationRequest request,
                              ToolMetadata tool,
                              Map<String, Object> args,
                              Map<String, Object> meta) {

}
